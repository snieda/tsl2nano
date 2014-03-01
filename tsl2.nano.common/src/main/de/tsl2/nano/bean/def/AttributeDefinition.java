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

import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.IActivator;
import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttribute;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.bean.PrimitiveUtil;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.util.PrivateAccessor;
import de.tsl2.nano.util.StringUtil;

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
    protected transient EventController eventController;
    private int length = UNDEFINED;
    private int scale = UNDEFINED;
    private int precision = UNDEFINED;
    private boolean nullable = true;
    private boolean id;
    private boolean unique;
    private Class<? extends Date> temporalType;
    protected Format format;
    protected T defaultValue;
    protected String description;
    protected transient IStatus status;
    private Comparable<T> min;
    private Comparable<T> max;
    @ElementList(inline = true, entry = "value", required = false)
    private transient Collection<T> allowedValues;
    @Element(type = Presentable.class, required = false)
    private IPresentable presentable;
    @Element(type = ValueColumn.class, required = false)
    private IPresentableColumn columnDefinition;
    private boolean doValidation = true;
    /** see {@link #composition()} */
    private boolean composition;
    /** see {@link #cascading()} */
    private boolean cascading;

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
            ForwardedException.forward(e);
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
                setNumberDef(def.scale(), def.precision());
                temporalType = def.temporalType();
                composition = def.composition();
                cascading = def.cascading();
            }
        }
        initDeserialization();
    }

    @Commit
    private void initDeserialization() {
        if (Enum.class.isAssignableFrom(getType()))
            allowedValues = CollectionUtil.getEnumValues((Class<Enum>) getType());

        status = IStatus.STATUS_OK;
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
        this.length = length;
        this.nullable = nullable;
        this.format = format;
        this.defaultValue = defaultValue;
        this.description = description;
        if (defaultValue != null && doValidation) {
            IStatus s = isValid(defaultValue);
            if (!s.ok())
                throw new FormattedException(s.message());
        }
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
     * setNumberDef
     * 
     * @param scale {@link #scale()}
     * @param precision {@link #precision()}
     */
    @Override
    public IAttributeDefinition<T> setNumberDef(int scale, int precision) {
        this.scale = scale;
        this.precision = precision;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setRange(Comparable<T> min, Comparable<T> max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public IAttributeDefinition<T> setRange(Collection<T> allowedValues) {
        this.allowedValues = allowedValues;
        return this;
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
    public IAttributeDefinition<T> setFormat(Format format) {
        this.format = format;
        return this;
    }

    /**
     * @return Returns the length.
     */
    @Override
    public int getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setLength(int length) {
        this.length = length;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * @return Returns the scale.
     */
    @Override
    public int getScale() {
        return scale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setScale(int scale) {
        this.scale = scale;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scale() {
        return scale;
    }

    /**
     * @return Returns the precision.
     */
    @Override
    public int getPrecision() {
        return precision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setPrecision(int precision) {
        this.precision = precision;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int precision() {
        return precision;
    }

    /**
     * @return Returns the nullable.
     */
    @Override
    public boolean isNullable() {
        return nullable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nullable() {
        return nullable;
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
        if (format == null) {
            Class<T> type = getType();
            if (Collection.class.isAssignableFrom(type)) {
                this.format = new CollectionExpressionFormat<T>((Class<T>) getGenericType(0));
            } else if (Map.class.isAssignableFrom(type)) {
                this.format = new MapExpressionFormat<T>((Class<T>) getGenericType(1));
            } else if (type.isEnum() || BeanUtil.isStandardType(type)) {
//                this.format = FormatUtil.getDefaultFormat(type, true);
//                //not all types have default formats
//                if (this.format == null) {
//                    this.format = new GenericParser<T>(type);
//                }
                this.format = Environment.get(BeanPresentationHelper.class).getDefaultRegExpFormat(this);
            } else {
                this.format = new ValueExpressionFormat<T>(type);
            }
        }
        return format;
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
        return isMultiValue() || (!BeanUtil.isStandardType(type) && BeanDefinition.getBeanDefinition(type).isSelectable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus isValid(T value) {
        IStatus status = IStatus.STATUS_OK;

        try {
            if (!nullable() && value == null) {
                status = Status.illegalArgument(getId(), value, "not null");
            } else if (value != null) {
                String fval = value instanceof String ? (String) value : getFormat().format(value);
                if (!PrimitiveUtil.isAssignableFrom(getType(), value.getClass())) {
                    status = Status.illegalArgument(getId(), fval, getType());
                } else if (value instanceof String && parse(fval) == null) {
                    status = Status.illegalArgument(getId(), fval, "format '" + format + "'");
                } else if (!(value instanceof String) && fval == null) {
                    status = Status.illegalArgument(getId(), fval, "format '" + format + "'");
                } else if (min != null && min.compareTo(value) > 0) {
                    status = Status.illegalArgument(getId(), fval, " greater than " + min);
                } else if (max != null && max.compareTo(value) < 0) {
                    status = Status.illegalArgument(getId(), fval, " lower than " + max);
                } else if (length > 0 && fval.length() > length) {
                    status = Status.illegalArgument(getId(), fval, " a maximum-length of " + length);
                }
            }
            //TODO: check numbers on scale and precision
        } catch (Exception ex) {
            status = Status.illegalArgument(getId(), value, ex);
        }
        if (!status.ok()) {
            LOG.warn(status);
        }
        return status;
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
                value = (T) format.parseObject(source);
            } catch (ParseException e) {
                ForwardedException.forward(e);
            }
        else if (String.class.isAssignableFrom(getType()))
            value = (T) source;
        else
            throw new FormattedException("no format/parser available for field " + getName());
        return value;
    }

    /**
     * parse the given text - will not throw any exception.
     * 
     * @param text text to parse
     * @return object yield from parsing given text - or null on any error
     */
    protected T parse(String text) {
        try {
            return getFormat() != null ? (T) format.parseObject(text) : null;
        } catch (Exception e) {
            LOG.warn(e.toString());
            //do nothing - the null return value indicates the error
            return null;
        }
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
    @Override
    public T getDefault() {
        if (defaultValue == null && getAccessMethod() != null)
            defaultValue = (T) getAccessMethod().getGenericReturnType();
        return defaultValue;
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

    @Override
    public Collection<T> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public T getMininum() {
        return (T) min;
    }

    @Override
    public T getMaxinum() {
        return (T) max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus getStatus() {
        return status;
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
            final IActivator enabler,
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

}
