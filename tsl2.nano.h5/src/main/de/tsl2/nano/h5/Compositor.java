/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.04.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanFinder;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.Composition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.SecureAction;

/**
 * The Compositor is a fast- or one-click collector using a base-type to create composition instances from. It's a kind
 * of charging items of the base-type.
 * <p/>
 * It provides actions to refresh, save and storno the list of charges. Each charge (=add or create) will be done by
 * activing the representable action for an item. Each item will represented by such an action.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class Compositor<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    /** base type of items to create composition instances from */
    @Transient
    Class<?> parentType;
    /** composition instance attribute to assign the base instance to */
    @Transient
    private String baseAttribute;
    /** composition instance attribute to assign the target instance to */
    @Transient
    private String targetAttribute;
    /** base instance attribute name to evaluate an image path for the compositor action */
    @Transient
    private String iconAttribute;
    @Transient
    /** if true, the user will be asked to save the new item */
    protected boolean forceUserInteraction = false;

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
        init(beanType, baseType, baseAttribute, targetAttribute, iconAttribute);
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
        this.name = "Compositor (" + baseType.getSimpleName() + "-" + beanType.getSimpleName() + ")";
        this.parentType = baseType;
        this.baseAttribute = baseAttribute;
        this.targetAttribute = targetAttribute;
        this.iconAttribute = iconAttribute;
    }

    @Override
    public Collection<IAction> getActions() {
        Collection<IAction> actions = super.getActions();
        List<IAction> compositorActions =
            createCompositorActions(parentType, getSearchPanelBeans().iterator().next(), baseAttribute, targetAttribute,
                iconAttribute);
        compositorActions.addAll(actions);
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
     * @param targetAttribute see {@link #targetAttribute}
     * @param iconAttribute (optional) see {@link #iconAttribute}
     * @return list of compositor actions
     */
    public List<IAction> createCompositorActions(Class<?> baseType,
            final T preparedComposition,
            final String baseAttribute,
            final String targetAttribute,
            final String iconAttribute) {
        final Compositor _this = this;
        Collection<?> bases = BeanContainer.instance().getBeans(baseType, 0, -1);
        ArrayList<IAction> actions = new ArrayList<>();
        for (final Object base : bases) {
            actions.add(new SecureAction<T>(getName().toLowerCase() + "." + Bean.getBean(base).getId().toString(),
                Bean.getBean(base).toString()) {

                @SuppressWarnings("unchecked")
                @Override
                public T action() throws Exception {
                    getSelectionProvider().getValue().clear();
                    getSelectionProvider().getValue().add(preparedComposition);
                    T item = createItem(preparedComposition);
                    BeanContainer.initDefaults(item);
                    setDefaultValues(item, true);

                    BeanValue parent;
                    parent = (BeanValue) Bean.getBean(base).getAttribute(baseAttribute);
                    //on manyTomany targetAttribute must not be null
                    Composition comp = new Composition(parent,
                        targetAttribute != null ? getAttribute(targetAttribute) : null);
                    Object resolver = comp.createChildOnTarget(item);
                    if (!forceUserInteraction && Bean.getBean(item).isValid(new HashMap<BeanValue<?>, String>())) {
                        resolver = (T) Bean.getBean(resolver).save();
                        item = (T) Bean.getBean(item).save();
                        getCurrentData().add(item);
                        return (T) _this;//the type T should be removed
                    } else if (targetAttribute != null)
                        Bean.getBean(resolver).save();
                    getCurrentData().add(item);
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

    @Override
    @Commit
    protected void initDeserialization() {
        beanFinder = new BeanFinder(super.getClazz());
        //without commit annotation in this class, the super wont be called
        super.initDeserialization();
    }

}
