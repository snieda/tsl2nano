/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 24, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IConnector;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.Entry;
import de.tsl2.nano.collection.TimedReferenceMap;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.messaging.IListener;

/**
 * full bean access with extended attribute definitions, registering observers, attribute validations etc.! uses bean
 * reflection base classes like {@link BeanClass} and {@link BeanAttribute}. through the extension interface
 * {@link IAttributeDefinition}, the {@link BeanValue} works in the background for handling all defined bean values.
 * <p/>
 * it is possible to create virtual beans - means, you create a bean without base instance through {@link #Bean()} and
 * are able to add attributes to that bean through
 * {@link #addAttribute(Object, String, int, boolean, String, Object, String, IPresentable)}. this provides the
 * possibility to avoid creating new java classes to have the bean definitions.
 * <p/>
 * you are able to define actions for the bean with {@link #addAction(IAction)}.
 * <p/>
 * use:<br/>
 * - {@link #Bean(Object)} - {@link #setAttributeFilter(String...)}<br>
 * - {@link #setAttrDef(String, int, boolean, String, Object, String)}<br>
 * - {@link #getAttribute(String)}<br>
 * - {@link #getValue(String)}<br>
 * - {@link #setValue(String, Object)}<br>
 * - {@link #check()}<br>
 * - {@link #observe(String, IListener)}
 * <p/>
 * conveniences and utilities:<br/>
 * - {@link #toValueMap()} - {@link #toValueMap(String, boolean, String...)}
 * <p/>
 * example test code:<br/>
 * 
 * <pre>
 * TypeBean inst1 = new TypeBean();
 * TypeBean inst2 = new TypeBean();
 * inst1.setObject(inst2);
 * 
 * Bean b1 = new Bean(inst1);
 * Bean b2 = new Bean(inst2);
 * 
 * b1.setAttributeFilter(&quot;string&quot;, &quot;bigDecimal&quot;);
 * b2.setAttributeFilter(&quot;primitiveChar&quot;, &quot;immutableInteger&quot;);
 * 
 * b1.setAttrDef(&quot;string&quot;, 5, false, &quot;[A-Z]+&quot;, null, null);
 * 
 * b2.connect(&quot;primitiveChar&quot;, b1.getAttribute(&quot;string&quot;), new CommonAction&lt;Object&gt;() {
 *     public Object action() throws Exception {
 *         LOG.info(&quot;starting connection callback...&quot;);
 *         return null;
 *     }
 * });
 * 
 * b1.observe(&quot;string&quot;, new de.tsl2.nano.bean.IValueChangeListener() {
 *     public void handleChange(de.tsl2.nano.bean.ValueChangeEvent changeEvent) {
 *         LOG.info(changeEvent);
 *     }
 * });
 * 
 * b1.setValue(&quot;string&quot;, &quot;TEST&quot;);
 * b1.check();
 * 
 * b1.setValue(&quot;string&quot;, &quot;test&quot;);
 * try {
 *     b1.check();
 *     fail(&quot;check on &quot; + b1.getValue(&quot;string&quot;) + &quot; must fail!&quot;);
 * } catch (IllegalArgumentException ex) {
 *     //ok
 * }
 * b1.setValue(&quot;string&quot;, &quot;xxxxxxxxxxxxxxxxxxxxxxx&quot;);
 * try {
 *     b1.check();
 *     fail(&quot;check on &quot; + b1.getValue(&quot;string&quot;) + &quot; must fail!&quot;);
 * } catch (IllegalArgumentException ex) {
 *     //ok
 * }
 * b2.setValue(&quot;primitiveChar&quot;, 'X');
 * b2.setValue(&quot;immutableInteger&quot;, 99);
 * LOG.info(b1.getAttribute(&quot;object&quot;).getRelation(&quot;immutableInteger&quot;).getValue());
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Bean<T> extends BeanDefinition<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1192383647173369697L;

    private static final Log LOG = LogFactory.getLog(Bean.class);

    /** java object instance to evaluate the attributes for - may be null on virtual beans */
    protected T instance;

    /** string representation. see {@link #toString()} */
    protected String asString;
    /** inner function to detach this bean from a beancollector */
    private IAction detacher;

    /**
     * hold beans only for a short time. this will enhance performance on loading a bean. holding beans to long will
     * result in memory problems as the application may be used by many clients.
     */
    static final TimedReferenceMap<Bean> timedCache = new TimedReferenceMap<Bean>();

    /**
     * constructor to create a virtual bean - without an object instance. all attribute definitions must be done by
     * yourself.
     */
    public Bean() {
        this((T) UNDEFINED);
        //no reflection has to be done to evaluate the instance attributes!
        allDefinitionsCached = true;
        name = "virtualbean-" + System.currentTimeMillis();
    }

    /**
     * constructor
     * 
     * @param instance java instance to reflect
     */
    public Bean(T instance) {
        super((Class<T>) instance.getClass());
        this.instance = instance;
    }

    /**
     * @return Returns the instance.
     */
    public T getInstance() {
        return instance;
    }

    /**
     * unique id for this bean. on persistable beans, it is defined by evaluating {@link #getIdAttribute()} on current
     * instance.
     * 
     * @return unique bean id
     */
    @Override
    public Object getId() {
        IAttribute idAttribute = getIdAttribute();
        return idAttribute != null ? idAttribute.getValue(instance) : super.getId();
    }

    /**
     * only to be used by framework for performance aspects.
     * 
     * @param instance
     */
    public Bean<T> setInstance(T instance) {
        if (clazz.equals(UNDEFINED.getClass())) {
            this.clazz = (Class<T>) instance.getClass();
        }
        this.asString = null;
        this.instance = instance;
        replaceInstanceInAttributes(instance);
        return this;
    }

    @Override
    public Collection<IAction> getActions() {
        if (actions == null) {
            if (!isVirtual()) {
                if (instance != null) {
                    actions = getActionsByClass(instance.getClass(), null, new Object[] { instance });
                }
                if (actions.size() == 0 && isSelectable()) {
                    addDefaultSaveAction();
                }
            }
        }
        return super.getActions();
    }

    @Override
    public IValueDefinition getAttribute(String name) {
        return (IValueDefinition) super.getAttribute(name);
    }

    /**
     * evaluate the value of the given bean attribute path
     * 
     * @param bean starting instance
     * @param path full relation path, separated by '.'
     * @return attribute value or null
     */
    public Object getValue(String... path) {
        if (isVirtual()) {
            return getAttribute(path[0]).getValue();
        } else {
            return BeanClass.getValue(instance, path);
        }
    }

    /**
     * getValues
     * 
     * @param attributeNames attribute names. if no names are given, all attributes will be used
     * @return list of values ordered by attributeNames
     */
    public Collection getValues(String... attributeNames) {
        if (attributeNames.length == 0) {
            attributeNames = getAttributeNames();
        }
        final ArrayList values = new ArrayList(attributeNames.length);
        for (int i = 0; i < attributeNames.length; i++) {
//            values.add(BeanAttribute.getBeanAttribute(clazz, attributeNames[i]).getValue(instance));
            values.add(getValue(attributeNames[i]));
        }
        return values;
    }

    /**
     * setValue
     * 
     * @param attributeName attribute name
     * @param value new value
     */
    public void setValue(String attributeName, Object value) {
        getAttribute(attributeName).setValue(value);
    }

    public void setParsedValue(String attributeName, String value) {
        ((BeanValue) getAttribute(attributeName)).setParsedValue(value);
    }

    /**
     * @see #getValueAsBean(String, boolean)
     */
    public BeanDefinition<?> getValueAsBean(String name) {
        return getValueAsBean(name, true);
    }

    /**
     * wraps the attribute value into a bean. the attribute has to be an entity.
     * 
     * @param name attribute name
     * @return new bean holding the attributes value.
     */
    public BeanDefinition<?> getValueAsBean(String name, boolean cacheInstance) {
        IValueDefinition<?> attribute = getAttribute(name);
        if (BeanUtil.isStandardType(attribute.getType()) /*!BeanContainer.instance().isPersistable(attribute.getType())*/) {
            throw new ManagedException("The attribute '" + name + "' is not a persistable bean");
        }
        Serializable value = (Serializable) attribute.getValue();
        if (value == null) {
            return null;
        }
        Bean<?> bean = value instanceof Bean ? (Bean) value : getBean(value, cacheInstance);
        return bean;
    }

    /**
     * convenience to observe an attribute
     * 
     * @param name attribute name
     * @param listener observer
     */
    public void observe(String name, IListener listener) {
        getAttribute(name).changeHandler().addListener(listener);
    }

    public IAttributeDefinition<?> addAttribute(String name,
            int length,
            boolean nullable,
            Format format,
            Object defaultValue,
            String description,
            IPresentable presentation) {
        if (instance.equals(UNDEFINED)) {
            throw new IllegalStateException(
                "this bean has no real instance (UNDEFINED). if you add bean-attributes, they must have own instances!");
        }
        return addAttribute(instance, name, length, nullable, format, defaultValue, description, presentation);
    }

    /**
     * isValid
     * 
     * @param messages filled warning and error messages
     * @return true, if current value is ok
     */
    public boolean isValid(Map<BeanValue<?>, String> messages) {
        final List<BeanValue<?>> attributes = getBeanValues();
        boolean valid = true;
        for (final BeanValue<?> beanValue : attributes) {
            if (!beanValue.getStatus().ok()) {
                messages.put(beanValue, beanValue.getStatus().message());
                if (beanValue.getStatus().error() != null) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    /**
     * checks all attribute values and throws an {@link IllegalArgumentException}, if any value is not valid
     */
    public void check() {
        final Map<BeanValue<?>, String> msgMap = new LinkedHashMap<BeanValue<?>, String>();
        final boolean isValid = isValid(msgMap);
        if (!isValid) {
            throw new IllegalArgumentException(StringUtil.toString(msgMap, 0));
        }
        if (crossChecker != null)
        {
            crossChecker.check();
            //TODO: refactore incubation rule to be usable here...
//        if (rule != null) {
//            RulePool.get(rule).run(...);
//        }
        }
    }

    public void addCrossValueChecker(BeanValue checkBean,
            ArrayList<BeanValue> mustHave,
            ArrayList<BeanValue> mustNotHave) {
        crossChecker.add(checkBean, mustHave, mustNotHave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IAttributeDefinition createAttributeDefinition(String name) {
        BeanValue beanValue = BeanValue.getBeanValue(instance, name);
        beanValue.setRange(getPresentationHelper().getDefaultAllowedValues(beanValue));
        return beanValue;
    }

    /**
     * getBeanValues
     * 
     * @return available bean attribute values
     */
    public List<BeanValue<?>> getBeanValues() {
        //workaround for collection generic
        Object result = getAttributes();
        return (List<BeanValue<?>>) result;
    }

    /**
     * <p/>
     * Attention: this implementation uses the {@link BeanValue} cache - means, that the new instance may resist inside
     * that cache. override this method to initialize your bean.
     * 
     * @param type object type
     * @param constructorArgs object type constructor arguments
     * @return new bean
     */
    public void newInstance(Object... constructorArgs) {
        instance = createInstance(constructorArgs);
        replaceInstanceInAttributes(instance);
    }

    /**
     * replaceInstanceInAttributes
     * 
     * @param instance2
     */
    Bean<T> replaceInstanceInAttributes(T instance2) {
        if (attributeDefinitions != null) {
            for (final IAttributeDefinition<?> a : attributeDefinitions.values()) {
                //TODO: what to do with virtual values?
                if (a.isVirtual() && a.getAccessMethod() != null) {
                    continue;
                }
                ((BeanValue) a).setInstance(instance2);
            }
        }
        return this;
    }

    /**
     * creates a new bean with new object instance
     * 
     * @param type object type
     * @param constructorArgs object type constructor arguments
     * @return new bean
     */
    public static <T> Bean<T> newBean(Class<T> type, Object... constructorArgs) {
        return new Bean<T>(BeanClass.createInstance(type, constructorArgs));
    }

    @Override
    public int hashCode() {
        return isVirtual() ? super.hashCode() : 31 * super.hashCode() + instance.hashCode();
    }

    /**
     * addDefaultSaveAction
     * 
     * @return new created default save action for {@link #instance}.
     */
    public IAction<?> addDefaultSaveAction() {
        SecureAction<?> saveAction = createSaveAction(instance);
        addAction(saveAction);
        return saveAction;
    }

    /**
     * creates a save action for the given bean. the action-id will be 'bean.getClass().save' - that will be used as
     * permission id, too.
     * 
     * @param bean bean to save
     */
    protected SecureAction<?> createSaveAction(final Object bean) {
        final String actionId = BeanContainer.getActionId(bean.getClass(), false, "save");
        return createSaveAction(bean, actionId);
    }

    /**
     * creates a save action for the given bean. the action-id will be 'bean.getClass().save' - that will be used as
     * permission id, too.
     * 
     * @param bean bean to save
     * @param actionId action id (important for user permissions!)
     */
    protected SecureAction<T> createSaveAction(final Object bean, String actionId) {
        final String saveLabel =
            BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getDefiningClass(clazz))
                && !CompositionFactory.contains(bean) ? Messages
                .getString("tsl2nano.save") : Messages.getString("tsl2nano.assign");
        return new SaveAction(this, bean, actionId, saveLabel, saveLabel, IAction.MODE_DLG_OK);
    }

    /**
     * save
     * 
     * @return
     */
    public Object save() {
        return /*setInstance(*/save(instance)/*)*/;
    }

    /**
     * overwrite this method to define your own saving mechanism
     * 
     * @param bean normally the presenters bean, but on an unpersistable presenter-bean you should give the right
     *            persistable entity.
     * @return saved and refreshed bean (please assign the new bean to the presenters {@link BasePresenter#data}) to be
     *         used as presenters bean (see {@link BasePresenter#data}). on any catched error, you should return
     *         {@link IAction#CANCELED} to inform the framework to cancel following gui-actions. then f.e. a dialog
     *         won't be closed.
     */
    protected Object save(Object bean) {
        //on not-persistable beans, the presentation layer has to handle the save or assignment
        if (!BeanContainer.isInitialized() || !BeanContainer.instance().isPersistable(getDefiningClass(clazz))) {
            return bean;
        }
        // do the save - fill the old bean with the new id value
        Object newBean;
        try {
            if (CompositionFactory.markToPersist(bean)) {
                //refresh the bean!
//                result = BeanContainer.instance().getBeansByExample(instance).iterator().next();
                newBean = bean;
            } else {
                newBean = BeanContainer.instance().save(bean);
                /*
                 * after the save operation, the presenter can be used only after
                 * a BeanContainer.resolveLayzRelation() and a reset()-call.
                 * 
                 * the gui using this presenter should be closed or recreated!
                 */
                newBean = BeanContainer.instance().resolveLazyRelations(newBean);
            }
        } catch (final RuntimeException e) {
            if (BeanContainer.isConstraintError(e)) {
                throw new ManagedException("tsl2nano.impossible_create", new Object[] { /*Configuration.current()
                                                                                           .getDefaultFormatter()
                                                                                           .format(*/bean /*)*/});
            } else {
                throw e;
            }
        } finally {
//          detach();
        }
        //if the saved object is the presenters bean - use the new refreshed bean
        if (newBean.getClass().equals(instance.getClass())) {
            instance = (T) newBean;
        } else {//refresh the old bean with the new id
            final BeanAttribute idAttribute = BeanContainer.getIdAttribute(bean);
            if (idAttribute != null) {
                idAttribute.setValue(bean, idAttribute.getValue(newBean));
            }
        }
        return newBean;
    }

    /**
     * fills a map with all bean-attribute-names and their values.
     * 
     * @param properties will be ignored (is only inherited)
     * @return map filled with all attribute values
     */
    @Override
    public Map<String, Object> toValueMap(Map<String, Object> properties) {
        return toValueMap(instance, true, false, false);
    }

    /**
     * creates a bean through informations of a bean-definition
     * 
     * @param <I>
     * @param instance instance of bean
     * @param beandef bean description
     * @return new created bean holding given instance
     */
    protected static <I> Bean<I> createBean(I instance, BeanDefinition<I> beandef) {
        Bean<I> bean = new Bean<I>();
        copy(beandef, bean, "attributeFilter", "attributeDefinitions", "asString", "presentationHelper");
        bean.attributeFilter = beandef.attributeFilter != null ? CollectionUtil.copy(beandef.attributeFilter) : null;
        bean.attributeDefinitions =
            (LinkedHashMap<String, IAttributeDefinition<?>>) Util.untyped(createValueDefinitions(beandef
                .getAttributeDefinitions()));
        bean.setInstance(instance);

        injectIntoRuleCovers(bean);
        if (bean.getPlugins() != null) {
            for (IConnector p : bean.getPlugins()) {
                p.connect(bean);
            }
        }
        //give the new bean the chance to create actions...only if null
        if (bean.actions != null && bean.actions.size() == 0) {
            bean.actions = null;
        }
        return bean;
    }

    /**
     * injectIntoRuleCovers
     * 
     * @param bean
     */
    protected static <I> void injectIntoRuleCovers(Bean<I> bean) {
        Set<String> keys = bean.getAttributeDefinitions().keySet();
        for (String k : keys) {
            IAttributeDefinition a = bean.getAttributeDefinitions().get(k);

            //change listeners hold only the attribute-id and must have attribute instances
            AttributeDefinition attrDef;
            if (a instanceof AttributeDefinition) {
                attrDef = (AttributeDefinition) a;
                attrDef.injectAttributeOnChangeListeners(bean);
                if (attrDef instanceof IValueDefinition)
                    attrDef.injectIntoRuleCover((IValueDefinition) attrDef);
            }
        }
    }

    /**
     * creates new enhanced value definitions from given attribute definitions. the values instance will be
     * {@link BeanDefinition#UNDEFINED}.
     * 
     * @param attributeDefinitions attributes to copy and enhance
     * @return new map holding value definitions
     */
    @SuppressWarnings("serial")
    protected static LinkedHashMap<String, ? extends IValueAccess<?>> createValueDefinitions(Map<String, IAttributeDefinition<?>> attributeDefinitions) {
        LinkedHashMap<String, IValueAccess<?>> valueDefs =
            new LinkedHashMap<String, IValueAccess<?>>(attributeDefinitions.size());
        try {
            for (IAttributeDefinition<?> attr : attributeDefinitions.values()) {
//                if (!(attr instanceof IValueAccess)) {//--> standard attribute-definition
                //use any simple arguments - they will be overwritten on next line in copy(...)
                IValueAccess valueDef = new BeanValue();/*UNDEFINED, AttributeDefinition.UNDEFINEDMETHOD) {
                                                        @Override
                                                        protected void defineDefaults() {
                                                        // don't set any defaults - overwrite members in the next step
                                                        }
                                                        };*/
                valueDef = copy(attr, valueDef, "parent");
                //Workaround for 'parent' field in BeanValue to avoid a ConcurrentModificationException in Android
                if (attr instanceof BeanValue)
                    ((BeanValue) valueDef).setParent(((BeanValue) attr).getParent());
                BeanValue.beanValueCache.add((BeanValue) valueDef);
                if (valueDef instanceof IPluggable) {
                    Collection<IConnector> plugins = ((IPluggable) valueDef).getPlugins();
                    if (plugins != null) {
                        for (IConnector p : plugins) {
                            p.connect(valueDef);
                        }
                    }
                }
                valueDefs.put(attr.getName(), valueDef);
//                } else {//it is a specialized beanvalue like pathvalue or ruleattribute
//                    valueDefs.put(attr.getName(), (IValueAccess<?>) BeanUtil.clone(attr));
//                }
            }
            return valueDefs;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * creates a bean with given instance or name. if you give a name, a virtual bean will be created and returned. if
     * you give an instance, a bean definition will be searched and copied to a new created bean.
     * 
     * @param <I> bean instance type
     * @param instanceOrName an object or string (--> {@link #isVirtual()})
     * @param keysAndValues key-value-pairs to be filled to the given instance
     * @return new created bean
     */
    public static <I> Bean<I> getBean(I instanceOrName, Object... keysAndValues) {
        Bean<I> bean = getBean(instanceOrName);
        Map map = MapUtil.asMap(keysAndValues);
        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            bean.setValue(key, map.get(key));
        }
        return bean;
    }

    /**
     * @see #getBean(Serializable, boolean)
     */
    public static <I> Bean<I> getBean(I instanceOrName) {
        return getBean(instanceOrName, true/*!BeanUtil.isStandardType(instanceOrName)*/);
    }

    /**
     * creates a bean with given instance or name. if you give a name, a virtual bean will be created and returned. if
     * you give an instance, a bean definition will be searched and copied to a new created bean.
     * 
     * @param <I> bean instance type
     * @param instanceOrName an object or string (--> {@link #isVirtual()})
     * @return new created bean
     */
    public static <I> Bean<I> getBean(I instanceOrName, boolean cacheInstance) {
        Bean<I> bean = timedCache.get(instanceOrName);
        if (bean != null) {
            return bean;
        }
        if (instanceOrName instanceof String) {
            BeanDefinition<I> beandef = (BeanDefinition<I>) getBeanDefinition((String) instanceOrName);
            bean = createBean((I) UNDEFINED, beandef);
        } else if (instanceOrName.getClass().isArray()) {
            bean = createArrayBean(instanceOrName);
        } else if (Map.class.isAssignableFrom(instanceOrName.getClass())) {
            bean = createMapBean(instanceOrName);
        } else if (Entry.class.isAssignableFrom(instanceOrName.getClass())) {
            BeanDefinition<I> beandef =
                getBeanDefinition((Class<I>) BeanClass.getDefiningClass(instanceOrName.getClass()));
            bean = createBean(instanceOrName, beandef);
            Entry entry = (Entry) instanceOrName;
            if (entry.getValue() != null) {
                IConstraint c = bean.getAttribute("value").getConstraint();
                c.setFormat(null);
                c.setType(BeanClass.getDefiningClass(entry.getValue().getClass()));
            }
        } else {
            BeanDefinition<I> beandef =
                getBeanDefinition((Class<I>) BeanClass.getDefiningClass(instanceOrName.getClass()));
            bean = createBean(instanceOrName, beandef);
        }

        if (cacheInstance && ENV.get("use.bean.cache", true)) {
            timedCache.put(instanceOrName, bean);
        }
        return bean;
    }

    @Override
    //not yet used on creation
    public void autoInit(String name) {
        super.autoInit(name);
        List<BeanValue<?>> beanValues = getBeanValues();
        for (BeanValue<?> bv : beanValues) {
            bv.getPresentation();
            bv.getColumnDefinition();
        }
    }

    private static Bean createArrayBean(Object array) {
        int length = Array.getLength(array);
        Bean bean = new Bean(array);
        for (int i = 0; i < length; i++) {
            bean.addAttribute(new BeanValue(bean.instance, new ArrayValue(String.valueOf(i), i)));
        }
        return bean;
    }

    private static Bean createMapBean(Object mapInstance) {
        Map map = (Map) mapInstance;
        Bean bean = new Bean(map);
        Set keySet = map.keySet();
        Object v;
        for (Object k : keySet) {
            v = map.get(k);
            bean.addAttribute(new BeanValue(bean.instance, new MapValue(v != null ? v : k, (v != null ? BeanClass.getDefiningClass(v
                .getClass()) : null), map)));
        }
        return bean;
    }

    /**
     * attaches the given detacher
     * 
     * @param detacher
     */
    public void attach(IAction detacher) {
        this.detacher = detacher;
    }

    /**
     * cleans all bean relevant caches (BeanClass, BeanValue, BeanDefinition, Bean).
     * 
     * @return amount of cleared objects.
     */
    public static int clearCache() {
        CompositionFactory.clearCache();
        int cleared = BeanValue.clearCache();
        cleared += BeanClass.clearCache();
        cleared += BeanDefinition.clearCache();
        if (timedCache != null) {
            cleared += timedCache.size();
            LOG.info("clearing beanclass cache of " + cleared + " elements");
            timedCache.clear();
        }
        return cleared;
    }

    @Override
    public void onDeactivation() {
        super.onDeactivation();
        //if a new object was cancelled, it must be removed
        if (!isMultiValue()) {
            detach("remove");
        }
    }

    /**
     * runs detacher and sets detacher to null. if arguments equals 'remove' this bean will be removed from a parent
     * list.
     */
    public boolean detach(Object... arguments) {
        timedCache.remove(this);
        if (detacher != null) {
            detacher.setParameter(arguments);
            detacher.run();
            detacher = null;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        asString = null;
    }

    @Override
    public void onActivation() {
        //on new beans, we fill manyToOne relations if exactly one item is available
        if (BeanContainer.isTransient(instance) && ENV.get("bean.new.fill.relations.on.one.item", true)) {
            String[] names = getAttributeNames();
            for (int i = 0; i < names.length; i++) {
                IValueDefinition attr = getAttribute(names[i]);
                if (attr.isRelation() && !attr.isMultiValue() && attr.getValue() == null) {
                    if (BeanContainer.getCount(attr.getType()) == 1) {
                        attr.setValue(BeanContainer.instance().getBeans(attr.getType(), 0, -1).iterator().next());
                    }
                }
            }
        }
        super.onActivation();
    }

    public String toStringDescription() {
        final Collection<? extends IAttribute> attributes = getAttributes();
        final StringBuilder buf = new StringBuilder(attributes.size() * 15);
        buf.append(getName() + " {");
        for (final IAttribute<T> beanAttribute : attributes) {
            if (beanAttribute instanceof BeanValue) {
                buf.append(beanAttribute.toString());
            } else {
                buf.append(beanAttribute.getName() + "=" + beanAttribute.getValue(instance) + "\n");
            }
        }
        buf.append("}");
        return buf.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        //don't call a getter to evaluate attributes - the attributes would be created then
        if (attributeDefinitions != null) {
            for (IAttributeDefinition<?> bv : attributeDefinitions.values()) {
                if (bv instanceof BeanValue)
                    BeanValue.beanValueCache.remove(bv);
            }
        }
        super.finalize();
    }

    @Override
    public String toString() {
        if (asString == null) {
            asString = toString(instance);
        }
        return asString;
    }
}

/**
 * To be serializable, we had to extract a full class instead of using the shorter inline class. Only used at
 * {@link Bean#createSaveAction(Object, String)}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked" })
class SaveAction<T> extends SecureAction<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -3009132079876910521L;
    /** bean to handle the save action */
    private Bean<?> bean;
    /** instance to save - may differ from bean.instance! */
    private Object instance;

    public SaveAction(Bean<?> bean,
            Object instance,
            String id,
            String shortDescription,
            String longDescription,
            int actionMode) {
        super(id, shortDescription, longDescription, actionMode);
        this.bean = bean;
        this.instance = instance;
    }

    @Override
    public T action() throws Exception {
        return (T) bean.save();
    }

    @Override
    public String getImagePath() {
        return "icons/save.png";
    }
}