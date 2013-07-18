/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 24, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tsl2.nano.Messages;
import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.collection.MapUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanAttribute;
import de.tsl2.nano.util.bean.BeanClass;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.BeanUtil;

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
 * b1.observe(&quot;string&quot;, new de.tsl2.nano.util.bean.IValueChangeListener() {
 *     public void handleChange(de.tsl2.nano.util.bean.ValueChangeEvent changeEvent) {
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
@SuppressWarnings({"rawtypes", "unchecked"})
public class Bean<T> extends BeanDefinition<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1192383647173369697L;

    /** java object instance to evaluate the attributes for - may be null on virtual beans */
    protected T instance;

    /** string representation. see {@link #toString()} */
    protected String asString;

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
     * only to be used by framework for performance aspects.
     * 
     * @param instance
     */
    public Bean<T> setInstance(T instance) {
        if (clazz.equals(UNDEFINED.getClass()))
            this.clazz = (Class<T>) instance.getClass();
        this.asString = null;
        this.instance = instance;
        replaceInstanceInAttributes(instance);
        return this;
    }

    @Override
    public Collection<IAction> getActions() {
        if (actions == null && BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(clazz))
            addDefaultSaveAction();
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
        if (isVirtual())
            return getAttribute(path[0]).getValue();
        else
            return BeanClass.getValue(instance, path);
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
     * wraps the attribute value into a bean. the attribute has to be an entity.
     * 
     * @param name attribute name
     * @return new bean holding the attributes value.
     */
    public BeanDefinition<?> getValueAsBean(String name) {
        IValueDefinition<?> attribute = getAttribute(name);
        if (BeanUtil.isStandardType(attribute.getType()) /*!BeanContainer.instance().isPersistable(attribute.getType())*/)
            throw new FormattedException("The attribute '" + name + "' is not a persistable bean");
        Serializable value = (Serializable) attribute.getValue();
        if (value == null)
            return null;
        Bean<?> bean = value instanceof Bean ? (Bean)value : getBean(value);
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

    public AttributeDefinition addAttribute(String name,
            int length,
            boolean nullable,
            Format format,
            Object defaultValue,
            String description,
            IPresentable presentation) {
        if (instance.equals(UNDEFINED))
            throw FormattedException.implementationError("this bean has no real instance. if you add bean-attributes, they must have own instances!",
                "undefined-instance");
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
            crossChecker.check();
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
     * getValueGroups. see {@link #addValueGroup(String, String...)}
     * 
     * @return all value groups of this bean
     */
    public Collection<ValueGroup> getValueGroups() {
        return valueGroups;
    }

    /**
     * addValueGroup
     * 
     * @param label title of group
     * @param attributeNames attributes contained in group. normally only the first and the last attribute name are
     *            necessary.
     * @return current bean instance
     */
    public ValueGroup addValueGroup(String label, String... attributeNames) {
        if (valueGroups == null)
            valueGroups = new LinkedList<ValueGroup>();
        ValueGroup valueGroup = new ValueGroup(label, attributeNames);
        valueGroups.add(valueGroup);
        return valueGroup;
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
        final String saveLabel = Messages.getString("tsl2nano.save");
//        return new SecureAction(actionId, saveLabel, saveLabel, IAction.MODE_DLG_OK) {
//            public Object action() throws Exception {
//                return save(bean);
//            }
//        };
        return new SaveAction(this, bean, actionId, saveLabel, saveLabel, IAction.MODE_DLG_OK);
    }

    /**
     * save
     * 
     * @return
     */
    public Object save() {
        return save(instance);
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
        // do the save - fill the old bean with the new id value
        Object newBean;
        try {
            newBean = BeanContainer.instance().save(bean);
        } catch (final RuntimeException e) {
            if (BeanContainer.isConstraintError(e)) {
                throw new FormattedException("tsl2nano.impossible_create", new Object[] { /*Configuration.current()
                                                                                           .getDefaultFormatter()
                                                                                           .format(*/bean /*)*/});
            } else {
                throw e;
            }
        }
        /*
         * after the save operation, the presenter can be used only after
         * a BeanContainer.resolveLayzRelation() and a reset()-call.
         * 
         * the gui using this presenter should be closed or recreated!
         */
        newBean = BeanContainer.instance().resolveLazyRelations(newBean);
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
     * isPersistable
     * 
     * @return
     */
    public boolean isPersistable() {
        return BeanContainer.instance().isPersistable(clazz);
    }

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @return map filled with all attribute values
     */
    public Map<String, Object> toValueMap() {
        return toValueMap(false, false, false);
    }

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @param useClassPrefix if true, the class-name will be used as prefix for the key
     * @param onlySingleValues if true, collections will be ignored
     * @param onlyFilterAttributes if true, all other than filterAttributes will be ignored
     * @param filterAttributes attributes to be filtered (ignored, if onlyFilterAttributes)
     * @return map filled with all attribute values
     */
    public Map<String, Object> toValueMap(boolean useClassPrefix,
            boolean onlySingleValues,
            boolean onlyFilterAttributes,
            String... filterAttributes) {
        final String classPrefix = useClassPrefix ? BeanAttribute.toFirstLower(instance.getClass().getSimpleName()) + "."
            : "";
        return toValueMap(classPrefix, onlySingleValues, onlyFilterAttributes, filterAttributes);
    }

    /**
     * delegates to {@link #toValueMap(String, boolean, boolean, boolean, String...)} with formatted=false
     */
    public Map<String, Object> toValueMap(String keyPrefix,
            boolean onlySingleValues,
            boolean onlyFilterAttributes,
            String... filterAttributes) {
        return toValueMap(keyPrefix, onlySingleValues, false, onlyFilterAttributes, filterAttributes);
    }

    /**
     * fills a map with all bean-attribute-names and their values. keys in alphabetic order.
     * 
     * @param keyPrefix to be used as prefix for the bean attribute name
     * @param onlySingleValues if true, collections will be ignored
     * @param formatted if true, not the values itself but the formatted values (strings) will be put. if no format was
     *            defined, the {@link DefaultFormat} will be used
     * @param onlyFilterAttributes if true, all other than filterAttributes will be ignored
     * @param filterAttributes attributes to be filtered (ignored, if onlyFilterAttributes)
     * @return map filled with all attribute values
     */
    public Map<String, Object> toValueMap(String keyPrefix,
            boolean onlySingleValues,
            boolean formatted,
            boolean onlyFilteredAttributes,
            String... filterAttributes) {
        final List<BeanAttribute> attributes = onlySingleValues ? getSingleValueAttributes() : getAttributes();
        if (filterAttributes.length == 0)
            Collections.sort(attributes);
        final Map<String, Object> map = new LinkedHashMap<String, Object>(attributes.size());
        final List<String> filter = Arrays.asList(filterAttributes);
        Object value;
        for (final BeanAttribute beanAttribute : attributes) {
            if ((onlyFilteredAttributes && filter.contains(beanAttribute.getName())) || (!onlyFilteredAttributes && !filter.contains(beanAttribute.getName()))) {
                value = beanAttribute.getValue(instance);
                if (formatted) {
                    BeanValue<?> bv = (BeanValue<?>) beanAttribute;
                    if (bv.getFormat() != null)
                        value = bv.getFormat().format(value);
                    else
                        value = value != null ? value.toString() : "";
                }
                map.put(keyPrefix + beanAttribute.getName(), value);
            }
        }
        return map;
    }

    /**
     * creates a bean through informations of a bean-definition
     * 
     * @param <I>
     * @param instance instance of bean
     * @param beandef bean description
     * @return new created bean holding given instance
     */
    protected static <I extends Serializable> Bean<I> createBean(I instance, BeanDefinition<I> beandef) {
        Bean<I> bean = new Bean<I>();
        copy(beandef, bean, "attributeDefinitions", "asString");
        bean.attributeDefinitions = (LinkedHashMap<String, IAttributeDefinition<?>>) createValueDefintions(beandef.getAttributeDefinitions());
        bean.setInstance(instance);
        return bean;
    }

    /**
     * creates new enhanced value definitions from given attribute definitions. the values instance will be {@link BeanDefinition#UNDEFINED}.
     * @param attributeDefinitions attributes to copy and enhance
     * @return new map holding value definitions
     */
    protected static LinkedHashMap<String, ? extends IAttributeDefinition<?>> createValueDefintions(Map<String, IAttributeDefinition<?>> attributeDefinitions) {
        LinkedHashMap<String, IValueDefinition<?>> valueDefs = new LinkedHashMap<String, IValueDefinition<?>>(attributeDefinitions.size());
        try {
            for (IAttributeDefinition<?> attr : attributeDefinitions.values()) {
                IValueDefinition valueDef = new BeanValue(UNDEFINED, Object.class.getMethod("getClass", new Class[0]));
                valueDefs.put(attr.getName(), copy(attr, valueDef));
            }
            return valueDefs;
        } catch (Exception e) {
            ForwardedException.forward(e);
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
    public static <I extends Serializable> Bean<I> getBean(I instanceOrName, Object...keysAndValues) {
        Bean<I> bean = getBean(instanceOrName);
        Map map = MapUtil.asMap(keysAndValues);
        Set<String> keySet = map.keySet();
        for (String key: keySet) {
            bean.setValue(key, map.get(key));
        }
        return bean;
    }
    
    /**
     * creates a bean with given instance or name. if you give a name, a virtual bean will be created and returned. if
     * you give an instance, a bean definition will be searched and copied to a new created bean.
     * 
     * @param <I> bean instance type
     * @param instanceOrName an object or string (--> {@link #isVirtual()})
     * @return new created bean
     */
    public static <I extends Serializable> Bean<I> getBean(I instanceOrName) {
        Bean<I> bean;
        if (instanceOrName instanceof String) {
            BeanDefinition<I> beandef = (BeanDefinition<I>) getBeanDefinition((String) instanceOrName);
            bean = createBean((I)UNDEFINED, beandef);
            return bean;
        } else {
            BeanDefinition<I> beandef = getBeanDefinition((Class<I>) instanceOrName.getClass());
            bean = createBean(instanceOrName, beandef);
            return bean;
        }
    }

    public String toStringDescription() {
        final Collection<BeanAttribute> attributes = getAttributes();
        final StringBuilder buf = new StringBuilder(attributes.size() * 15);
        buf.append(getName() + " {");
        for (final BeanAttribute beanAttribute : attributes) {
            if (beanAttribute instanceof BeanValue)
                buf.append(beanAttribute.toString());
            else
                buf.append(beanAttribute.getName() + "=" + beanAttribute.getValue(instance) + "\n");
        }
        buf.append("}");
        return buf.toString();
    }

    @Override
    public String toString() {
        if (asString == null)
            asString = toString(instance);
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
@SuppressWarnings({"unchecked"})
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

    public T action() throws Exception {
        return (T) bean.save(instance);
    }
    @Override
    public String getImagePath() {
        return "icons/save.png";
    }
}