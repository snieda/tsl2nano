/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.04.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Attachment;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanFinder;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.Composition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.specification.Pool;

/**
 * The Compositor is a fast- or one-click collector using a base-type to create composition instances from. It's a kind
 * of charging items of the base-type. see {@link Composition}
 * <p/>
 * It provides actions to refresh, save and storno the list of charges. Each charge (=add or create) will be done by
 * activing the representable action for an item. Each item will represented by such an action.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes" })
public class Compositor<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(Compositor.class);
    
    /** base type of items to create composition instances from */
    @Transient
    Class<?> parentType;
    /** composition instance attribute to assign the base instance to */
    @Transient
    protected String baseAttribute;
    /** composition target type - normally the type of this BeanDefinition */
    @Transient
    protected Class<?> targetType;
    /** composition instance attribute to assign the target instance to */
    @Transient
    protected String targetAttribute;
    /** base instance attribute name to evaluate an image path for the compositor action */
    @Transient
    protected String iconAttribute;
    @Transient
    /** if true, the user will be asked to save the new item */
    protected boolean forceUserInteraction = false;
    @Transient
    protected String actionEnablerRule;
    
    private transient List<IAction> compositorActions;

    public static <T> Compositor<Collection<T>, T> getCompositor(Class<T> target) {
        return BeanDefinition.getVirtualBeanDefinition(target, Compositor.class);
    }

    /**
     * constructor
     */
    public Compositor() {
    }

    /**
     * constructor
     * 
     * @param beanType
     * @param iconAttribute2
     * @param workingMode
     */
    public Compositor(Class<T> beanType,
            Class<?> baseType,
            final String baseAttribute,
            final String targetAttribute,
            String iconAttribute) {
        BeanCollector<Collection<T>, T> bc = BeanCollector.getBeanCollector(beanType, null, MODE_SEARCHABLE, null);
        copy(bc, this, "asString", "presentationHelper", "presentable");
        //use an own map instance to be independent of changes by other beans or beancollectors.
        attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>(getAttributeDefinitions());
        presentable = (Presentable) BeanUtil.copy(bc.getPresentable());
        init(collection, new BeanFinder(beanType), workingMode, composition);
        if (baseType != null)
            init(beanType, baseType, baseAttribute, targetAttribute, iconAttribute);
        else
            LOG.warn("no baseType defined -> Compositor provides no function");
    }

    /**
     * init
     * 
     * @param beanType
     * @param baseType
     * @param baseAttribute
     * @param targetAttribute
     * @param iconAttribute
     */
    void init(Class<T> beanType,
            Class<?> baseType,
            final String baseAttribute,
            final String targetAttribute,
            String iconAttribute) {
        this.name = createName(beanType, baseType);
        this.parentType = baseType;
        this.baseAttribute = baseAttribute;
        this.targetAttribute = targetAttribute;
        this.iconAttribute = iconAttribute;
    }

    public String createName(Class<?> beanType, Class<?> baseType) {
        return createBeanDefName("Compositor", beanType, baseType);
    }
    public static String createBeanDefName(String classPrefix, Class<?> beanType, Class<?> baseType) {
        return classPrefix + " (" + (baseType != null ? baseType.getSimpleName() + "-" : "") + beanType.getSimpleName() + ")";
    }

    @Override
    public Collection<IAction> getActions() {
        if (compositorActions == null) {
            Collection<IAction> actions = super.getActions();
            if (parentType == null)
                return actions;
            compositorActions = createCompositorActions(parentType, getSearchPanelBeans().iterator().next(),
                baseAttribute, getTargetType(), targetAttribute,
                iconAttribute);
            compositorActions.addAll(actions);
        }
        return compositorActions;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    /**
     * @deprecated: see {@link #createCompositorActions(Class, Object, String, String, String)} creates an
     *              compositor-action for each item of type baseType, found in the database.
     * 
     * @param baseType see {@link #parentType}
     * @param preparedComposition pre-filled instance to be used to create new composition instances
     * @param targetAttribute see {@link #targetAttribute}
     * @param iconAttribute (optional) see {@link #iconAttribute}
     * @return list of compositor actions
     */
    public List<IAction> createCompositorActions(Class<?> baseType,
            final T preparedComposition,
            final String targetAttribute,
            final String iconAttribute) {
        Collection<?> bases = BeanContainer.instance().getBeans(baseType, 0, -1);
        ArrayList<IAction> actions = new ArrayList<>();
        for (final Object base : bases) {
            actions.add(new SecureAction<T>(Bean.getBean(base).getId().toString(),
                Bean.getBean(base).toString()) {
                T item;

                @Override
                public T action() throws Exception {
                    item = createItem(preparedComposition);
                    Bean.getBean(item).setValue(targetAttribute, base);
                    return item;
                }

                @Override
                public String getImagePath() {
                    return (String) (iconAttribute != null ? Bean.getBean(base).getValue(iconAttribute)
                        : Bean.getBean(base).getPresentable().getIcon());
                }
            });
        }
        return actions;
    }

    /**
     * creates an compositor-action for each item of type baseType, found in the database.
     * 
     * @param baseType see {@link #parentType}
     * @param preparedComposition pre-filled instance to be used to create new composition instances
     * @param baseAttribute see {@link #targetAttribute}
     * @param targetType see #
     * @param targetAttribute see {@link #targetAttribute}
     * @param iconAttribute (optional) see {@link #iconAttribute}
     * @return list of compositor actions
     */
    public List<IAction> createCompositorActions(Class<?> baseType,
            final T preparedComposition,
            final String baseAttribute,
            final Class<?> targetType,
            final String targetAttribute,
            final String iconAttribute) {
        final Compositor _this = this;
        Collection<?> bases = BeanContainer.instance().getBeans(baseType, 0, -1);
        ArrayList<IAction> actions = new ArrayList<>();
        if (bases == null)
            return actions;
        for (final Object base : bases) {
            actions.add(new SecureAction<T>(createCompositorActionId(base),
                Bean.getBean(base).toString()) {

                @SuppressWarnings("unchecked")
                @Override
                public T action() throws Exception {
                    getSelectionProvider().getValue().clear();
                    getSelectionProvider().getValue().add(preparedComposition);
                    return composeItem(preparedComposition, baseAttribute, targetType, targetAttribute, base, getParameter());
                }

                @Override
                public boolean isEnabled() {
                    boolean actionEnabled = true;
                    if (actionEnablerRule != null) {
                        IPRunnable filterRule = ENV.get(Pool.class).get(actionEnablerRule);
                        if (filterRule != null)
                            actionEnabled = (boolean) filterRule.run(MapUtil.asMap(base));
                    }
                    return super.isEnabled() && actionEnabled;
                }
                
                @Override
                public String getImagePath() {
                    if (iconAttribute != null) {
                        Bean bean = Bean.getBean(base);
                        Object pictInfo = bean.getValue(iconAttribute);
                        if (pictInfo instanceof String)
                            return (String) pictInfo;
                        else if (pictInfo != null) {
                            return Attachment.getValueFile(bean.getAttribute(iconAttribute).getId(), pictInfo).getPath();
                        } else {
                            return null;
                        }
                    } else {
                        return Bean.getBean(base).getPresentable().getIcon();
                    }
                }
            });
        }
        return actions;
    }

    public T composeItem(final T preparedComposition, final Object base, Object...parameters) {
        return composeItem(preparedComposition, baseAttribute, targetType, targetAttribute, base, parameters);
    }

    public T composeItem(final T preparedComposition, final String baseAttribute,
            final Class<?> targetType, final String targetAttribute,
            final Object base, Object...parameters) {
        BeanCollector c = targetAttribute == null ? this
                : BeanCollector.getBeanCollector((Class) targetType);
        T item = (T) c.createItem(preparedComposition);
        BeanContainer.initDefaults(item);
        if (targetAttribute == null) {
            setDefaultValues(item, true);

            //fill context parameters
            Object[] pars = parameters;
            if (pars != null)
                fillContext(item, pars);
        }
        BeanValue parent = (BeanValue) Bean.getBean(base).getAttribute(baseAttribute);
        //on manyTomany targetAttribute must not be null
        IAttributeDefinition<?> targetAttr = targetAttribute != null ? c.getAttribute(targetAttribute) : null;
        Composition comp = new Composition(parent, targetAttr);
        Object resolver = comp.createChildOnTarget(item);
        if (resolver == item) //yes we need direct instance comparing with '=='
            resolver = base;
        if (!forceUserInteraction && Bean.getBean(item).isValid(null, true)) {
            resolver = (T) persistResolverIfNotCascading(targetAttr, resolver);
            item = (T) Bean.getBean(item).save();
            getCurrentData().add(item);
            return (T) this;//the type T should be removed
        } else if (targetAttr != null && !Collection.class.isAssignableFrom(targetAttr.getType())) {
            persistResolverIfNotCascading(targetAttr, resolver);
        }
        getCurrentData().add(item);
        return item;
    }

    private Object persistResolverIfNotCascading(IAttributeDefinition<?> targetAttr, Object resolver) {
        if (targetAttr == null || !targetAttr.cascading()) {
            HashMap<BeanValue<?>, String> errors = new HashMap<>();
            if (Bean.getBean(resolver).isValid(errors, true)) {
                resolver = Bean.getBean(resolver).save();
            } else {
                LOG.error("couldn't persist resolver of type " + resolver.getClass() + " for composition target "
                        + targetAttr);
                String msg = StringUtil.toFormattedString(errors, -1, true);
                if (ENV.get("app.mode.strict", false))
                    throw new ManagedException(msg);
                else
                    Message.send(msg);
            }
        }
        return resolver;
    }

    protected String createCompositorActionId(final Object baseInstance) {
        return getName().toLowerCase() + "." + Bean.getBean(baseInstance).getId().toString();
    }

    public void setForceUserInteraction(boolean forceUserInteraction) {
        this.forceUserInteraction = forceUserInteraction;
    }
    
    public Class<?> getTargetType() {
        if (targetType == null)
            targetType = getDeclaringClass();
        return targetType;
    }

    public void setTargetType(Class<?> targetType) {
        this.targetType = targetType;
        this.name = createName(targetType, parentType);
    }

    public T getParentInstance(Object targetInstance) {
        Object v = getAttribute(targetAttribute).getValue(targetInstance);
        return (T) Bean.getBean(v).getAttribute(parentType).getValue(v);
    }
    
    @Override
    public <B extends BeanDefinition<T>> B onActivation(Map context) {
        compositorActions = null;
        return super.onActivation(context);
    }
    
    @Override
    public Compositor<COLLECTIONTYPE, T> refreshed() {
        if (isStale())
            return new Compositor<>(clazz, parentType, baseAttribute, targetAttribute, iconAttribute);
        return this;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    @Commit
    protected void initDeserialization() {
    	if (hasMode(MODE_SEARCHABLE))
    		beanFinder = new BeanFinder(super.getClazz());
        //without commit annotation in this class, the super wont be called
        super.initDeserialization();
    }

}
