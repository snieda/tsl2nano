/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 13, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.FormatUtil;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Default(value = DefaultType.FIELD, required = false)
public class AttributeDefinition<T> implements IAttributeDefinition<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1403875731423120506L;

    @Element(name = "declaring")
    protected IAttribute<T> attribute;
    protected EventController eventController;
    @Element(type = Constraint.class, required = false)
    protected IConstraint<T> constraint;
    @Attribute(required = false)
    private boolean id;
    @Attribute(required = false)
    private boolean unique;
    @Element(required = false)
    private Class<? extends Date> temporalType;
    @Element(required = false)
    protected String description;
    protected transient IStatus status;
    @Element(type = Presentable.class, required = false)
    private IPresentable presentable;
    @Element(type = ValueColumn.class, required = false)
    private IPresentableColumn columnDefinition;
    @Element(required = false)
    private boolean doValidation = true;
    /** see {@link #composition()} */
    @Attribute(required = false)
    private boolean composition;
    /** see {@link #cascading()} */
    @Attribute(required = false)
    private boolean cascading;
    /** see {@link #generatedValue()} */
    @Attribute(required = false)
    private boolean generatedValue;

    private static final Log LOG = LogFactory.getLog(AttributeDefinition.class);

    /** internal definition to create a temporarily new beanvalue instance. */
    static final Method UNDEFINEDMETHOD = UNDEFINEDMETHOD();

    /**
     * constructor to be serializable
     * 
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    protected AttributeDefinition() {
        super();
        status = Status.STATUS_OK;
    }

    /**
     * constructor
     * 
     * @param attribute
     */
    public AttributeDefinition(IAttribute<T> attribute) {
        super();
        this.attribute = attribute;
        if (attribute.getAccessMethod() != null)
            defineDefaults();
    }

    protected AttributeDefinition(Method readAccessMethod) {
        super();
        attribute = new BeanAttribute(readAccessMethod);
        defineDefaults();
    }

    /**
     * default readaccessmethod if default constructor was invoked
     */
    private static final Method UNDEFINEDMETHOD() {
        try {
            //get yourself
            return AttributeDefinition.class.getDeclaredMethod("UNDEFINEDMETHOD", new Class[0]);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * sets default properties through the {@link BeanContainer}, if available. These defaults may be read from
     * jpa-annotations.
     */
    protected void defineDefaults() {
        if (BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getDeclaringClass())) {
            IAttributeDef def = BeanContainer.instance().getAttributeDef(getDeclaringClass(), getName());
            if (def != null) {
                LOG.debug("setting defaults from annotations for attribute: " + getName());
                setId(def.id());
                setUnique(def.unique());
                setBasicDef(def.length(), def.nullable(), null, null, null);
                getConstraint().setNumberDef(def.scale(), def.precision());
                temporalType = def.temporalType();
                composition = def.composition();
                cascading = def.cascading();
                generatedValue = def.generatedValue();
            }
        }
        initDeserialization();
    }

    @Commit
    private void initDeserialization() {
        status = IStatus.STATUS_OK;
        if (getColumnDefinition() != null && getColumnDefinition() instanceof ValueColumn)
            ((ValueColumn) getColumnDefinition()).attributeDefinition = this;

        //provide dependency listeners their attribute-definition
        if (hasListeners()) {
            BeanDefinition beandef = BeanDefinition.getBeanDefinition(getDeclaringClass());
            Collection<IListener> listener = changeHandler().getListeners(Object.class);
            for (IListener l : listener) {
                if (l instanceof AbstractDependencyListener) {
                    AbstractDependencyListener<?> dl = (AbstractDependencyListener<?>) l;
                    String name = StringUtil.substring(dl.attributeID, ".", null);
                    dl.setAttribute((AttributeDefinition) beandef.getAttribute(name));
                }
            }
        }
    }

    public final IConstraint<T> getConstraint() {
        if (constraint == null)
            constraint = new Constraint(BeanClass.getDefiningClass(attribute.getType()));
        return constraint;
    }

    /**
     * setBasicDef
     * 
     * @param length {@link #length()}
     * @param nullable {@link #nullable()}
     * @param format {@link #getPattern()}
     * @param defaultValue {@link #getDefault()}
     * @param description {@link #getDescription()}
     */
    @Override
    public IAttributeDefinition<T> setBasicDef(int length,
            boolean nullable,
            Format format,
            T defaultValue,
            String description) {
        try {
            getConstraint().setBasicDef(length, nullable, format, defaultValue);
        } catch (Exception e) {
            if (doValidation)
                ManagedException.forward(e);
        }
        this.description = description;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue(Object beanInstance) {
        try {
            //using the default may result in problems on checking the value (e.g. in value-expression or isValue())
            return /*beanInstance == null && isVirtualAccess() ? getDefault() : */attribute.getValue(beanInstance);
        } catch (Exception ex) {
            LOG.error("error evaluating value for attribute '" + getName() + "'", ex);
            status = new Status(ex);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setId(boolean isId) {
        this.id = isId;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setUnique(boolean isUnique) {
        this.unique = isUnique;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return getConstraint().getLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scale() {
        return getConstraint().getScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int precision() {
        return getConstraint().getPrecision();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nullable() {
        return getConstraint().isNullable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean id() {
        return id;
    }

    @Override
    public boolean unique() {
        return unique;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Date> temporalType() {
        return temporalType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Format getFormat() {
        if (getConstraint().getFormat() == null) {
            Class<T> type = getType();
            if (Collection.class.isAssignableFrom(type)) {
                getConstraint().setFormat(new CollectionExpressionTypeFormat<T>((Class<T>) getGenericType(0)));
            } else if (Map.class.isAssignableFrom(type)) {
                getConstraint().setFormat(new MapExpressionFormat<T>((Class<T>) getGenericType(1)));
            } else if (type.isEnum()) {
                getConstraint().setFormat(FormatUtil.getDefaultFormat(type, true));
            } else if (BeanUtil.isStandardType(type)) {
//                this.format = FormatUtil.getDefaultFormat(type, true);
//                //not all types have default formats
//                if (this.format == null) {
//                    this.format = new GenericParser<T>(type);
//                }
                getConstraint().setFormat(Environment.get(BeanPresentationHelper.class).getDefaultRegExpFormat(this));
            } else {
                getConstraint().setFormat(new ValueExpressionTypeFormat<T>(type));
            }
        }
        return getConstraint().getFormat();
    }

    @Override
    public ValueExpression<T> getValueExpression() {
        Format f = getFormat();
        return f instanceof ValueExpressionFormat ? ((ValueExpressionFormat) f).getValueExpression() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        return attribute.getType();
    }

    @Override
    public Method getAccessMethod() {
        return attribute.getAccessMethod();
    }

    protected Class<T> getGenericType(int pos) {
        return (Class<T>) (getAccessMethod() != null ? BeanAttribute.getGenericType(getAccessMethod(), pos)
            : Object.class);
    }

    /**
     * isMultiValue
     * 
     * @return true, if the value type is a collection
     */
    @Override
    public boolean isMultiValue() {
        Class<?> type = getType();
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    /**
     * see {@link BeanClass#getActions()} and {@link BeanDefinition#isSelectable()}
     * 
     * @param beanValue attribute to evaluate
     * @return true, if bean type is selectable
     */
    public boolean isSelectable() {
        Class<T> type = getType();
        return isMultiValue()
            || (!BeanUtil.isStandardType(type) && BeanDefinition.getBeanDefinition(type).isSelectable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus isValid(T value) {
        return getConstraint().checkStatus(getId(), value);
    }

    /**
     * getParsedValue. throws a runtime exception on parsing error.
     * 
     * @param source text to parse
     * @return parsed value
     */
    public T getParsedValue(String source) {
//        if (StringUtil.isEmpty(source))
//            return null;
        T value = null;
        if (getFormat() != null)
            try {
                //the parser will decide, how to handle empty/null values
                value = (T) getFormat().parseObject(source);
            } catch (ParseException e) {
                ManagedException.forward(e);
            }
        else if (String.class.isAssignableFrom(getType()))
            value = (T) source;
        else
            throw new ManagedException("no format/parser available for field " + getName());
        return value;
    }

    /**
     * hasStatusError
     * 
     * @return true, if has status.error != null
     */
    public boolean hasStatusError() {
        return status.error() != null;
    }

    /**
     * {@inheritDoc}
     */
    public void check() {
        if (hasStatusError()) {
            throw new RuntimeException(status.error());
        }
    }

    /**
     * {@inheritDoc}
     */
    public T getDefault() {
        IConstraint<T> c = getConstraint();
        if (c.getDefault() == null && getAccessMethod() != null && !c.isNullable() && !generatedValue()
            && Environment.get("attribute.use.default", true)) {
            Object genType = getAccessMethod().getGenericReturnType();
            if (genType instanceof Class) {
                Class<T> gtype = (Class<T>) genType;
                if (BeanUtil.isStandardType(gtype)) {
                    if (BeanClass.hasDefaultConstructor(gtype))
                        getConstraint().setDefault((T) BeanClass.createInstance(gtype));
                }
            } else if (NumberUtil.isNumber(getType())) {
                getConstraint().setDefault((T) NumberUtil.getDefaultInstance((Class<Number>) getType()));
            } else if (Environment.isTestMode()) {
                /*
                 * to create new entities without user input, these fields are filled on test mode
                 */
                if (CharSequence.class.isAssignableFrom(getType())) {
                    c.setDefault((T) ("Y" + UUID.randomUUID().toString().substring(0, c.getLength() - 1)));
                } else if (BeanContainer.instance().isPersistable(getType())) {
                    c.setDefault((T) BeanContainer.instance().getBeans(getType(), 0, 1));
                }
            }
        }
        return getConstraint().getDefault();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        if (description == null && getName() != null)
            description = StringUtil.toFirstUpper(getName());
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setNumberDef(int scale, int precision) {
        getConstraint().setNumberDef(scale, precision);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setRange(Comparable<T> min, Comparable<T> max) {
        getConstraint().setRange(min, max);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setRange(Collection<T> allowedValues) {
        getConstraint().setRange(allowedValues);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setFormat(Format format) {
        getConstraint().setFormat(format);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setLength(int length) {
        getConstraint().setLength(length);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setScale(int scale) {
        getConstraint().setScale(scale);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setPrecision(int precision) {
        getConstraint().setPrecision(precision);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setNullable(boolean nullable) {
        getConstraint().setNullable(nullable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus getStatus() {
        return status;
    }

    /** see IVirtualDefinition#isRelation() */
    @Override
    public boolean isRelation() {
        return BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getType());
    }

    /**
     * if the bean instance is of type {@link ValueHolder}, the attribute is virtual - no special bean-attribute is
     * available, the attribute name is always {@link ValueHolder#getValue()} .
     * 
     * @return true, if the declaring class is of type {@link IValueAccess}.
     */
    public boolean isVirtual() {
        return attribute.isVirtual();
    }

    protected boolean isVirtualAccess() {
        return isVirtual() && getAccessMethod() != null;
    }

    /**
     * getPresentation
     * 
     * @return
     */
    @Override
    public IPresentable getPresentation() {
        //TODO: create presentation helper through MultipleInheritanceProxy
        if (presentable == null)
            presentable = Environment.get(BeanPresentationHelper.class).createPresentable(this);
        return presentable;
    }

    /**
     * setPresentation
     * 
     * @param label
     * @param type
     * @param style
     * @param enabler
     * @param visible
     * @param layout
     * @param layoutConstraints
     * @param description
     * @return
     */
    public IPresentable setPresentation(final String label,
            final int type,
            final int style,
            final IActivable enabler,
            final boolean visible,
            final Map<String, Object> layout,
            final Map<String, Object> layoutConstraints,
            final String description) {
        presentable = Environment.get(BeanPresentationHelper.class).createPresentable();
        presentable.setPresentation(label, type, style, enabler, visible, (Serializable) layout,
            (Serializable) layoutConstraints, description);
        return presentable;
    }

    /**
     * setPresentation
     * 
     * @param presentable
     */
    public void setPresentation(IPresentable presentable) {
        this.presentable = presentable;
    }

    /**
     * perhaps that definition was build yet. use BeanCollector.getColumnDefinition() to get the column - on first time
     * they will be created.
     * 
     * @return Returns the columnDefinition.
     */
    @Override
    public IPresentableColumn getColumnDefinition() {
        return columnDefinition;
    }

    /**
     * @param columnDefinition The columnDefinition to set.
     */
    public void setColumnDefinition(IPresentableColumn columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    /**
     * setColumnDefinition
     * 
     * @param index
     * @param sortIndex
     * @param sortUpDirection
     * @param width
     */
    public void setColumnDefinition(int index, int sortIndex, boolean sortUpDirection, int width) {
        this.columnDefinition = new ValueColumn(this, index, sortIndex, sortUpDirection, width);
        this.columnDefinition.setPresentable(getPresentation());
    }

    /**
     * @return Returns the doValidation.
     */
    public boolean isDoValidation() {
        return doValidation;
    }

    /**
     * @param doValidation The doValidation to set.
     */
    public void setDoValidation(boolean doValidation) {
        this.doValidation = doValidation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean composition() {
        return composition;
    }

    @Override
    public boolean cascading() {
        return cascading;
    }

    @Override
    public boolean generatedValue() {
        return generatedValue;
    }

    @Override
    public void setAsRelation(String relationChain) {
        new PrivateAccessor<AttributeDefinition<T>>(this).set("name", relationChain);
    }

///////////////////////////////////////////////////////////////////////////////
// Delegators to IAttribute
///////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public Class getDeclaringClass() {
        return attribute.getDeclaringClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return attribute.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Object instance, T value) {
        attribute.setValue(instance, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return attribute.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasWriteAccess() {
        return attribute.hasWriteAccess();
    }

    /**
     * looks for registered change handlers without creating an {@link EventController} instance.
     * 
     * @return
     */
    public boolean hasListeners() {
        return eventController != null && eventController.hasListeners();
    }

    /**
     * {@inheritDoc}
     */
    public EventController changeHandler() {
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(IAttribute<T> o) {
        return attribute.compareTo(o);
    }

    @Override
    public String toString() {
        return attribute.toString();
    }

    public String toDebugString() {
        return Util.toString(getClass(), "declaringClass: " + getType(), "temporal-type: " + temporalType, "name: "
            + getName(),
            "id: " + id, "unique: " + unique, "cascading: " + cascading, "composition: " + composition, "\nattribute: "
                + attribute, "\nstatus: "
                + status, "\nconstraints: "
                + constraint, "\npresentable: " + presentable);
    }
}
