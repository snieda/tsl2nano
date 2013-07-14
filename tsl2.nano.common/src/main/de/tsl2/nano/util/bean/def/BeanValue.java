/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 15, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_ALL;
import static de.tsl2.nano.util.bean.def.IBeanCollector.MODE_ALL_SINGLE;
import static de.tsl2.nano.util.bean.def.IPresentable.POSTFIX_SELECTOR;

import java.lang.reflect.Method;
import java.text.Format;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.bean.BeanAttribute;
import de.tsl2.nano.util.bean.ValueHolder;

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

    Bean<?> parent;

    protected static final List<BeanValue> beanValueCache = new LinkedList<BeanValue>();

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
     * @param readAccessMethod getter-method defining the beans attribute
     */
    public BeanValue(Object bean, Method readAccessMethod) {
        super(readAccessMethod);
        this.instance = bean;
    }

    /**
     * getBean
     * 
     * @return
     */
    public Object getInstance() {
        return instance;
    }

    /**
     * @param beanInstance The bean to set.
     */
    void setInstance(Object beanInstance) {
        this.instance = beanInstance;
    }

    @Override
    public Class<T> getType() {
        if (isVirtual())
            return ((IValueAccess<T>) instance).getType();
        else if (instance != null && Environment.get("value.use.instancetype", true)) {
            try {
                T value = getValue();
                //don't use inner class infos or enum values
                if (value != null && !value.getClass().isAnonymousClass()
                    && value.getClass().getDeclaringClass() == null)
                    return (Class<T>) value.getClass();
            } catch (Exception e) {
                LOG.warn("couldn't evaluate type through instance. using method-returntype instead. error was: " + e.toString());
            }
        }
        return super.getType();
    }

    @Override
    public Format getFormat() {
        if (format == null) {
            /*
             * on jpa persistable collections, the origin instance has to be hold for orphan removals.
             */
            if (Collection.class.isAssignableFrom(getType())) {
                T value = getValue();
                if (value != null)
                    this.format = new CollectionExpressionFormat<T>((Class<T>) getGenericType(), (Collection<T>) value);
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
        return (T) getValue(instance);
    }

    /**
     * getValueText
     * 
     * @return formatted value
     */
    public String getValueText() {
        T v = getValue();
        return v != null ? getFormat().format(v) : "";
    }

    /**
     * setParsedValue
     * 
     * @param source source to parse
     * @return object for given source text or null
     */
    public T setParsedValue(String source) {
        try {
            T v = getParsedValue(source);
            setValue(v);
            return v;
        } catch (Exception ex) {
            //ok, don't set the new value!
            LOG.error(ex.toString());
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
            setValue(instance, value);
            if (isDoValidation())
                status = isValid(value);
            event.hasChanged = true;
            changeHandler().fireEvent(event);
        }
    }

    /**
     * get the beanvalue instance of the given attribute value. useful to walk through relations.
     * 
     * @param name attribute name
     * @return bean value instance or null
     */
    @Override
    public IValueDefinition getRelation(String name) {
        final Object value = getValue();
        return (IValueDefinition) (value == null ? null : getBeanValue(value, name));
    }

    protected IAttributeDefinition createAttributeDefinition(String name) {
        return BeanValue.getBeanValue(instance, name);
    }

    /**
     * creates a new bean value or - if existing in cache, reuses an already created bean value with that bean-instance
     * and attributename.
     * <p/>
     * TODO: optimize performance using a temp BeanValue instance and extracting readAccessMethod. getBeanValue
     * 
     * @param bean bean instance
     * @param attributeName attribute definition
     * @return new bean value instance
     */
    public static final BeanValue getBeanValue(Object bean, String attributeName) {
        final BeanAttribute attribute = getBeanAttribute(bean.getClass(), attributeName);
        BeanValue tbv = new BeanValue(bean, attribute.getAccessMethod());
        int i = beanValueCache.indexOf(tbv);
        if (i != -1) {
            return beanValueCache.get(i);
        } else {
            beanValueCache.add(tbv);
            return tbv;
        }
    }

    /**
     * clears cache of already created bean values.
     */
    public static final void clearCache() {
        beanValueCache.clear();
    }

    /**
     * a new listener will be created to set the new value of this bean to the given beanvalue.
     * 
     * @param anotherValue beanvalue to bind
     */
    public void bind(final BeanValue<T> anotherValue) {
//        if (!getType().equals(anotherValue.getType()))
//            FormattedException.implementationError("binding beanvalues must have the same type", anotherValue.getType());
        changeHandler().addListener(new IListener<ChangeEvent>() {
            @Override
            public void handleEvent(ChangeEvent changeEvent) {
                if (changeEvent.hasChanged)
                    anotherValue.setValue((T) changeEvent.newValue);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        if (isVirtual() || description != null)
            return description;
        else
            return super.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        if (isVirtual())
            return description;
        else
            return super.getName();
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
    public int compareTo(BeanAttribute o) {
        //not really a compareTo...but a base for equals
        if (!(o instanceof BeanValue))
            return -1;
        if (instance != ((BeanValue) o).instance)
            return -1;
        if (description != null && !description.equals(((BeanValue) o).description))
            return -1;
        return super.compareTo(o);
    }

    /**
     * if the bean instance is of type {@link ValueHolder}, the attribute is virtual - no special bean-attribute is
     * available, the attribute name is always {@link ValueHolder#getValue()} .
     * 
     * @return true, if the instance is of type {@link ValueHolder}.
     */
    public boolean isVirtual() {
        return instance instanceof IValueAccess;
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
     * the parent may be a bean definition, holding this attribute. setting this parent through {@link #setParent(Bean)}
     * will provide a bean-value-tree.
     * 
     * @return Returns the parent or null, if undefined.
     */
    public Bean<?> getParent() {
        return parent;
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
     * {@inheritDoc}
     */
    @Override
    public EventController changeHandler() {
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }

    /** returns an optional action as finder/assigner - useful to select a new value */
    public IAction<IBeanCollector<?, T>> getSelector() {
        //TODO: move that to the attribute: IPresentable
        return new SecureAction<IBeanCollector<?, T>>(getName() + POSTFIX_SELECTOR,
            Environment.get("field.selector.text", "...")) {
            @Override
            public IBeanCollector<?, T> action() throws Exception {
                BeanCollector<?, ?> beanCollector;
                if (isMultiValue()) {
                    Collection<T> collection = (Collection<T>) getValue();
                    if (collection == null) {
                        collection = new ListSet<T>();
                        beanCollector = BeanCollector.getBeanCollector((Class<T>) getGenericType(), null, MODE_ALL);
                    } else {
                        beanCollector = BeanCollector.getBeanCollector((Class<T>) getGenericType(),
                            collection,
                            MODE_ALL);
                    }
                    setValue((T) collection);
                    beanCollector.setSelectionProvider(new SelectionProvider(beanCollector.getCurrentData()));
                } else {
                    LinkedList<T> selection = new LinkedList<T>();
                    selection.add(getValue());
                    beanCollector = BeanCollector.getBeanCollector(getType(), selection, MODE_ALL_SINGLE);
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
     * connects this attribute to a selector (see {@link #getSelector()} to assign values from a list.
     * 
     * @param parent the parent bean of this attribute (must be given as {@link #getParent()} not allways is filled.
     * @return selector (see {@link #getSelector()}.
     */
    public IBeanCollector<?, ?> connectToSelector(BeanDefinition<?> parent) {
        IAction<?> selector = getSelector();
        final IBeanCollector<?, ?> collector = (IBeanCollector<?, ?>) selector.activate();
        final ISelectionProvider<?> selectionProvider = collector.getSelectionProvider();
        parent.connect(getName(), selectionProvider, new CommonAction<Object>() {
            @Override
            public Object action() throws Exception {
                Collection s;
                if (isMultiValue()) {
                    Collection v = (Collection) getValue();
                    s = selectionProvider.getValue();
                    if (v == null)
                        setValue((T) s);
                    else {
                        v.clear();
                        v.addAll(s);
                    }
                } else {
                    s = selectionProvider.getValue();
                    setValue((T) selectionProvider.getFirstElement());
                }
                return s;
            }
        });
        return collector;
    }
}
