/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 16, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.messaging.IListener;

/**
 * Holds all informations to define a bean as a container of bean-attributes. Uses {@link BeanClass} and
 * {@link BeanAttribute} to evaluate all attributes for a given type. usable to define a table (with columns) of beans
 * of that type - like a ListDescriptor.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Namespace(reference = "beandef.xsd")
@Default(value = DefaultType.FIELD, required = false)
public class BeanDefinition<T> extends BeanClass<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -1110193041263724431L;
    private static final Log LOG = LogFactory.getLog(BeanDefinition.class);
    /** optional filter to constrain the available attributes - in the given order */
    protected transient String[] attributeFilter;
    /** cached attribute values (directly a {@link LinkedHashMap} on type to get this map instance on deserialization */
    @ElementMap(entry = "attribute", key = "name", attribute = true, inline = true, value = "attributeDefinition", valueType = AttributeDefinition.class, required = false)
    protected LinkedHashMap<String, IAttributeDefinition<?>> attributeDefinitions;
    /** flag to define, that all attributes are evaluated and cached - for performance aspects */
    transient boolean allDefinitionsCached = false;
    /** naturalSortedAttributeNames used for performance in {@link #hasAttribute(String)} */
    transient String[] naturalSortedAttributeNames;
    /** optional presentation informations */
    protected Presentable presentable;
    /** optional helper to define presentation informations */
    protected transient BeanPresentationHelper<T> presentationHelper;
    /** the beans name. used, if bean is virtual */
    @Attribute
    protected String name;

    /** should be able to create a representable string for the given instance */
    protected ValueExpression<T> valueExpression;

    /**
     * optional bean actions. not serialized because most actions will be defined inline - so the containing class would
     * have to be serializable, too.
     */
    protected Collection<IAction> actions;

    /**
     * optional action names (must exist inside {@link #actions} to be started on each bean activation - normally
     * through the gui implementation
     */
    protected String[] activationActionNames;

    /** optional attribute relations */
    protected Map<String, IAttributeDefinition<?>> connections;
    /** optional constraints between attributes */
    protected BeanValueConditionChecker crossChecker;
    /** optional grouping informations */
    @ElementList(inline = true, name = "group", type = ValueGroup.class, required = false)
    protected Collection<ValueGroup> valueGroups;

    @Attribute(required = false)
    protected boolean isNested;
    /**
     * this value is true, if the bean-definition was created through default algorithms - no attribute filter was
     * defined.
     */
    @Attribute(required = false)
    protected boolean isdefault = true;

    protected Extension extension;

    /** used by virtual beans, having no object instance. TODO: will not work in different vm's */
    @SuppressWarnings("serial")
    static final Serializable UNDEFINED = new Serializable() {
    };

    public static final String PREFIX_VIRTUAL = "virtual.";
    protected static final String POSTFIX_FILE_EXT = ".xml";

    private static final List<BeanDefinition> virtualBeanCache = new ListSet<BeanDefinition>();
    private static final BeanDefinition volatileBean = new BeanDefinition(Object.class);
    private static boolean usePersistentCache = Environment.get("beandef.usepersistent.cache", true);

    /**
     * This constructor is only for internal use (serialization) - don't call this constructor - use
     * {@link #getBeanDefinition(Class)}, {@link #getBeanDefinition(String)} or
     * {@link #getBeanDefinition(String, Class)} instead!
     */
    protected BeanDefinition() {
        this((Class<T>) UNDEFINED.getClass());
    }

    /**
     * constructor
     * 
     * @param beanClass
     */
    public BeanDefinition(Class<T> beanClass) {
        super((Class<T>) (Environment.get("beandef.ignore.anonymous.fields", true) ? getDefiningClass(beanClass)
            : beanClass));
        name = beanClass == UNDEFINED.getClass() ? "undefined" : super.getName();
    }

    /**
     * optional actions to be executed on each activation of this bean. this method will be called internally on
     * selecting the bean - this is done by a specialized gui implementation. see {@link #activationActionNames}.
     * 
     * see {@link #setActivationActionNames(String...)}. {@link #activationActionNames} and {@link #getActions()}.
     */
    public void onActivation() {
        if (activationActionNames != null) {
            for (int i = 0; i < activationActionNames.length; i++) {
                getAction(activationActionNames[i]).activate();
            }
        }
    }

    /**
     * @return Returns the activationActionNames.
     */
    protected String[] getActivationActionNames() {
        return activationActionNames;
    }

    /**
     * see {@link #activationActionNames} and {@link #activationActionNames}.
     * 
     * @param activationActionNames The activationActionNames to set.
     */
    protected void setActivationActionNames(String... activationActionNames) {
        this.activationActionNames = activationActionNames;
    }

    /**
     * constrains the available attributes. the order of the filter will be used for generic attribute evaluations.
     * 
     * @param availableAttributes only these attributes will be usable on this bean instance. see
     *            {@link #getAttributes(boolean)}.
     */
    public void setAttributeFilter(String... availableAttributes) {
        this.attributeFilter = availableAttributes;
        isdefault = false;
        refreshAttributeDefinitions();
    }

    protected void refreshAttributeDefinitions() {
        allDefinitionsCached = false;
        isdefault = false;
        getAttributes(false);
    }

    /**
     * getDeclaringClass
     * 
     * @return
     */
    public Class<T> getDeclaringClass() {
        return getClazz();
    }

    /**
     * removes the given standard-attributes from bean (given names must be contained in standard-bean-definition. not
     * performance-optimized, because standard names have to be evaluated first!)
     * 
     * @param attributeNamesToRemove attributes to remove
     */
    public void removeAttributes(String... attributeNamesToRemove) {
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(getAttributeNames()));
        int currentSize = names.size();
        names.removeAll(Arrays.asList(attributeNamesToRemove));
        if (names.size() + attributeNamesToRemove.length != currentSize)
            throw ManagedException.implementationError("not all of given attributes were removed!",
                attributeNamesToRemove,
                getAttributeNames());
        setAttributeFilter(names.toArray(new String[0]));
    }

    /**
     * @return Returns the {@link #isdefault}.
     */
    public boolean isDefault() {
        return isdefault;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        if (!allDefinitionsCached) {
            if (attributeFilter == null) {
                List<IAttribute> attributes = super.getAttributes(readAndWriteAccess);
                attributeFilter =
                    new String[attributes.size() + (attributeDefinitions != null ? attributeDefinitions.size() : 0)];
                int i = 0;
                for (IAttribute attr : attributes) {
                    attributeFilter[i++] = attr.getName();
                }
                //the already defined virtual attributes should not be lost!
                if (attributeDefinitions != null) {
                    Set<String> defs = attributeDefinitions.keySet();
                    for (String name : defs) {
                        attributeFilter[i++] = name;
                    }
                }
            }
            for (int i = 0; i < attributeFilter.length; i++) {
                IAttributeDefinition v = getAttributeDefinitions().get(attributeFilter[i]);
                if (v == null) {
                    v = createAttributeDefinition(attributeFilter[i]);
                } else {//TODO: do we need this to reorder?
                    attributeDefinitions.remove(attributeFilter[i]);
                }
                attributeDefinitions.put(attributeFilter[i], v);
            }
            //remove previously created attributes that are not contained in filter
            if (attributeDefinitions != null) {
                Set<String> allAttributes = attributeDefinitions.keySet();
                if (allAttributes.size() != attributeFilter.length) {
                    List<String> filterList = Arrays.asList(attributeFilter);
                    for (Iterator<String> it = allAttributes.iterator(); it.hasNext();) {
                        if (!filterList.contains(it.next()))
                            it.remove();
                    }
                }
            }
            createNaturalSortedAttributeNames(attributeFilter);
            allDefinitionsCached = true;
            /*
             * create additional attributes - ignoring the filter
             */
            getPresentationHelper().defineAdditionalAttributes();

        }
        //don't use the generic type H (Class<H>) to be compilable on standard jdk javac.
        ArrayList<IAttribute> attributes =
            new ArrayList<IAttribute>((Collection/*<? extends BeanAttribute>*/) getAttributeDefinitions().values());
        /*
         * filter the result using a default filter by the presentation helper
         */
        if (Environment.get("bean.use.beanpresentationhelper.filter", true)) {
            return CollectionUtil.getFiltering(attributes, new IPredicate<IAttribute>() {
                @Override
                public boolean eval(IAttribute arg0) {
                    return getPresentationHelper().isDefaultAttribute(arg0)
                        || getValueExpression().isExpressionPart(arg0.getName());
                }
            });
        } else {
            return attributes;
        }
    }

    protected IAttributeDefinition createAttributeDefinition(String name) {
        return new AttributeDefinition<T>(BeanAttribute.getBeanAttribute(getClazz(), name).getAccessMethod());
    }

    /**
     * delegates to {@link #combineRelationAttributes(IAttributeDefinition)}
     */
    void combineRelationAttributes(String relationAttributeName) {
        combineRelationAttributes(getAttribute(relationAttributeName));
    }

    /**
     * adds all attributes of the given bean relation (must be a bean attribute holding another bean).
     * 
     * @param relation relation bean attribute
     */
    void combineRelationAttributes(IAttributeDefinition<?> relation, String... attrNames) {
        BeanDefinition<?> beanDefinition = getBeanDefinition(relation.getDeclaringClass());
        List<String> filter = Arrays.asList(attrNames);
        Collection<IAttributeDefinition<?>> attributes = beanDefinition.getAttributeDefinitions().values();
        for (IAttributeDefinition<?> attr : attributes) {
            if (filter.contains(attr.getName())) {
                attr.setAsRelation(relation.getName() + "." + attr.getName());
                addAttribute(attr);
            }
        }
    }

    /**
     * a bean is virtual, if no java class definition was given. all attribute definitions must be added manual by
     * calling {@link #addAttribute(Object, String, int, boolean, String, Object, String, IPresentable)} .
     * 
     * @return true, if this bean has no class definition - the default constructor was called
     */
    public boolean isVirtual() {
        return clazz.equals(UNDEFINED.getClass())
            || /*after deserialization it is only object */clazz.equals(Object.class);
    }

    public List<IAttributeDefinition<?>> getBeanAttributes() {
        //workaround for collection generic
        Object attributes = getAttributes(false);
        return (List<IAttributeDefinition<?>>) attributes;
    }

    /**
     * perhaps you may reference the given map in the next bean instance of same type.
     * 
     * @return definition map
     */
    protected Map<String, IAttributeDefinition<?>> getAttributeDefinitions() {
        if (attributeDefinitions == null) {
            attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>();
        }
        return attributeDefinitions;
    }

    /**
     * gets an attribute of this bean. if you defined a filter, only attributes defined in that filter are accessible.
     * if you try to access a virtual beanvalue ({@link BeanValue#isVirtual()=true}) you don't have to use the real
     * attribute name (this would be always {@link IValueAccess#ATTR_VALUE} but the description (
     * {@link BeanValue#getDescription()}).
     * 
     * @param name attribute name (on virtual attributes it is the description of the {@link BeanValue}).
     * @return attribute
     */
    public IAttributeDefinition getAttribute(String name) {
        IAttributeDefinition definition = getAttributeDefinitions().get(name);
        if (definition == null && !allDefinitionsCached) {
            if (isVirtual())
                throw ManagedException.implementationError("The attribute " + name
                    + " was not defined in this virtual bean!\nPlease define this attribute through addAttribute(...)",
                    name);
            definition = createAttributeDefinition(name);
            attributeDefinitions.put(name, definition);
        }
        return definition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAttributeNames(boolean readAndWriteAccess) {
        if (attributeFilter == null) {
            if (isVirtual() && attributeDefinitions != null)
                attributeFilter = attributeDefinitions.keySet().toArray(new String[0]);
            else if (!isVirtual()) {
                if (allDefinitionsCached) {
                    attributeFilter = attributeDefinitions.keySet().toArray(new String[0]);
                } else {
                    attributeFilter = super.getAttributeNames(readAndWriteAccess);
                }
            } else {
                attributeFilter = new String[0];
            }
            createNaturalSortedAttributeNames(attributeFilter);
        }
        return attributeFilter;
    }

    /**
     * create performance enhancing cache
     */
    private void createNaturalSortedAttributeNames(String[] attributeNames) {
        naturalSortedAttributeNames = CollectionUtil.copyOfRange(attributeNames,
            0,
            attributeNames.length,
            String[].class);
        Arrays.sort(naturalSortedAttributeNames);
    }

    /**
     * creates a new attribute with value. internally, a new {@link ValueHolder} instance will be created to work on its
     * {@link ValueHolder#getValue()} attribute. the value-expression will be added to work instead of the standard
     * mechanism. see {@link AttributeDefinition#setExpression(String)}.
     * 
     * @param name desired name of the attribute
     * @param expression value expression to be used as attribute-value
     * @param format (optional) regexp to constrain the value
     * @param description (optional) description of this attribute. if null, the name will be used.
     * @param presentation (optional) abstract presentation informations
     * @return new added bean value
     */
    public <A> AttributeDefinition<A> addAttribute(String name,
            IAttribute<A> expression,
            String description,
            IPresentable presentation) {
        AttributeDefinition<A> bv = new AttributeDefinition<A>(expression);
        bv.setBasicDef(-1, true, null, null, description != null ? description : name);
        bv.setPresentation(presentation);
        return (AttributeDefinition<A>) addAttribute(name, bv);
    }

    /**
     * creates a new attribute with value. internally, a new {@link ValueHolder} instance will be created to work on its
     * {@link ValueHolder#getValue()} attribute.
     * 
     * @param name desired name of the attribute
     * @param value current value
     * @param pattern (optional) regexp to constrain the value
     * @param description (optional) description of this attribute. if null, the name will be used.
     * @param presentation (optional) abstract presentation informations
     * @return new add bean value
     */
    public <A> AttributeDefinition<A> addAttribute(String name,
            A value,
            Format format,
            String description,
            IPresentable presentation) {
        IValueAccess v = new ValueHolder(value);
        BeanValue<A> bv = new BeanValue(v, new VAttribute(name));
        bv.setBasicDef(-1, true, format, value, description != null ? description : name);
        bv.setPresentation(presentation);
        return (AttributeDefinition<A>) addAttribute(name, bv);
    }

    /**
     * addAttribute
     * 
     * @param instance
     * @param name
     * @param length
     * @param nullable
     * @param pattern
     * @param defaultValue
     * @param description
     * @param presentation
     * @return
     */
    public IAttributeDefinition<?> addAttribute(Object instance,
            String name,
            int length,
            boolean nullable,
            Format format,
            Object defaultValue,
            String description,
            IPresentable presentation) {
        BeanValue bv = BeanValue.getBeanValue(instance, name);
        description = description != null ? description : name;
        bv.setBasicDef(length, nullable, format, defaultValue, description);
        bv.setPresentation(presentation);
        return addAttribute(bv);
    }

    /**
     * adds a new attribute to this definition
     * 
     * @param newAttribute attribute
     * @return the new created attribute
     */
    public <A> IAttributeDefinition<A> addAttribute(IAttributeDefinition<A> newAttribute) {
        return addAttribute(newAttribute.getName(), newAttribute);
    }

    /**
     * addAttribute
     * 
     * @param name
     * @param newAttribute
     * @return
     */
    protected <A> IAttributeDefinition<A> addAttribute(String name, IAttributeDefinition<A> newAttribute) {
        getAttributeDefinitions().put(
            name/*newAttribute.getName() != null ? newAttribute.getName() : newAttribute.getDescription()*/,
            newAttribute);
        //if no filter was defined, it will be prefilled in getAttributeNames()
        if (attributeFilter == null)
            attributeFilter = getAttributeNames();
        else
            attributeFilter = CollectionUtil.concatNew(new String[attributeFilter.length + 1],
                attributeFilter,
                new String[] { name });
        allDefinitionsCached = false;
        return newAttribute;
    }

    /**
     * setAttributeDefinitions
     * 
     * @param definitionMap attribute definitions. mainly to constrain and validate values.
     */
    public void setAttrDef(String name,
            int length,
            boolean nullable,
            Format format,
            Object defaultValue,
            String description) {
        getAttribute(name).setBasicDef(length, nullable, format, defaultValue, description);
    }

    /**
     * setNumberDef
     * 
     * @param name attribute name
     * @param scale attribute number scale
     * @param precision attribute number precision
     */
    public void setNumberDef(String name, int scale, int precision) {
        getAttribute(name).setNumberDef(scale, precision);
    }

    /**
     * isPersistable
     * 
     * @return
     */
    public boolean isPersistable() {
        return BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(clazz);
    }

    /**
     * isInterface
     * 
     * @return
     */
    public boolean isInterface() {
        return getDefiningClass(clazz).isInterface();
    }

    /**
     * the bean is selectable, if it has at least one action to do on it - or it has a default-constructor or is
     * persistable.
     * 
     * @return true, if an action was defined
     */
    public boolean isSelectable() {
        return !isFinal()
            && (isMultiValue() || isInterface() || isCreatable() || isPersistable()
                || Map.Entry.class.isAssignableFrom(getClazz()) || !Util.isEmpty(actions));
    }

    /**
     * isCreatable
     * 
     * @return true, if attribute is non-standard propriety object type having a public default constructor.
     */
    public boolean isCreatable() {
        Class t = getDefiningClass(clazz);
        return !BeanUtil.isStandardType(t) && BeanClass.hasDefaultConstructor(t);
    }

    static final String ACTION_PREFIX = "action";

    /**
     * getBeanActions
     * 
     * @return all methods (wrapped into actions) starting with 'action' and having no arguments.
     */
    public Collection<IAction> getActionsByClass() {
        return getActionsByClass(clazz, null);
    }

    /**
     * getBeanActions
     * 
     * @param clazz class to analyze
     * @param actions (optional) collection to be filled with actions
     * @return all public methods (wrapped into actions) starting with 'action' and having no arguments.
     */
    @SuppressWarnings("serial")
    public static Collection<IAction> getActionsByClass(Class<?> clazz,
            Collection<IAction> actions,
            Object... parameters) {
        final Method[] methods = getDefiningClass(clazz).getMethods();
        if (actions == null)
            actions = new ArrayList<IAction>();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().startsWith(ACTION_PREFIX) && methods[i].getParameterTypes().length == 0) {
                final Method m = methods[i];
                final String name = m.getName().substring(ACTION_PREFIX.length());
                CommonAction<Object> newAction = new CommonAction<Object>(m.toGenericString(),
                    name,
                    Messages.getStringOpt(m.toGenericString())) {
                    @Override
                    public Object action() throws Exception {
                        return m.invoke(getParameter()[0], new Object[0]);
                    }
                };
                newAction.setParameter(parameters);
                actions.add(newAction);
            }
        }
        return actions;
    }

    /**
     * @return Returns the actions.
     */
    public Collection<IAction> getActions() {
        if (actions == null) {
            //load entity actions (methods starting with 'action')
            actions = getActionsByClass();
        }
        return actions;
    }

    /**
     * @param actions The actions to set.
     */
    public void setActions(Collection<IAction> actions) {
        this.actions = actions;
    }

    /**
     * addAction
     * 
     * @param abstractAction
     */
    public BeanDefinition<T> addAction(IAction action) {
        if (actions == null)
            actions = new LinkedHashSet<IAction>();
        actions.add(action);
        return this;
    }

    /**
     * getAction
     * 
     * @param id action id
     * @return action or null
     */
    public IAction<?> getAction(String id) {
        if (actions != null) {
            for (IAction a : actions) {
                if (a.getId().equals(id))
                    return a;
            }
        }
        return null;
    }

    public boolean hasAttribute(String name) {
        if (naturalSortedAttributeNames == null) {
            //will define naturalSortedAttributeNames
            getAttributeNames();
        }
        return Arrays.binarySearch(naturalSortedAttributeNames, name) > -1;
    }

    /**
     * @return Returns the presentable.
     */
    public IPresentable getPresentable() {
        if (presentable == null) {
            presentable = (Presentable) Environment.get(BeanPresentationHelper.class).createPresentable();
        }
        return presentable;
    }

    /**
     * @param presentable The presentable to set.
     */
    public void setPresentable(Presentable presentable) {
        this.presentable = presentable;
    }

    /**
     * @return Returns the presentationHelper.
     */
    public <PH extends BeanPresentationHelper<T>> PH getPresentationHelper() {
        if (presentationHelper == null)
            presentationHelper = new BeanPresentationHelper<T>(this);
        return (PH) presentationHelper;
    }

    /**
     * @param presentationHelper The presentationHelper to set.
     */
    public <PH extends BeanPresentationHelper> PH setPresentationHelper(PH presentationHelper) {
        presentationHelper.init(this);
        this.presentationHelper = presentationHelper;
        return presentationHelper;
    }

    /**
     * all foreign attributes, connected to an own attribute through
     * {@link #connect(String, IAttributeDefinition, IAction)} are accessible through this method.
     * 
     * @param name foreign attribute name
     * @return foreign attribute or null
     */
    public IAttributeDefinition<?> getConnection(String name) {
        if (connections != null) {
            return connections.get(name);
        }
        return null;
    }

    public void connect(final String attrName, IValueAccess<?> valueToConnect, final IAction<?> callback) {
        connect(attrName, valueToConnect, callback, false);
    }

    /**
     * connect the attribute by the given attribute-name to another attribute through listening to value changes. if the
     * other attribute is changing, the own attribute will fire a value change, too (without changing the value!) to
     * inform other listeners, that a dependent attribute was changed. maybe useful for refreshings. the connect 'other'
     * attribute is accessible though {@link #getConnection(String)}.
     * 
     * @param attrName the beans attribute name to be connected. the attribute has to be at least of type
     *            {@link IValueAccess}! check it through {@link #getAttribute(String)}.
     * @param valueToConnect foreign attribute to listen for changes.
     * @param callback method to be called, if foreign attribute changes.
     */
    public void connect(final String attrName,
            IValueAccess<?> valueToConnect,
            final IAction<?> callback,
            boolean afterChanging) {
        valueToConnect.changeHandler().addListener(
            new ValueConnection(this, attrName, valueToConnect, callback, afterChanging));
    }

    public void connect(BeanDefinition<?> anotherBean, Object beanInstance, final IAction<?> callback) {
        connect(anotherBean, beanInstance, callback, false);
    }

    /**
     * connects all attributes of given 'anotherBean' and it's 'beanInstance'. 'callback' will be called on any value
     * change. see {@link #connect(String, IValueAccess, IAction)}.
     * <p/>
     * Attention: connections can only be done on values != null!
     * 
     * @param anotherBean bean to connect
     * @param beanInstance bean instance
     * @param callback action to start on change
     */
    public void connect(BeanDefinition<?> anotherBean,
            Object beanInstance,
            final IAction<?> callback,
            boolean afterChanging) {
        Map<String, IAttributeDefinition<?>> attributes = anotherBean.getAttributeDefinitions();
        Object v;
        for (IAttributeDefinition<?> attr : attributes.values()) {
            v = attr instanceof IValueAccess ? ((IValueAccess) attr).getValue() : attr.getValue(beanInstance);
            if (v instanceof IValueAccess)
                connect(attr.getName(), (IValueAccess<?>) attr.getValue(beanInstance), callback, afterChanging);
        }
    }

    /**
     * isMultiValue
     * 
     * @return true, if bean class is assignable from {@link Collection}.
     */
    public boolean isMultiValue() {
        return Util.isContainer(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getNameAndPath() {
        return (isVirtual() ? "" : getPath() + ".") + name;
    }

    /**
     * the bean name will only be used, if the bean has no instance (see {@link #isVirtual()}).
     * 
     * @param name bean name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        /*
         * will only work on about 99.9% - but works fast without castings. in java implementations, 
         * the class path is not contained in the hashCode.
         */
        return hashCode() == obj.hashCode();
    }

    public IAttribute getIdAttribute() {
        //this was previously evaluated on each AttributeDefinition.defineDefaults()
        Collection<IAttributeDefinition<?>> attrs = getAttributeDefinitions().values();
        for (IAttributeDefinition<?> attr : attrs) {
            if (attr.id())
                return attr;
        }
        return null;
    }

    /**
     * delegates to {@link #getBeanDefinition(String, Class, boolean)} with null and true.
     */
    public static BeanDefinition<?> getBeanDefinition(String name) {
        return getBeanDefinition(name, null, true);
    }

    /**
     * delegates to {@link #getBeanDefinition(String, Class, boolean)} with {@link BeanClass#getName(Class)} and true.
     */
    public static <T> BeanDefinition<T> getBeanDefinition(Class<T> type) {
        return getBeanDefinition(BeanClass.getName(type), type, true);
    }

    /**
     * provides predefined virtual beans - loaded initially from serialized xml. virtual beans (see {@link #isVirtual()}
     * cannot be found!
     * 
     * @param <T> bean instance type
     * @param name name of BeanDefinition. Normally the name will be a ((Class)type).getName().
     * @param type (optional) beandef type
     * @param fullInitStore if true, a full init will be done for new BeanDefinitions - but only if it has a real type
     *            (not only a name) and can be serialized to a file.
     * @return bean definition, found in cache - or new beandef
     */
    public static <T> BeanDefinition<T> getBeanDefinition(final String name, Class<T> type, boolean fullInitStore) {
        volatileBean.name = name;
        //TODO: think about use a structure through package path on file system
        int i = virtualBeanCache.indexOf(volatileBean);
        BeanDefinition<T> beandef = null;
        if (i == -1) {
            File xmlFile = getDefinitionFile(name);
            if (usePersistentCache && xmlFile.canRead()) {
                try {
                    beandef = (BeanDefinition<T>) XmlUtil.loadXml(xmlFile.getPath(), BeanDefinition.class);
                    //workaround for simple-xml not creating the desired root-extension-instance
                    if (beandef.extension != null) {
                        beandef = (BeanDefinition<T>) beandef.extension.to(beandef);
                    }
                    //perhaps, the file defines another bean-name or bean-type
                    if ((name == null || name.equals(beandef.getName())
                        && (type == null || type.equals(beandef.getClazz()))))
                        virtualBeanCache.add(beandef);
                    else {
                        LOG.warn("the file " + xmlFile.getPath() + " doesn't define the bean with name '" + name
                            + "' and type " + type);
                        beandef = null;
                    }
                } catch (Exception e) {
                    if (Environment.get("application.mode.strict", false))
                        ManagedException.forward(e);
                    else
                        LOG.error("couldn't load configuration " + xmlFile.getPath() + " for bean " + type, e);
                }
            }
            if (beandef == null) {
                type = (Class<T>) (type == null ? UNDEFINED.getClass() : type);
                beandef = new BeanDefinition<T>(type);
                virtualBeanCache.add(beandef);
                beandef.setName(name);
//                if (fullInitStore)
//                    if (true /*xmlFile.canWrite()*/) {
//                        //be careful: having write access will introduce another behaviour
//                        if (type != UNDEFINED.getClass())
                beandef.autoInit(name);
//                        beandef.saveBeanDefinition(xmlFile);
//                    } else
//                        LOG.warn("couldn't write bean-definition cache to: " + xmlFile.getPath());
            }
        } else {
            beandef = virtualBeanCache.get(i);
        }
        return beandef;
    }

    /**
     * puts the given bean definition to the cache of source bean-definitions. means, if a new bean of an equal named
     * bean definition will be created, it will inherit it's properties.<br/>
     * Only if the application was shutdown, this bean definition will be saved for future use.
     * 
     * @param beandef bean defintion to be used as source definition for new beans.
     */
    public static void define(BeanDefinition beandef) {
        virtualBeanCache.add(beandef);
    }

    @Persist
    protected void initSerialization() {
        extension = new Extension(this);
        if (extension.isEmpty())
            extension = null;
        //remove not-serializable or cycling actions
        if (actions != null && !Environment.get("strict.mode", false)) {
            Class<?> cls;
            for (Iterator<IAction> actionIt = actions.iterator(); actionIt.hasNext();) {
                IAction a = (IAction) actionIt.next();
                //on inline implementations check the parent class
                cls = a.getClass().getEnclosingClass();
                if (cls != null && (BeanDefinition.class.isAssignableFrom(cls)
                    || !Serializable.class.isAssignableFrom(cls))) {
                    LOG.warn("removing action " + a.getId() + " to do serialization");
                    actionIt.remove();
                }
            }
            if (actions.isEmpty())
                actions = null;
        }
    }

    /**
     * Extension for {@link Serializable}
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initDeserialization();
    }

    @Commit
    protected void initDeserialization() {
        if (attributeDefinitions != null) {
            attributeFilter = attributeDefinitions.keySet().toArray(new String[0]);
            createNaturalSortedAttributeNames(attributeFilter);
            allDefinitionsCached = true;
        }
    }

    /**
     * Extension for {@link Serializable}
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        //inline actions may not be serializable, so we remove them!
        initSerialization();
        out.defaultWriteObject();
    }

    public static String getDefinitionDirectory() {
        return Environment.getConfigPath() + "beandef/";
    }

    protected static File getDefinitionFile(String name) {
        return new File(getDefinitionDirectory()
            + FileUtil.getValidPathName(FileUtil.getFilePath(name).toLowerCase()) + POSTFIX_FILE_EXT);
    }

    public void saveDefinition() {
        saveBeanDefinition(getDefinitionFile(getName()));
    }

    /**
     * Load virtual BeanCollectors like QueryResult from directory. name-convention: beandef/virtual/*.xml
     */
    public static Collection<BeanDefinition<?>> loadVirtualDefinitions() {
        File[] virtDefs = FileUtil.getFiles(getDefinitionDirectory() + PREFIX_VIRTUAL, ".*.xml");
        if (virtDefs == null)
            return new ArrayList<BeanDefinition<?>>();
        Collection<BeanDefinition<?>> types = new ArrayList<BeanDefinition<?>>();
        String name;
        for (File file : virtDefs) {
            name = StringUtil.substring(PREFIX_VIRTUAL + file.getName(), null, POSTFIX_FILE_EXT);
            types.add(getBeanDefinition(name));
        }
        return types;
    }

    /**
     * saveVirtualDefinition
     * 
     * @param name
     */
    public void saveVirtualDefinition(String name) {
        setName(PREFIX_VIRTUAL + name);
        saveDefinition();
    }

    /**
     * persists cache
     */
    public static void dump() {
        for (BeanDefinition<?> bd : virtualBeanCache) {
            bd.saveDefinition();
        }
    }

    /**
     * saveBeanDefinition
     * 
     * @param xmlFile xml serialized file
     */
    protected void saveBeanDefinition(File xmlFile) {
        if (usePersistentCache)
            try {
                xmlFile.getParentFile().mkdirs();
                XmlUtil.saveXml(xmlFile.getPath(), this);
            } catch (Exception e) {
                if (Environment.get("strict.mode", false))
                    ManagedException.forward(e);
                else
                    LOG.warn("couldn't save configuration " + xmlFile.getPath() + " for bean" + getClazz(), e);
            }
    }

    /**
     * deletes all definition files - and clears the cache!
     */
    public static void deleteDefinitions() {
        if (!usePersistentCache)
            return;
        for (BeanDefinition<?> beandef : virtualBeanCache) {
            beandef.deleteDefinition(false);
        }
        virtualBeanCache.clear();
    }

    public void deleteDefinition() {
        deleteDefinition(true);
    }

    /**
     * deleteDefinition
     */
    protected void deleteDefinition(boolean remove) {
        if (usePersistentCache) {
            File file = getDefinitionFile(getName());
            if (file.canWrite())
                file.delete();
        }
        if (remove)
            virtualBeanCache.remove(this);
    }

    public static int clearCache() {
        int cleared = virtualBeanCache.size();
        LOG.info("clearing bean-definition cache of " + cleared + " elements");
        virtualBeanCache.clear();
        return cleared;
    }

    /**
     * calls all methods initializing members of this instance.
     * 
     * @param name bean name to set.
     */
    protected void autoInit(String name) {
        LOG.debug("calling autoinit() for " + name);
        setName(name);
        List<IAttributeDefinition<?>> attributes = getBeanAttributes();
        getValueExpression();
        getAttributeNames();
        BeanCollector.createColumnDefinitions(this,
            getPresentationHelper().matches("default.present.attribute.multivalue", true));
        for (IAttributeDefinition<?> a : attributes) {
            a.getFormat();
            a.getPresentation();
            a.getColumnDefinition();
        }
//        getActions();
        getPresentable();
        getConnection("");
        if (usePersistentCache)
            saveDefinition();
    }

    /**
     * get another field around the given one. not performance optimized!
     * 
     * @param name field to search around
     * @param distance positive/negative number for distance to given field key.
     * @return desired neighbour or null
     */
    IAttributeDefinition getNeighbour(String name, int distance) {
        List<String> names = Arrays.asList(getAttributeNames());
        int i = names.indexOf(name);
        int ineighbour = i + distance;
        if (ineighbour > -1 && ineighbour < names.size()) {
            return getAttribute(names.get(ineighbour));
        } else {
            return null;
        }
    }

    /**
     * whether this bean is part (attribute) of another (parent) bean. see {@link #setNested(boolean)} and
     * {@link #getNestingBean(String...)}.
     * 
     * @return true, if this bean is attribute of another bean
     */
    public boolean isNested() {
        return isNested;
    }

    /**
     * see {@link #isNested()}.
     * 
     * @param nested nested
     */
    public void setNested(boolean nested) {
        this.isNested = nested;
    }

    /**
     * If you have a presenter holding a structure of nesting beans (e.g. as panels), you are able to get a field of any
     * sub-bean.
     * <p/>
     * e.g.: getNestingField('fieldNameOfNestingBean', 'fieldNameOfNestingNestingBean');
     * 
     * @param beanStructure nesting field names
     * @return field descriptor of nesting presenter
     */
    public final IAttributeDefinition getNestingBean(String... beanStructure) {
        if (beanStructure.length > 1)
            return getNestingField(this, beanStructure);
        else
            return getAttribute(beanStructure[0]);
    }

    private final IAttributeDefinition getNestingField(BeanDefinition<?> bean, String... keyStructure) {
        if (keyStructure.length > 1) {
            Bean<?> p = (Bean<?>) bean.getNestingBean(keyStructure[0]);
            String keyStruct[] = new String[keyStructure.length - 1];
            System.arraycopy(keyStructure, 1, keyStruct, 0, keyStruct.length);
            return getNestingField(p, keyStruct);
        }
        return getAttribute(keyStructure[0]);
    }

    /**
     * @return Returns the valueExpression.
     */
    public ValueExpression<T> getValueExpression() {
        if (valueExpression == null) {
            String presentingAttr = getPresentationHelper().getBestPresentationAttribute();
            if (presentingAttr != null)
                valueExpression = new ValueExpression<T>("{" + presentingAttr + "}", getClazz());
            else
                valueExpression = new ValueExpression<T>(getName(), getClazz());
        }
        return valueExpression;
    }

    /**
     * define a string representation for instances of the beans type. see {@link #toString(Object)}.
     * 
     * @param uniqueTextValue converter from bean type to string
     */
    public void setValueExpression(ValueExpression<T> valueExpression) {
        this.valueExpression = valueExpression;
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
     * fills a map with all bean-attribute-names and their values
     * 
     * @param useClassPrefix if true, the class-name will be used as prefix for the key
     * @param onlySingleValues if true, collections will be ignored
     * @param onlyFilterAttributes if true, all other than filterAttributes will be ignored
     * @param filterAttributes attributes to be filtered (ignored, if onlyFilterAttributes)
     * @return map filled with all attribute values
     */
    public Map<String, Object> toValueMap(Object instance, boolean useClassPrefix,
            boolean onlySingleValues,
            boolean onlyFilterAttributes,
            String... filterAttributes) {
        final String classPrefix =
            useClassPrefix ? BeanAttribute.toFirstLower(instance.getClass().getSimpleName()) + "."
                : "";
        return toValueMap(instance, classPrefix, onlySingleValues, onlyFilterAttributes, filterAttributes);
    }

    /**
     * delegates to {@link #toValueMap(String, boolean, boolean, boolean, String...)} with formatted=false
     */
    public Map<String, Object> toValueMap(Object instance, String keyPrefix,
            boolean onlySingleValues,
            boolean onlyFilterAttributes,
            String... filterAttributes) {
        return toValueMap(instance, keyPrefix, onlySingleValues, false, onlyFilterAttributes, filterAttributes);
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
    public Map<String, Object> toValueMap(Object instance,
            String keyPrefix,
            boolean onlySingleValues,
            boolean formatted,
            boolean onlyFilteredAttributes,
            String... filterAttributes) {
        final List<? extends IAttribute> attributes = onlySingleValues ? getSingleValueAttributes() : getAttributes();
        if (filterAttributes.length == 0)
            Collections.sort(attributes);
        final Map<String, Object> map = new LinkedHashMap<String, Object>(attributes.size());
        final List<String> filter = Arrays.asList(filterAttributes);
        Object value;
        for (final IAttribute<?> beanAttribute : attributes) {
            if ((onlyFilteredAttributes && filter.contains(beanAttribute.getName()))
                || (!onlyFilteredAttributes && !filter.contains(beanAttribute.getName()))) {
                value =
                    beanAttribute instanceof IValueAccess ? ((IValueAccess) beanAttribute).getValue() : beanAttribute
                        .getValue(instance);
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
     * type specific string representation of the given instance
     * 
     * @param instance instance to present
     * @return string representation of instance
     */
    public String toString(T instance) {
        return getValueExpression().to(instance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }

    public Object getId() {
        return getName();
    }

    public void setIdAttribute(String name) {
        getAttribute(name).setId(true);
    }

}

/**
 * To be serializable, we had to extract a full class instead of using the shorter inline class. Only used at
 * {@link #connect(String, IValueDefinition, IAction)}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
class ValueConnection implements IListener<ChangeEvent>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1165743296608679166L;
    BeanDefinition<?> bean;
    String name;
    IValueAccess<?> valueToConnect;
    IAction<?> callback;
    boolean afterChanging;

    public ValueConnection(BeanDefinition<?> bean,
            String name,
            IValueAccess<?> valueToConnect,
            IAction<?> callback,
            boolean afterChanging) {
        super();
        this.bean = bean;
        this.name = name;
        this.valueToConnect = valueToConnect;
        this.callback = callback;
        this.afterChanging = afterChanging;
    }

    @Override
    public void handleEvent(ChangeEvent changeEvent) {
        if (changeEvent.hasChanged == afterChanging) {
            final Object value = BeanDefinition.getValue(name);
            ((IValueAccess<?>) bean.getAttribute(name)).changeHandler().fireValueChange(bean, value, value, false);
            callback.activate();
        }
    }
}
