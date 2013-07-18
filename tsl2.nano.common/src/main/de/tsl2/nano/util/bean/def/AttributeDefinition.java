/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 13, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import static de.tsl2.nano.util.bean.def.IPresentable.UNDEFINED;

import java.lang.reflect.Method;
import java.text.Format;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.IActivator;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.messaging.EventController;
import de.tsl2.nano.util.bean.BeanAttribute;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.BeanUtil;
import de.tsl2.nano.util.bean.IAttributeDef;
import de.tsl2.nano.util.bean.PrimitiveUtil;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Default(value = DefaultType.FIELD, required = false)
public class AttributeDefinition<T> extends BeanAttribute implements IAttributeDefinition<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1403875731423120506L;

    protected transient EventController eventController;
    private int length = UNDEFINED;
    private int scale = UNDEFINED;
    private int precision = UNDEFINED;
    private boolean nullable = true;
    private boolean id;
    private Class<? extends Date> temporalType;
    protected Format format;
    protected T defaultValue;
    protected String description;
    protected transient IStatus status;
    private Comparable<T> min;
    private Comparable<T> max;
    private transient Collection<T> allowedValues;
    private IPresentable presentable;
    private IPresentableColumn columnDefinition;
    private boolean doValidation = true;

    private static final Log LOG = LogFactory.getLog(AttributeDefinition.class);

    /**
     * constructor to be serializable
     */
    protected AttributeDefinition() {
        super();
    }

    protected AttributeDefinition(Method readAccessMethod) {
        super(readAccessMethod);
        defineDefaults();
    }

    /**
     * sets default properties through the {@link BeanContainer}, if available. These defaults may be read from
     * jpa-annotations.
     */
    private void defineDefaults() {
        if (BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getDeclaringClass())) {
            IAttributeDef def = BeanContainer.instance().getAttributeDef(getDeclaringClass(), getName());
            if (def != null) {
                LOG.debug("setting defaults from annotations for attribute: " + getName());
                setId(def.id());
                setBasicDef(def.length(), def.nullable(), null, null, null);
                setNumberDef(def.scale(), def.precision());
                temporalType = def.temporalType();
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
                this.format = new CollectionExpressionFormat<T>((Class<T>) getGenericType());
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
        return (Class<T>) super.getType();
    }

    /**
     * isMultiValue
     * 
     * @return true, if the value type is a collection
     */
    @Override
    public boolean isMultiValue() {
        return Collection.class.isAssignableFrom(getType());
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
                if (!PrimitiveUtil.isAssignableFrom(getType(), value.getClass())) {
                    status = Status.illegalArgument(getId(), value, getType());
                } else if (value instanceof String && parse((String) value) == null) {
                    status = Status.illegalArgument(getId(), value, "format '" + format + "'");
                } else if (!(value instanceof String) && getFormat().format(value) == null) {
                    status = Status.illegalArgument(getId(), value, "format '" + format + "'");
                } else if (min != null && min.compareTo(value) > 0) {
                    status = Status.illegalArgument(getId(), value, " greater than " + min);
                } else if (max != null && min.compareTo(value) > 0) {
                    status = Status.illegalArgument(getId(), value, " lower than " + max);
                } else if (length > 0 && value.toString().length() > length) {
                    status = Status.illegalArgument(getId(), value, " a maximum-length of " + length);
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
                //the parser will decide, how to handle emtpy/null values
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
        if (defaultValue == null)
            defaultValue = (T) readAccessMethod.getGenericReturnType();
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        if (description == null)
            description = getNameFU();
        return description;
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
     * getPresentation
     * 
     * @return
     */
    @Override
    public IPresentable getPresentation() {
        //TODO: create presentation helper through MultipleInheritanceProxy
        if (presentable == null)
            presentable = new Presentable(this);
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
        presentable = new Presentable();
        presentable.setPresentation(label, type, style, enabler, visible, layout, layoutConstraints, description);
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

}
