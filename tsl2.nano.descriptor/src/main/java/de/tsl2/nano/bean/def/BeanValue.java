/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 15, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.bean.def;

import static de.tsl2.nano.bean.def.IBeanCollector.MODE_ALL;
import static de.tsl2.nano.bean.def.IBeanCollector.MODE_ALL_SINGLE;
import static de.tsl2.nano.bean.def.IPresentable.POSTFIX_SELECTOR;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.Entry;
import de.tsl2.nano.collection.MapEntrySet;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.ChangeEvent;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * BeanAttribute holding the bean instance, observers and exact attribute definitions - with validation.
 * {@link IAttributeDefinition} will constrain the attribute value. it is able to add a value change listener as
 * observer ({@link #changeHandler()}. {@link EventController#addListener(IListener)}.
 * 
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BeanValue<T> extends AttributeDefinition<T> implements IValueDefinition<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 8690371851484504875L;

    private static final Log LOG = LogFactory.getLog(BeanValue.class);

    transient Object instance;
    transient Selector selector;
    protected transient Bean<?> parent;

    /** a cache of all created beanvalues - if bean cache is not deaktivated */
    protected static final List<BeanValue> beanValueCache = new LinkedList<BeanValue>();
    /** for performance enhancement one bean to be used to search inside the {@link #beanValueCache} */
    protected static final BeanValue searchBV = new BeanValue();

    /**
     * constructor to be serializable
     */
    protected BeanValue() {
        super();
    }

    /**
     * constructor
     * 
     * @param bean the instance to wrap and reflect
     * @param attribute beanattribute or attribute-expression
     */
    public BeanValue(Object bean, IAttribute<T> attribute) {
        super(attribute);
        this.instance = bean;
        beanValueCache.add(this);
    }

    public BeanValue(Object bean, String name, IConstraint<T> constraint) {
        super(name, constraint);
        this.instance = bean;
        beanValueCache.add(this);
    }

    /**
     * constructor
     * 
     * @param bean the instance to wrap and reflect
     * @param readAccessMethod getter-method defining the beans attribute
     */
    protected BeanValue(Object bean, Method readAccessMethod) {
        super(readAccessMethod);
        this.instance = bean;
    }

    /**
     * getBean
     * 
     * @return
     */
    @Override
    public Object getInstance() {
        return instance;
    }

    /**
     * @param beanInstance The bean to set.
     */
    void setInstance(Object beanInstance) {
        if (isVirtualAccess() && beanInstance != null && !(beanInstance instanceof IValueAccess)) {
            throw new IllegalArgumentException("instance of virtual attribute " + this
                + " must be of type IValueAccess, but is: " + beanInstance);
        }
        this.instance = beanInstance;
    }

    @Override
    public Class<T> getType() {
        //TODO: set UNDEFINED instead of object
        if (getConstraint().getType() == Object.class || getConstraint().getType().isInterface()) {
            //if a value-expression was defined, the valueexpression-type has to be used!
            if (attribute.isVirtual()) {
                getConstraint().setType(super.getType());
            } else if (temporalType() != null) {
                getConstraint().setType((Class<T>) temporalType());
            } else if (isVirtual() && instance != null) {
                getConstraint().setType(((IValueAccess<T>) instance).getType());
            } else if (instance != null && ENV.get("value.use.instancetype", true)) {
                try {
                    T value = getValue();
                    //don't use inner class infos or enum values
                    if (value != null && !value.getClass().isAnonymousClass()
                        && value.getClass().getDeclaringClass() == null) {
                        getConstraint().setType((Class<T>) BeanClass.getDefiningClass(value.getClass()));
                    }
                } catch (Exception e) {
                    LOG.warn("couldn't evaluate type through instance. using method-returntype instead. error was: "
                        + e.toString());
                }
            } else {
                getConstraint().setType(super.getType());
            }
        }
        return getConstraint().getType();
    }

    @Override
    public Format getFormat() {
        if (getConstraint().getFormat() == null) {
            /*
             * on jpa persistable collections, the origin instance has to be hold for orphan removals.
             */
            if (Collection.class.isAssignableFrom(getType())) {
                T value = getValue();
                if (value != null) {
                    getConstraint().setFormat(
                        new CollectionExpressionFormat<T>(getGenericType(0), (Collection<T>) value));
                }
            }
        }
        return super.getFormat();
    }

    /**
     * getValue
     * 
     * @return bean attribute value
     */
    @Override
    public T getValue() {
        return getValue(instance);
    }

    /**
     * getValueText
     * 
     * @return formatted value
     */
    public String getValueText() {
        T v = getValue();
        try {
            return v != null ? getFormat().format(v) : "";
        } catch (Exception e) {
            LOG.warn(e);
            status = isValid(v);
            /* 
             * the toString() conversion should not break the application flow.
             * if a PersistentSet is not initialized but already serialized, the toString()
             * would throw a LazyInitializationException
             */
            try {
                return v.toString();
            } catch (Exception e1) {
                Bean.clearCache();
                return v.getClass() + ": " + e.toString();
            }
        }
    }

    /**
     * can be used, if for example a bean value is a byte-array to be used from outside (perhaps on an html-page loading
     * an image). if the value is not a byte-array, it will be created through serialization. this byte-array will be
     * saved (if not saved before) to a file inside a temp-directory of your environment.
     * 
     * @return temporary file-path of the current bean-value, saved as byte-array.
     */
    public File getValueFile() {
        return String.class.isAssignableFrom(getType()) ? new File(Attachment.getFilename(instance, getName(), (String) getValue())) : Attachment.getValueFile(getId(), getValue());
    }

    @Override
    public T getParsedValue(String source) {
        if (Attachment.isAttachment(this)) {
            //--> attachment, holding the data (not a file-name only)
            //first: load the transferred temporary file
            byte[] transferredBytes = FileUtil.getFileBytes(Attachment.getFilename(instance, getName(), source), null);
            //second: save that as new file - only for this instance!
            byte[] bytes = Attachment.getFileBytes(getId(), transferredBytes);
            //perhaps, convert the byte[] to an instanceof of Blob, InputStream,...
            return ByteUtil.toByteStream(bytes, getType());
        } else {
            return super.getParsedValue(source);
        }
    }

    /**
     * setParsedValue
     * 
     * @param source source to parse
     * @return object for given source text or null
     */
    public T setParsedValue(String source) {
        try {
            T v = null;
            /* 
             * if 'allowed values' are defined, re-use their instances!
             * it is not possible to move that block to value-expression,
             * because value-expression doesn't have access to the attribute-definition!
             */
            if (!Util.isEmpty(source) && getConstraint().getAllowedValues() != null
                && !getConstraint().getType().isEnum()) {
                String name;
                Object id;
                for (Object allowed : getConstraint().getAllowedValues()) {
                    id = Bean.getBean((Serializable) allowed).getId();
                    name = Bean.getBean((Serializable) allowed).toString();
                    if ((id != null && id.equals(source)) || name.equals(source)) {
                        LOG.debug("recognition of selected value '" + name + "' successful!");
                        v = (T) allowed;
                        break;
                    }
                }
                if (v == null) {
                    throw ManagedException.illegalArgument(source, getConstraint().getAllowedValues());
                }
            } else {
                v = getParsedValue(source);
            }
            setValue(v);
            return v;
        } catch (Exception ex) {
            //ok, don't set the new value!
//            LOG.error(ex.toString());
            ManagedException.forward(ex);
        }
        return null;
    }

    /**
     * setValue. informs all registered listeners
     * 
     * @param value new value
     */
    @Override
    public void setValue(T value) {
        final Object oldValue = getValue();
        final ChangeEvent event = new ChangeEvent(this, false, false, oldValue, value);
        changeHandler().fireEvent(event);
        if (!event.breakEvent) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("setting new value for attribute '" + getName() + "': " + value);
            }
            setValue(instance, value);
            if (isDoValidation()) {
                T assignValue = getValue();
                value = //the type may be changed through wrap()
                    value != null && oldValue != assignValue
                        && !PrimitiveUtil.isAssignableFrom(getType(), value.getClass()) ? assignValue : value;
                status = isValid(value);
            }
            event.hasChanged = true;
            changeHandler().fireEvent(event);
        }
    }

    @Override
    public IValueDefinition getRelation(String name) {
        return getRelation(instance, ValuePath.splitChain(name));
    }

    /**
     * TODO: check for duplication with ValueBean and BeanClass<br/>
     * get the beanvalue instance of the given attribute value. useful to walk through relations (recursive!). will stop
     * on the first relation having a value of null!
     * 
     * @param root root instance to start from
     * @param chain of attribute names
     * @return bean value or null
     */
    protected IValueDefinition getRelation(Object root, String... chain) {
        assert chain.length > 0 : "chain must not be empty!";

        BeanValue bv = root == null ? null : getBeanValue(root, chain[0]);
        if (bv == null || chain.length == 1) {
            return bv;
        }
        return getRelation(bv.getValue(), Arrays.copyOfRange(chain, 1, chain.length));
    }

    protected IAttributeDefinition createAttributeDefinition(String name) {
        return BeanValue.getBeanValue(instance, name);
    }

    /**
     * creates a new bean value or - if existing in cache, reuses an already created bean value with that bean-instance
     * and attributename.
     * <p/>
     * 
     * @param bean bean instance
     * @param attributeName attribute definition
     * @return new bean value instance
     */
    public static final BeanValue getBeanValue(Object bean, String attributeName) {
        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(bean.getClass(), attributeName, true);
        searchBV.instance = bean;
        searchBV.attribute = attribute;
        int i = beanValueCache.indexOf(searchBV);
        if (i != -1) {
            return beanValueCache.get(i);
        } else {
            BeanValue tbv = new BeanValue(bean, attribute.getAccessMethod());
            beanValueCache.add(tbv);
            return tbv;
        }
    }

    /**
     * removes this bean value from internal cache
     * 
     * @return result of {@link List#remove(Object)}
     */
    public boolean removeFromCache() {
        return beanValueCache.remove(this);
    }

    /**
     * clears cache of already created bean values.
     */
    public static final int clearCache() {
        int cleared = beanValueCache.size();
        LOG.info("clearing beanvalue cache of " + cleared + " elements");
        beanValueCache.clear();
        searchBV.instance = null;
        searchBV.attribute = null;
        return cleared;
    }

    /**
     * a new listener will be created to set the new value of this bean to the given beanvalue.
     * 
     * @param anotherValue beanvalue to bind
     */
    public void bind(final BeanValue<T> anotherValue) {
//        if (!getType().equals(anotherValue.getType()))
//            ManagedException.implementationError("binding beanvalues must have the same type", anotherValue.getType());
        changeHandler().addListener(new IListener<ChangeEvent>() {
            @Override
            public void handleEvent(ChangeEvent changeEvent) {
                if (changeEvent.hasChanged) {
                    anotherValue.setValue((T) changeEvent.newValue);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        if (isVirtual()) {
            String name;
            if (description != null) {
                return description;
            } else if ((name=getName()) != null){
                return StringUtil.toString(getParent()) + getName();
            } else {
                return BeanUtil.createUUID();
            }
        } else {
            return super.getId();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        if (isVirtualAccess()) {
            if (description == null) {
                description = StringUtil.toFirstUpper(super.getName());
            }
            return description;
        } else {
            return super.getName();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((instance == null) ? 0 : instance.hashCode());
        /*
         * on using e.g. ValueHolders, the instance and its attribute (=value) are identical.
         * then it is possible to distinguish them through the description, if defined
         */
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(IAttribute<T> o) {
        //not really a compareTo...but a base for equals
        if (!(o instanceof BeanValue)) {
            return -1;
        }
        if (instance != ((BeanValue) o).instance) {
            return -1;
        }
        if (description != null && !description.equals(((BeanValue) o).description)) {
            return -1;
        }
        return super.compareTo(o);
    }

    /**
     * if the bean instance is of type {@link ValueHolder}, the attribute is virtual - no special bean-attribute is
     * available, the attribute name is always {@link ValueHolder#getValue()} .
     * 
     * @return true, if the instance is of type {@link ValueHolder}.
     */
    @Override
    public boolean isVirtual() {
        return super.isVirtual() || instance instanceof IValueAccess;
    }

    /**
     * a beans value may be a bean again (stored as {@link #instance}).
     * 
     * @return true, if instance is of type {@link Bean}
     */
    public boolean isBean() {
        return instance instanceof Bean;
    }

    /**
     * a beans value may be a bean-collector (stored as {@link #instance}).
     * 
     * @return true, if instance is of type {@link BeanCollector}
     */
    public boolean isBeanCollector() {
        return (instance instanceof IValueAccess) && ((IValueAccess<?>) instance).getValue() instanceof BeanCollector;
    }

    /**
     * the parent may be a bean definition, holding this attribute. setting this parent through {@link #setParent(Bean)}
     * will provide a bean-value-tree.
     * 
     * @return Returns a given parent or the standard parent evaluated from instance.
     */
    public Bean<?> getParent() {
        return parent != null || instance == null ? parent : Bean.getBean((Serializable) instance);
    }

    /**
     * see {@link #getParent()}.
     * 
     * @param parent The parent to set.
     */
    public void setParent(Bean<?> parent) {
        this.parent = parent;
    }

    /**
     * as {@link #getId()} returns an identifier for the beans attribute, this id is an identifier for the value of the
     * beans attribute - a combination of it's parent bean-id and the attribute name.
     * 
     * @return bean-value identifier
     */
    public String getValueId() {
        return parent != null ? parent.getName() + "." + parent.getId() + "." + getName() : getId();
    }

    Selector<T> selector() {
        if (selector == null) {
            selector = new Selector(this);
        }
        return selector;
    }

    /** returns an optional action as finder/assigner - useful to select a new value */
    @SuppressWarnings("serial")
    public IAction<IBeanCollector<?, T>> getSelectorAction() {
        //TODO: move that to the attribute: IPresentable
        return new SecureAction<IBeanCollector<?, T>>(getName() + POSTFIX_SELECTOR,
            ENV.get("field.selector.text", "...")) {
            @Override
            public IBeanCollector<?, T> action() throws Exception {
                BeanCollector<?, ?> beanCollector;
                Composition comp = composition() ? CompositionFactory.createComposition(BeanValue.this) : null;
                boolean enabled = BeanValue.this.getPresentation().getEnabler().isActive();
                if (isMultiValue()) {
                    Selector<T> vsel = selector();
                    Collection<T> collection = vsel.getValueAsCollection();
                    beanCollector =
                        (BeanCollector<?, ?>) Util.untyped(BeanCollector.getBeanCollector(
                            vsel.getCollectionEntryType(),
                            collection,
                            enabled ? MODE_ALL : 0,
                            comp));
                    if (collection == null) {
                        vsel.createCollectionValue();
                    }
                    beanCollector.setSelectionProvider(new SelectionProvider(beanCollector.getCurrentData()));
                } else {
                    LinkedList<T> selection = new LinkedList<T>();
                    T v = getValue();
                    if (v != null) {
                        selection.add(v);
                    }
                    beanCollector =
                        (BeanCollector<?, ?>) Util.untyped(BeanCollector.getBeanCollector(getType(), selection, enabled
                            ? MODE_ALL_SINGLE : 0, comp));
                    beanCollector.setSelectionProvider(new SelectionProvider(selection));
                }
                return (IBeanCollector<?, T>) beanCollector;
            }
        };
    }

    @Override
    public String toString() {
        return (isVirtual() ? description : getName()) + "=" + getValue();
    }

    /**
     * connects this attribute to a selector (see {@link #getSelectorAction()} to assign values from a list.
     * 
     * @param parent the parent bean of this attribute (must be given as {@link #getParent()} not allways is filled.
     * @return selector (see {@link #getSelectorAction()}.
     */
    @SuppressWarnings("serial")
    public IBeanCollector<?, ?> connectToSelector(BeanDefinition<?> parent) {
        final IAction<?> selectorAction = getSelectorAction();
        final IBeanCollector<?, ?> collector = (IBeanCollector<?, ?>) selectorAction.activate();
        final ISelectionProvider<?> selectionProvider = collector.getSelectionProvider();
        parent.connect(getName(), selectionProvider, new CommonAction<Object>() {
            @Override
            public Object action() throws Exception {
                Collection s;
                if (isMultiValue()) {
                    Selector<T> vsel = selector();
                    Collection v = vsel.cachedValue();
                    s = selectionProvider.getValue();
                    if (v == null) {
                        setValue((T) s);
                    } else {
                        //fill values - only if not the same reference!
                        if (v != s) {
                            v.clear();
                            v.addAll(s);
                        }
                        vsel.synchronize(v);
                    }
                } else {
                    /*
                     * perhaps this action was called by a change-handler before change,
                     * then the first parameter should provide the new value. the selectionProvider
                     * has not the new value, yet.
                     */

                    s = (Collection) (parameters().getValue(0) != null ? parameters().getValue(0) : selectionProvider.getValue());
                    setValue(s.isEmpty() ? null : (T) s.iterator().next());
//                    setValue((T) selectionProvider.getFirstElement());
                }
                return s;
            }

            @Override
            public boolean isDefault() {
                return true;
            }
        });
        return collector;
    }
}

/**
 * handles a value type that is not a single value to be a collection or map
 * 
 * @param <T>
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class Selector<T> {
    IValueAccess<T> valueAccess;
    Boolean isMap;
    Collection<T> cachedValue;

    /**
     * constructor
     * 
     * @param valueAccess
     */
    public Selector(IValueAccess<T> valueAccess) {
        super();
        this.valueAccess = valueAccess;
    }

    public void synchronize(Collection<T> current) {
        if (isMap) {
            ((MapEntrySet) current).map();
        }
    }

    Class getCollectionEntryType() {
        checkForMap();
        return isMap ? Entry.class : getAttribute() instanceof BeanAttribute
            ? ((BeanAttribute) getAttribute()).getGenericType() : Object.class;
    }

    IAttribute<T> getAttribute() {
        return ((BeanValue) valueAccess).attribute;
    }

    Collection<T> getValueAsCollection() {
        Object v = valueAccess.getValue();
        if (v instanceof Map) {
            isMap = true;
            return CollectionUtil.asEntrySetExtender((Map) v);
        } else {
            isMap = false;
            return (Collection<T>) v;
        }
    }

    Collection<T> cachedValue() {
        if (cachedValue == null) {
            cachedValue = getValueAsCollection();
        }
        return cachedValue;
    }

    void createCollectionValue() {
        checkForMap();
        if (isMap) {
            valueAccess.setValue((T) new LinkedHashMap());
        } else {
            valueAccess.setValue((T) new ListSet<T>());
        }
    }

    void checkForMap() {
        if (isMap == null) {
            isMap = valueAccess.getValue() instanceof Map;
        }
    }
}