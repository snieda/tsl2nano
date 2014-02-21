/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 8, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import static de.tsl2.nano.bean.def.IPresentable.ALIGN_CENTER;
import static de.tsl2.nano.bean.def.IPresentable.ALIGN_LEFT;
import static de.tsl2.nano.bean.def.IPresentable.ALIGN_RIGHT;
import static de.tsl2.nano.bean.def.IPresentable.STYLE_MULTI;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_DATE;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_INPUT_NUMBER;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_OPTION;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_SELECTION;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_TABLE;
import static de.tsl2.nano.bean.def.IPresentable.TYPE_TIME;
import static de.tsl2.nano.bean.def.IPresentable.UNDEFINED;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivator;
import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.format.GenericParser;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.messaging.ChangeEvent;
import de.tsl2.nano.messaging.IListener;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.Util;

/**
 * class to provide presentation definitions for sets of attributes.
 * <p/>
 * should be overridden if you create an own application.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class BeanPresentationHelper<T> {
    private static final Log LOG = LogFactory.getLog(BeanPresentationHelper.class);
    protected BeanDefinition<T> bean;
    protected Properties config = new Properties();
    protected Collection<IAction> appActions;
    protected Collection<IAction> sessionActions;
    protected Collection<IAction> pageActions;

    /*
     * IAttributeDefinitions
     */
    public static final String PROP_ID = "id";
    public static final String PROP_NAME = "name";
    public static final String PROP_FORMAT = "format";
    public static final String PROP_TYPE = "type";
    public static final String PROP_LENGTH = "length";
    public static final String PROP_MIN = "minimum";
    public static final String PROP_MAX = "maximum";
    public static final String PROP_DEFAULT = "default";
    public static final String PROP_VALUE = "value";
    public static final String PROP_ALLOWED_VALUES = "allowedValues";
    public static final String PROP_NULLABLE = "nullable";
    public static final String PROP_DOVALIDATION = "doValidation";
    public static final String PROP_LABEL = "label";
    public static final String PROP_DESCRIPTION = "description";

    /*
     * IPresentables
     */
//    public static final String PROP_TYPE = "type";
    public static final String PROP_STYLE = "style";
    public static final String PROP_LAYOUT = "layout";
    public static final String PROP_LAYOUTCONSTRAINTS = "layoutConstraints";
    public static final String PROP_ENABLER = "enabler";

    /*
     * Configuration
     */
    public static final String PREFIX_DEFRULE = "default.presentation.rule.";

    public static final String KEY_STR_TRUE = PREFIX_DEFRULE + "char.true";
    public static final String KEY_STR_FALSE = PREFIX_DEFRULE + "char.false";

    public static final String KEY_FILTER_FROM_LABEL = PREFIX_DEFRULE + "filter.min.label";
    public static final String KEY_FILTER_TO_LABEL = PREFIX_DEFRULE + "filter.max.label";

    public static final String KEY_TEXT_TAB = PREFIX_DEFRULE + "text.tab";
    public static final String KEY_TEXT_CR = PREFIX_DEFRULE + "text.cr";

    /** default comparator - used by lists and tables to do a default sort */
    public static Comparator STRING_COMPARATOR = NumberUtil.getNumberAndStringComparator(new DefaultFormat());

    public BeanPresentationHelper() {
    }

    /**
     * constructor
     * 
     * @param beanDefinition
     */
    protected BeanPresentationHelper(BeanDefinition<T> bean) {
        super();
        init(bean);
    }

    /**
     * init
     * 
     * @param bean
     */
    void init(BeanDefinition<T> bean) {
        this.bean = bean;
        config.setProperty(KEY_STR_TRUE, "J");
        config.setProperty(KEY_STR_FALSE, "N");
        config.setProperty(KEY_FILTER_FROM_LABEL, "min");
        config.setProperty(KEY_FILTER_TO_LABEL, "max");
    }

    public Object value(String name) {
        return config.get(name);
    }

    public String prop(String name) {
        return config.getProperty(name);
    }

    private static boolean isPresentableProperty(String propertyName) {
        return propertyName.equals(PROP_STYLE) || propertyName.equals(PROP_LAYOUT)
            || propertyName.equals(PROP_LAYOUTCONSTRAINTS)
            || propertyName.equals(PROP_ENABLER);
    }

    /**
     * change attribute with given key / values
     * 
     * @param attributeName attribute to change
     * @param keyValues properties to change
     * @return the helper itself
     */
    public BeanPresentationHelper<T> chg(String attributeName, Object... keyValues) {
        String n;
        for (int i = 0; i < keyValues.length; i += 2) {
            n = (String) keyValues[i];
            boolean isPresProp = isPresentableProperty(n);
            Object instance = isPresProp ? bean.getAttribute(attributeName).getPresentation()
                : bean.getAttribute(attributeName);
            BeanAttribute.getBeanAttribute(instance.getClass(), n).setValue(instance, keyValues[i + 1]);
        }
        return this;
    }

    /**
     * change
     * 
     * @param propertyName
     * @param newValue
     * @param attributeNames
     * @return
     */
    public BeanPresentationHelper<T> change(String propertyName, Object newValue, String... attributeNames) {
        attributeNames = attributeNames.length > 0 ? attributeNames : bean.getAttributeNames();
        boolean isPresProp = isPresentableProperty(propertyName);
        for (int i = 0; i < attributeNames.length; i++) {
            Object instance = isPresProp ? bean.getAttribute(attributeNames[i]).getPresentation()
                : bean.getAttribute(attributeNames[i]);
            BeanAttribute.getBeanAttribute(instance.getClass(), propertyName).setValue(instance, newValue);
        }
        return this;
    }

    /**
     * setDateAsText
     * 
     * @param attributeNames
     * @return
     */
    public BeanPresentationHelper<T> setDateAsText(String... attributeNames) {
        attributeNames = attributeNames.length > 0 ? attributeNames : bean.getAttributeNames();
        for (int i = 0; i < attributeNames.length; i++) {
            if (Date.class.isAssignableFrom(bean.getAttribute(attributeNames[i]).getType())) {
                bean.getAttribute(attributeNames[i])
                    .setFormat(RegExpFormat.createDateRegExp())
                    .setLength(10)
                    .getPresentation()
                    .setType(TYPE_INPUT);
            }
        }
        return this;
    }

    /**
     * setDateAsText
     * 
     * @param attributeNames
     * @return
     */
    public BeanPresentationHelper<T> setCharAsBoolean(String... attributeNames) {
        attributeNames = attributeNames.length > 0 ? attributeNames : bean.getAttributeNames();
        for (int i = 0; i < attributeNames.length; i++) {
            IAttributeDefinition attr = bean.getAttribute(attributeNames[i]);
            if (String.class.isAssignableFrom(attr.getType()) && attr instanceof IValueDefinition/* && attr.length() == 1*/) {
                BeanValue anfordernCharValue = BeanValue.getBeanValue(((IValueDefinition) attr).getInstance(),
                    attributeNames[i]);
                ValueMatcher vb = new ValueMatcher(anfordernCharValue, prop(KEY_STR_TRUE), prop(KEY_STR_FALSE));
                bean.getAttributeDefinitions().remove(attributeNames[i]);
                bean.addAttribute(vb,
                    IValueAccess.ATTR_VALUE,
                    Boolean.toString(false).length(),
                    false,
                    null,
                    false,
                    attributeNames[i],
                    null);
            } else {
                LOG.warn("attribute " + attr.getName()
                    + " is not a string-value - ignoring changing type from string to boolean");
            }
        }
        return this;
    }

    /**
     * adds an empty/invisible bean value for layout aspects.
     * 
     * @return created invisible bean value
     */
    public AttributeDefinition addSpaceValue() {
        AttributeDefinition<String> beanValue = bean.addAttribute("space-" + System.currentTimeMillis(),
            "[space]",
            null,
            null,
            null);
        beanValue.getPresentation().setVisible(false);
        return beanValue;
    }

    public int getDefaultHorizontalAlignment(IAttributeDefinition<?> beanAttribute) {
        if (beanAttribute.length() == 1) {
            return ALIGN_CENTER;
        }
        return getDefaultHorizontalAlignment((BeanAttribute) beanAttribute);
    }

    public int getDefaultHorizontalAlignment(BeanAttribute beanAttribute) {
        int alignment;
        final Class<?> type = beanAttribute.getType();
        if (beanAttribute instanceof IAttributeDefinition && ((IAttributeDefinition<?>) beanAttribute).length() == 1)
            alignment = ALIGN_RIGHT;
        else if (Number.class.isAssignableFrom(type)) {
            alignment = ALIGN_RIGHT;
        } else {
            alignment = ALIGN_LEFT;
        }
        return alignment;
    }

    /**
     * {@inheritDoc}
     */
    protected Format getDefaultFormat(BeanValue attr) {
        final IAttributeDef def = BeanContainer.instance().getAttributeDef(bean, attr.getName());
        if (def != null) {
            return getDefaultFormat(attr, attr.getInstance(), def.length(), def.scale(), def.precision());
        }
        return getDefaultFormat(attr, attr.getInstance(), UNDEFINED, UNDEFINED, UNDEFINED);
    }

    /**
     * overwrite this method to define special formats
     * 
     * @param attr bean attribute
     * @param bean bean instance
     * @return format, if available or null
     */
    protected Format getDefaultFormat(BeanAttribute attr, Object bean, int length, int scale, int precision) {
        if (BigDecimal.class.isAssignableFrom(attr.getType())) {
            final BigDecimal v = (!(bean instanceof Class) ? (BigDecimal) attr.getValue(bean) : null);
            if (v != null) {
                return RegExpFormat.createNumberRegExp(v);
            } else {
                final int l = length != UNDEFINED ? length : Environment.get("value.default.number64.length", 19);
                final int p = precision != UNDEFINED ? precision : Environment.get("value.default.number64.precision",
                    4);
                return RegExpFormat.createNumberRegExp(l, p);
            }
        } else if (Number.class.isAssignableFrom(attr.getType()) || double.class.isAssignableFrom(attr.getType())
            || float.class.isAssignableFrom(attr.getType())) {
            final int l = length != UNDEFINED ? length : Environment.get("value.default.number64.length", 19);
            final int p = precision != UNDEFINED ? precision : Environment.get("value.default.number64.length", 4);
            return RegExpFormat.createNumberRegExp(l, p);
        } else if (int.class.isAssignableFrom(attr.getType()) || long.class.isAssignableFrom(attr.getType())
            || short.class.isAssignableFrom(attr.getType())) {
            final int l = length != UNDEFINED ? length : Environment.get("value.default.number32.length", 10);
            return RegExpFormat.createNumberRegExp(l, 0);
        } else if (length != UNDEFINED) {
            return RegExpFormat.createAlphaNumRegExp(length, false);
        } else {
            return null;
        }
    }

    /**
     * tries to sort the given collection through the comparator, given by {@link #getComparator(BeanAttribute)}. if the
     * comparator or the collection is null, nothing will be done!
     * 
     * @param beanAttribute bean attribute, holding the collection
     * @param collection collection to sort
     * @return sorted list (new arraylist!) or the collection itself
     */
    protected <V> Collection<V> getDefaultSortedList(BeanAttribute beanAttribute, Collection<V> collection) {
        //already sorted ==> return
        if (collection == null || collection instanceof SortedSet) {
            return collection;
        }
        return CollectionUtil.getSortedList(collection, STRING_COMPARATOR, beanAttribute.getName(), false);
    }

    public int getDefaultType(AttributeDefinition<?> attr) {
        if (attr.temporalType() != null && Timestamp.class.isAssignableFrom(attr.temporalType()))
            return IPresentable.TYPE_DATE | IPresentable.TYPE_TIME;
        if (attr.temporalType() != null && Time.class.isAssignableFrom(attr.temporalType()))
            return IPresentable.TYPE_TIME;
        int type = getDefaultType((BeanAttribute) attr);
        if (attr.length() > Environment.get("field.min.multiline.length", 100))
            type |= IPresentable.TYPE_INPUT_MULTILINE;
        return type;
    }

    /**
     * evaluates the type of the given attribute to return the name of the constant to define the component.
     * 
     * if the type is not a simple type (string, date, boolean), the type list will be returned.
     * 
     * this method should only be used in a generating context.
     * 
     * @param attr beanattribute to evaluate
     * @return the type-constant (e.g.: 'IFieldDescriptor.TYPE_LIST')
     */
    public int getDefaultType(BeanAttribute attr) {
        int type = -1;

        if (Timestamp.class.isAssignableFrom(attr.getType())) {
            type = TYPE_DATE | TYPE_TIME;
        } else if (Time.class.isAssignableFrom(attr.getType())) {
            type = TYPE_TIME;
        } else if (Date.class.isAssignableFrom(attr.getType())) {
            type = TYPE_DATE;
        } else if (Boolean.class.isAssignableFrom(attr.getType()) || boolean.class.isAssignableFrom(attr.getType())) {
            type = TYPE_OPTION;
        } else if (attr.getType().isArray() || Collection.class.isAssignableFrom(attr.getType())) {//complex type --> list
            type = TYPE_TABLE;
        } else if (BeanUtil.isStandardType(attr.getType())) {
            type = TYPE_INPUT;
            if (Number.class.isAssignableFrom(attr.getType()))
                type |= TYPE_INPUT_NUMBER;
        } else {//complex type --> combo box list
            type = TYPE_SELECTION;
        }
        return type;
    }

    /**
     * getDefaultRegExpFormat
     * 
     * @param attribute definition to analyze
     * @return default {@link RegExpFormat} or null
     */
    public Format getDefaultRegExpFormat(AttributeDefinition<?> attribute) {
        Class<?> type = attribute.getType();
        Format regexp = null;
        if (BeanClass.isAssignableFrom(Number.class, type)) {
            if (BigDecimal.class.isAssignableFrom(type)) {
                final BigDecimal v = (BigDecimal) (attribute instanceof BeanValue ? ((BeanValue) attribute).getValue()
                    : null);
                if (v != null) {
                    return RegExpFormat.createNumberRegExp(v);
                } else {
                    int l = attribute.length() != UNDEFINED ? attribute.length()
                        : Environment.get("default.bigdecimal.length", 19);
                    int p = attribute.precision() != UNDEFINED ? attribute.precision()
                        : Environment.get("default.bigdecimal.precision", 4);

                    String currencyPattern = Environment.get("value.currency.length.precision", "11,2");
                    if (currencyPattern.equals(l + "," + p))
                        return RegExpFormat.createCurrencyRegExp();
                    else
                        return RegExpFormat.createNumberRegExp(l, p, type);
                }
            } else if (NumberUtil.isFloating(type)) {
                int l = attribute.length() != UNDEFINED ? attribute.length()
                    : Environment.get("default.bigdecimal.length", 19);
                int p = attribute.precision() != UNDEFINED ? attribute.precision()
                    : Environment.get("default.bigdecimal.precision", 4);
                return RegExpFormat.createNumberRegExp(l, p, type);
            } else if (NumberUtil.isInteger(type)) {
                int l = attribute.length() != UNDEFINED ? attribute.length()
                    : Environment.get("default.int.length", 10);
                return RegExpFormat.createNumberRegExp(l, 0, type);
            }
        } else if (BeanClass.isAssignableFrom(Date.class, type)) {
            if (BeanClass.isAssignableFrom(Timestamp.class, type)
                || (attribute.temporalType() != null && Timestamp.class.isAssignableFrom(attribute.temporalType())))
                regexp = RegExpFormat.createDateTimeRegExp();
            else if (BeanClass.isAssignableFrom(Time.class, type)
                || (attribute.temporalType() != null && Time.class.isAssignableFrom(attribute.temporalType())))
                regexp = RegExpFormat.createTimeRegExp();
            else
                regexp = RegExpFormat.createDateRegExp();
        } else if (BeanClass.isAssignableFrom(String.class, type)) {
            int l = attribute.length() != UNDEFINED ? attribute.length() : Environment.get("default.text.length", 100);
            regexp = RegExpFormat.createAlphaNumRegExp(l, false);
        } else {
            regexp = new GenericParser(attribute.getType());
        }
        return regexp;
    }

    /**
     * tries to fill a list with allowed values - getting relation collections through {@linkplain BeanContainer}.
     * please overwrite this method to avoid data transfers for unneeded validators.
     * 
     * @param beanAttribute bean attribute to evaluate a validator for.
     * @param bean bean instance (normally the presenters bean)
     * @return empty or filled collection
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected <V> Collection<V> getDefaultAllowedValues(AttributeDefinition<V> beanAttribute) {
        //TODO: move that to the attribute: IPresentable
        //OneToMany --> the relationObservable returns a collection
        //manyToOne --> call the service
        //to use the type as enum type, we can't use the generic type V!
        final Class type = beanAttribute.getType();
        Collection<?> manyToOneSelector = null;
        if (!Collection.class.isAssignableFrom(type)) {
            if (Enum.class.isAssignableFrom(type)) {
                manyToOneSelector = (Collection<V>) CollectionUtil.getEnumValues(type);
            } else if (!BeanUtil.isStandardType(type) && !BeanUtil.isByteStream(type)) {
                //TODO: don't load the relations at the moment :-(
                if (true)
                    return null;
                manyToOneSelector = getAttributeRelations(beanAttribute, type, Integer.MAX_VALUE);
                if (manyToOneSelector == null) {
                    manyToOneSelector = new LinkedList<V>();
                } else {
                    //do a sort - but only, if not the default comparator is set for this field!
                    manyToOneSelector = getDefaultSortedList(beanAttribute, (Collection<V>) manyToOneSelector);
                }
            }
        }
        return (Collection<V>) manyToOneSelector;
    }

    /**
     * Overwrite and implement this method to evaluate the list for the given editor.
     * 
     * @param beanAttribute attribute to evaluate the relations for.
     * @param type type to get relations for
     * @return validator (defining table columns)
     */
    protected <V> Collection<V> getAttributeRelations(BeanAttribute beanAttribute, Class<V> type, int maxResult) {
        return BeanContainer.instance().getBeans(type, 0, maxResult);
    }

    /**
     * override this method to define additional attributes for your bean. this implementation evaluates the id of the
     * bean and return it's attributes if the id is another bean!
     */
    public void defineAdditionalAttributes() {
        if (Environment.get("define.additional.attributes", true)) {
            BeanAttribute id = BeanContainer.getIdAttribute(bean.getClazz());
            if (id != null && !BeanUtil.isStandardType(id.getDeclaringClass()) && bean.hasAttribute(id.getName())) {
                bean.combineRelationAttributes(id.getName());
            }
        }
    }

    /**
     * checks, if given attribute should be presented.
     * 
     * @param attribute attribute to check.
     * @return true, if attribute should be presented.
     */
    public boolean isDefaultAttribute(BeanAttribute attribute) {
        AttributeDefinition<?> attr = (AttributeDefinition<?>) attribute;
        return (BeanContainer.instance() == null || BeanContainer.instance().hasPermission(attribute.getId(), null))
            && (matches("default.present.attribute.id",
                false) || !attr.id())
            && (matches("default.present.attribute.multivalue", true) || !attr.isMultiValue())
            && (matches("default.present.attribute.timestamp", false) || attr.temporalType() == null || !Timestamp.class
                .isAssignableFrom(attr.temporalType()));
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isDefaultDuty(BeanAttribute beanAttribute, Object bean) {
        final IAttributeDef attributeDef = BeanContainer.instance().getAttributeDef(bean, beanAttribute.getName());
        return attributeDef != null ? !attributeDef.nullable() : false;
    }

//    public BeanPresentationHelper<T> createDefaultSaveAction() {
//        bean.addAction(new CommonAction("vzlauf.speichern", IAction.MODE_DLG_OK, false) {
//            @Override
//            public Object action() throws Exception {
//                KionParameterUtil.clearCache();
//                return BeanContainer.instance().save(kistParameter);
//            }
//        });
//    }
//    
    /**
     * marks the given fields as duty
     * 
     * @param duty if true, field is mandatory
     * @param keys field keys. if empty, all not separating fields are duty!
     */
    protected void setDuty(boolean duty, String... keys) {
//        if (keys.length == 0) {
//            keys = bean.getAttributeNames();
//        }
//        for (final String k : keys) {
//            final IComponentDescriptor field = getField(k);
//            if (field.getType() == TYPE_SEPARATOR) {
//                continue;
//            }
//            field.setDuty(duty);
//            if (field.getValidator() instanceof MandatoryValidator) {
//                ((MandatoryValidator) field.getValidator()).setMandatory(duty);
//            } else if (field.getValidator() instanceof CompositeValidator) {
//                LOG.warn("can't set the mandatory flag on composite-validator");
//            } else if (field.getValidator() == null) {
//                field.setValidator(new MandatoryValidator(k, !duty));
//            } else {
//                LOG.warn("can't set the mandatory flag on " + field.getValidator());
//            }
//        }
    }

    /**
     * sets a layoutconstraint SWT.FILL horizontal.
     * 
     * @param keys field keys to set the layoutconstraints for. if no key is given, it will be done for all fields,
     *            having no layoutconstraints and not of type {@link IFieldConst#TYPE_SEPARATOR}!
     */
    protected void setLayoutHorFill(String... keys) {
//        boolean allFields = false;
//        if (keys.length == 0) {
//            keys = bean.getAttributeNames();
//            allFields = true;
//        }
//        for (int i = 0; i < keys.length; i++) {
//            if (allFields && layoutConstraints.get(keys[i]) == null && getField(keys[i]).getType() != TYPE_SEPARATOR) {
//                setLayoutConstraint(keys[i], -1, -1, -1, -1, SWT.FILL, -1, false, false);
//            }
//        }
    }

    /**
     * the destKey fields will have the same width as the srcKey field.
     * <p>
     * IMPORTANT: the srcKey field must have a layout constraint!
     * 
     * @param width width to set
     * @param destKeys field keys to set layoutconstraints with same width as srcKey field. if null, all fields will be
     *            changed!
     */
    protected void setLayoutEqualWidth(int width, String... destKeys) {
//        if (destKeys.length == 0) {
//            destKeys = getFieldKeysAsArray();
//        }
//        for (int i = 0; i < destKeys.length; i++) {
//            setLayoutConstraint(destKeys[i], width, -1, -1, -1);
//        }
    }

    /**
     * the destKey fields will have the same width as the srcKey field.
     * <p>
     * IMPORTANT: the srcKey field must have a layout constraint!
     * 
     * @param srcKey key of source field descriptor and layout constraint
     * @param destKeys field keys to set layoutconstraints with same width as srcKey field.
     */
    protected void setLayoutEqualWidth(String srcKey, String... destKeys) {
//        assert layoutConstraints.get(srcKey) != null : srcKey + " must have a layoutconstraint of type GridData!";
//        if (destKeys.length == 0) {
//            destKeys = getFieldKeysAsArray();
//        }
//        final int srcWidth = layoutConstraints.get(srcKey).widthHint;
//        for (int i = 0; i < destKeys.length; i++) {
//            setLayoutConstraint(destKeys[i], srcWidth, -1, -1, -1);
//        }
    }

    /**
     * sets the given layout constraints for the given fields. will overwrite the current layout constraints.
     * 
     * @param gd layout constraint to set
     * @param keys field keys to set layoutconstraints
     */
    protected void setLayout(Object gd, String... keys) {
//        if (keys.length == 0) {
//            keys = getFieldKeysAsArray();
//        }
//        for (int i = 0; i < keys.length; i++) {
//            layoutConstraints.put(keys[i], gd);
//        }
    }

    /**
     * defines dynamic component enabling. see {@link IComponentDescriptor#setComponentEnabler(IActivator)}.
     * 
     * @param a enabler to set
     * @param keys field keys to set enabler
     */
    protected void setEnabler(IActivator a, String... keys) {
//        if (keys.length == 0) {
//            keys = getFieldKeysAsArray();
//        }
//        for (int i = 0; i < keys.length; i++) {
//            getField(keys[i]).setComponentEnabler(a);
//        }
    }

    /**
     * convenience to set a format for several fields.
     * 
     * @param format new format
     * @param keys if empty, all field keys will be used
     */
    protected void setFormat(RegExpFormat format, String... keys) {
//        if (keys.length == 0) {
//            keys = getFieldKeysAsArray();
//        }
//        for (int i = 0; i < keys.length; i++) {
//            setFormat(keys[i], format);
//        }
    }

    /**
     * sets new properties for given fields
     * 
     * @param type field type
     * @param style field style
     * @param format field format
     * @param duty true, if duty
     * @param keys field keys
     */
    protected void set(int type, int style, RegExpFormat format, boolean duty, String... keys) {
//        if (keys.length == 0) {
//            keys = getFieldKeysAsArray();
//        }
//        for (int i = 0; i < keys.length; i++) {
//            setType(keys[i], type);
//            setStyle(keys[i], style);
//            setFormat(keys[i], format);
//        }
//        setDuty(duty, keys);
    }

    /**
     * convenience to set currency format. code assist will be hidden.
     * 
     * @param keys field keys
     */
    protected void setCurrencyFormat(String... keys) {
//        if (keys.length == 0) {
//            keys = getFieldKeysAsArray();
//        }
//        for (final String k : keys) {
//            setFormat(k, RegExpFormat.createCurrencyRegExp());
//            if (!isTypeDefOnly()) {
//                addStyle(k, SwtUtil.STYLE_HIST_INPUT_HIDE);
//            }
//        }
    }

    /**
     * convenience to set currency format. code assist will be hidden.
     * 
     * @param keys field keys
     */
    protected void setCurrencyFormatWithoutDecimalPlace(String... keys) {
//        if (keys.length == 0) {
//            keys = getFieldKeysAsArray();
//        }
//        for (final String k : keys) {
//            setFormat(k, RegExpFormat.createCurrencyWithoutDecimalPlaceRegExp());
//            if (!isTypeDefOnly()) {
//                addStyle(k, SwtUtil.STYLE_HIST_INPUT_HIDE);
//            }
//        }
    }

    /**
     * convenience method to set enable value for some fields. if no field names are given, all fields will be enabled.
     * 
     * @param enabled if true, if field will not be disabled. please see {@link #setEnabler(IActivator, String...)} and
     *            {@link #setEnabledVisibility(String, boolean, boolean)}.
     * @param keys field keys
     */
    protected void setEnabled(boolean enabled, String... keys) {
//        if (keys.length == 0) {
//            keys = getFieldKeysAsArray();
//        }
//        for (final String k : keys) {
//            getField(k).setEnabled(enabled);
//        }
    }

    /**
     * convenience method to change the type to a simple date field (see {@link FField#setType(int)}). a simple date
     * field is a text field with a regexp format that expects a standard date format.
     * 
     * @param key field keys
     */
    protected void setSimpleDateType(String... keys) {
//        if (keys.length == 0) {
//            final Set<String> keySet = editors.keySet();
//            final Collection<String> dateKeys = new LinkedList<String>();
//            for (final String k : keySet) {
//                if (editors.get(k).getType() == TYPE_DATE) {
//                    dateKeys.add(k);
//                }
//            }
//            keys = dateKeys.toArray(new String[dateKeys.size()]);
//        }
//        for (int i = 0; i < keys.length; i++) {
//            final String key = keys[i];
//            setType(key, TYPE_TEXT);
//            setFormat(key, RegExpFormat.createDateRegExp());
//            final IObservableValue v = getField(key).getObservableValue();
//            if (v != null && Date.class.isAssignableFrom((Class<?>) v.getValueType())) {
//                setValueType(key, Date.class);
//            }
//        }
    }

    /**
     * changes the type of the given field to be a combobox. the selectedValue will be packed into an
     * {@link ValueHolder}. Use this method only, if you don't have the right member inside your bean class.
     * 
     * @param key key of field to change
     * @param selectedValue previous selected value
     * @param allowedValues selectable list items
     * @param instance holding the desired/selected object
     */
    protected <T> ValueHolder<T> setListType(String key, T selectedValue, Collection<T> allowedValues) {
//        editors.remove(key + POSTFIX_LABEL);
//        setType(key, TYPE_LIST);
//        setValidator(key, new ObjectInListValidator(key, allowedValues));
//        final ValueHolder<T> valueHolder = new ValueHolder<T>(selectedValue);
//        setAttributeValue(key, valueHolder, ValueHolder.ATTR_VALUE);
//        return valueHolder;
        return null;
    }

    protected <E extends Enum<E>> OptionsWrapper<E> setEnumBooleanType(String key,
            int style,
            boolean subGroup,
            Class<E> enumType) {
        return setEnumBooleanType(key, style, subGroup, enumType.getEnumConstants());
    }

    /**
     * creates an enum radio button group up to ten enum values. the group will align its values horizontally.
     * 
     * @param <E> enum type
     * @param attrName field key
     * @param style (optional) {@link SWT#RADIO}, {@link SWT#PUSH}, {@link SWT#CHECK} --> multi selection!
     * @param subGroup whether to create a sub group panel
     * @param enumConstants enum constants
     * @return enum wrapper instance
     */
    protected <E> OptionsWrapper<E> setEnumBooleanType(String attrName, int style, boolean subGroup, E... enumConstants) {
        //analyze multi-style
        final boolean isMultiSelection = NumberUtil.hasBit(style, STYLE_MULTI);
        final IValueDefinition<E> attribute = ((Bean) bean).getAttribute(attrName);
        OptionsWrapper<E> enumWrapper;
        if (isMultiSelection) {
            if (!Collection.class.isAssignableFrom((Class<?>) attribute.getType())) {
                throw new FormattedException(
                    "tsl2nano.implementationerror",
                    new Object[] {
                        "IPresentable.STYLE_MULTI",
                        "If you define an EnumBooleanType with style IPresentable.STYLE_MULTI, you must have a bound bean attribute of type Collection!" });
            }
            enumWrapper = new MultiOptionsWrapper<E>(attribute, enumConstants);
        } else {
            enumWrapper = new OptionsWrapper<E>(attribute, enumConstants);
        }
        return setEnumBooleanType(attrName, attribute, style, subGroup, enumWrapper);
    }

    /**
     * wraps the model of the given field descriptor to a {@linkplain EnumStringBooleanWrapper}. The
     * {@link OptionsWrapper} instance will be set as field key (see {@link IComponentDescriptor#getBean()}. Usable to
     * create a radio-button-group. the resource bundle entries of the new boolean fields are the same as the original +
     * number starting with 1.<br>
     * e.g.: field: pflichtiger.geschlecht --> new fields: pflichtiger.geschlecht1, ...
     * 
     * @param attrName key of field to change
     * @param enumNames names to be bound to boolean values
     * @param style {@linkplain SWT#RADIO}, {@linkplain SWT#CHECK} or {@linkplain SWT#BUTTON}
     * @param subGroup whether to position all boolean values (radio buttons) to one line.
     * @return wrapper instance
     */
    protected <E> OptionsWrapper<E> setEnumBooleanType(String attrName,
            IValueAccess<E> ovalue,
            int style,
            boolean subGroup,
            OptionsWrapper<E> enumWrapper) {
//    setAttributeValue(attrName, enumWrapper, OptionsWrapper.ATTR_NAMES[0]);
//    bean.getAttribute(attrName).setType(TYPE_OPTION);
//    setStyle(attrName, style);
//    //validator is not needed any more (would have problems with new type)
//    setValidator(attrName, null);
//    //perhaps, the label was removed to be NO_LABEL.
//    if (style == SWT.RADIO) {
//        if (!NO_LABEL.equals(getField(attrName).getLabel())) {
//            final IComponentDescriptor label = new FLabelField(Messages.getString(attrName));
//            addField(label, attrName, true);
//            if (subGroup) {
//                addGroupingSuffix(label.getID());
//            }
//        }
//        setLabel(attrName, Messages.getString(attrName + 1));
//    }
//    if (subGroup) {
//        addGroupingSuffix(attrName);
//    }
//
//    //create new fields!
//    final Object[] enumConstants = enumWrapper.getEnumConstants();
//    IComponentDescriptor newEditor;
//    String keyBefore = attrName;
//    String idLabel;//used for both: id and label
//    for (int i = 1; i < enumConstants.length; i++) {
//        final BeanAttribute attribute = BeanAttribute.getBeanAttribute(OptionsWrapper.class,
//            OptionsWrapper.ATTR_NAMES[i]);
//        idLabel = attrName + (i + 1);
//        LOG.debug("creating enum-boolean-field: " + idLabel);
//        newEditor = createEditor(idLabel, attribute, enumWrapper);
//        addField(newEditor, keyBefore, false);
//        setLabel(idLabel, Messages.getString(idLabel));
//        setStyle(idLabel, style);
//        if (subGroup && i < enumConstants.length - 1) {
//            addGroupingSuffix(idLabel);
//        }
//        keyBefore = newEditor.getID();
//    }
//    //create an END field to separate several radio button groups
//    newEditor = new FHiddenField();
//    addField(newEditor, keyBefore, false);
//    final GridData gd = new GridData();
//    gd.exclude = true;
//    layoutConstraints.put(newEditor.getID(), gd);

        return enumWrapper;
    }

    /**
     * convenience method add a listener to all enumvalues. only usable, if
     * {@link #setEnumBooleanType(String, IObservableValue, int, boolean, OptionsWrapper)} was called before. the
     * wrapper instance is hold as field bean.
     * 
     * @param wrapper wrapper instance to have an instance using booleans for each enum type that was selected.
     * @param listener user defined listener to act on selection changes.
     * @param enumType enum type to evaluate the enum values from
     */
    protected void setEnumBooleanListener(OptionsWrapper<?> wrapper, IListener<ChangeEvent> listener, Class<?> enumType) {
        setEnumBooleanListener(wrapper, listener, (Enum[]) enumType.getEnumConstants());
    }

    /**
     * convenience method add a listener to all enumvalues.
     * 
     * @param wrapper wrapper instance to have an instance using booleans for each enum type that was selected.
     * @param listener user defined listener to act on selection changes.
     * @param enumConstants enum constants
     */
    @SuppressWarnings("unchecked")
    protected void setEnumBooleanListener(OptionsWrapper<?> wrapper,
            IListener<ChangeEvent> listener,
            Enum[] enumConstants) {
        for (int i = 0; i < enumConstants.length; i++) {
            LOG.debug("adding valuechange-listener for attribute: " + OptionsWrapper.ATTR_NAMES[i]);
            BeanValue.getBeanValue(wrapper, OptionsWrapper.ATTR_NAMES[i]).changeHandler().addListener(listener);
            //TODO: listener bei dispose evtl. wieder abbauen!
        }
    }

    /**
     * tries to evalute the best attribute as unique bean presenter. this algorithmus is used, if no attribute filter
     * (defining the attribute order) was defined.
     * 
     * @return bean presenting attribute name
     */
    protected String getBestPresentationAttribute() {
        if (bean.isDefault() && bean.getAttributeDefinitions().size() > 0) {
            Class<?> bestType = Environment.get("bean.best.attribute.type", String.class);
            String bestRegexp = Environment.get("bean.best.attribute.regexp", ".*(name|bezeichnung).*");
            int bestminlength = Environment.get("bean.best.attribute.minlength", 2);
            int bestmaxlength = Environment.get("bean.best.attribute.maxlength", 50);
            String[] names = bean.getAttributeNames();

            /*
             * create a map with matching levels and their attribute indexes.
             */
            int ml = 0;//matchinglevel: 5 criterias to match
            NavigableMap<Integer, Integer> levels = new TreeMap<Integer, Integer>();
            for (int i = 0; i < names.length; i++) {
                IAttributeDefinition attr = bean.getAttribute(names[i]);
                if (attr.getType().isInterface() || !BeanClass.hasDefaultConstructor(attr.getType()))
                    ml = 0;
                else if (isDefaultAttribute((BeanAttribute) attr) && !attr.isMultiValue()) {
                    ml = (1 << 7) * (attr.id() ? 1 : 0);
                    ml |= (1 << 6) * (attr.unique() ? 1 : 0);
                    ml |= (1 << 5) * (!attr.nullable() ? 1 : 0);
                    ml |= (1 << 4) * (BeanClass.isAssignableFrom(bestType, attr.getType()) ? 1 : 0);
                    ml |= (1 << 3) * (names[i].matches(bestRegexp) ? 1 : 0);
                    ml |=
                        (1 << 2)
                            * (attr.length() == -1
                                || (attr.length() >= bestminlength && attr.length() <= bestmaxlength) ? 1
                                : 0);
                } else {
                    ml = 0;
                }
                levels.put(ml, i);
            }
            /*
             * we don't have direct access to the database, so we can't read the
             * unique indexes. but the best matched attribute should be unique.
             * we solve this loading a 'group by' looking for duplicated attributes.
             */
            NavigableSet<Integer> keySet = levels.descendingKeySet();
            if (bean.isPersistable()) {
                Collection<Long> grouping;
                final String ALIAS = "XXX";
                String query = "select max(count(" + ALIAS + ")) from " + bean.getName() + " group by " + ALIAS;
                String q;
                Long maxCount;
                int i = 0;
                try {
                    for (Iterator<Integer> it = keySet.iterator(); it.hasNext();) {
                        //check data to seam unique
                        i = levels.get(it.next());
                        q = query.replace(ALIAS, names[i]);
                        grouping = BeanContainer.instance().getBeansByQuery(q, false, null);
                        maxCount = grouping.size() > 0  ? grouping.iterator().next() : null;
                        if (Util.isEmpty(maxCount) || maxCount.intValue() < 2)
                            break;
                        it.remove();
                    }
                } catch (Exception ex) {
                    BeanAttribute id = bean.getIdAttribute();
                    if (id != null) {
                        LOG.warn("couldn't check attribute for unique data: " + bean.getName() + "." + names[i]
                            + ". Using id-attribute " + id.getId(), ex);
                        return id.getName();
                    } else {
                        LOG.warn("couldn't check attribute for unique data: " + bean.getName() + "." + names[i], ex);
                        return names[i];
                    }
                }
                if (levels.isEmpty()) {
                    BeanAttribute id = bean.getIdAttribute();
                    LOG.warn("No unique field as value-expression found for " + bean
                        + " available fields: " + StringUtil.toString(names, 100)
                        + ". Using id-attribute " + id.getId());
                    return id.getName();
                }
            }
            /*
             * get the index of the highest matching level attribute
             */
            return names[levels.get(keySet.first())];
        } else if (bean.getAttributeDefinitions().size() > 0) {
            return bean.getAttributeNames()[0];
        } else if (BeanUtil.hasToString(bean.getClazz())) {
            return null;//TODO: -->toString()
        } else {
            return null;
        }
    }

    /**
     * creates a csv like text output - only available on {@link Bean} having an instance!
     * 
     * @return csv string
     */
    public String getSimpleTextualPresentation() {
        final String TAB = config.getProperty(KEY_TEXT_TAB, "\t");
        final String CR = config.getProperty(KEY_TEXT_CR, "\n");
        StringBuilder str = new StringBuilder();
        str.append(bean.getName() + CR);
        if (bean.isMultiValue())
            return fillCollectorPresentation(str, TAB, CR).toString();
        else
            return fillBeanPresentation(str, TAB, CR).toString();
    }

    /**
     * used by {@link #getSimpleTextualPresentation()}
     * 
     * @param str text instance
     * @param TAB div between fields
     * @param CR div between beans
     * @return text instance filled with bean collector data
     */
    protected StringBuilder fillCollectorPresentation(StringBuilder str, String TAB, String CR) {
        String[] names = bean.getAttributeNames();
        String n;
        for (int i = 0; i < names.length; i++) {
            IAttributeDefinition<?> attr = bean.getAttribute(names[i]);
            n = attr.getColumnDefinition() != null ? attr.getColumnDefinition().getName() : TAB;
            str.append(n + TAB);
        }
        str.append(CR);
        BeanCollector<?, T> collector = (BeanCollector<?, T>) bean;
        Collection<T> data = collector.getCurrentData();
        if (data != null) {
            for (T t : data) {
                for (int i = 0; i < names.length; i++) {
                    str.append(collector.getColumnText(t, i) + TAB);
                }
                str.append(CR);
            }
        }
        return str;
    }

    /**
     * fillBeanPresentation
     * 
     * @param str
     * @param TAB
     * @param CR
     * @return
     */
    protected StringBuilder fillBeanPresentation(StringBuilder str, final String TAB, final String CR) {
        String[] names = bean.getAttributeNames();
        for (int i = 0; i < names.length; i++) {
            IAttributeDefinition<?> attr = (IAttributeDefinition<?>) bean.getAttribute(names[i]);
            str.append(attr.getPresentation().getLabel() + TAB + " ??? "/*attr.getValue(bean.get)*/+ CR);
        }
        return str;
    }

    /**
     * to be overwritten
     * 
     * @param message
     * @return the message itself
     */
    public String decorate(String message) {
        return message;
    }

    /**
     * to be overwritten
     * 
     * @param message
     * @return the message itself
     */
    public String page(String message) {
        return message;
    }

    public void reset() {
        Environment.reload();
        BeanDefinition.clearCache();
        Bean.clearCache();
        BeanValue.clearCache();
    }

    /**
     * creates extended actions like 'print', 'help', 'export', 'select-all', 'deselect-all' etc.
     */
    public Collection<IAction> getApplicationActions() {
        if (appActions == null) {
            if (bean == null)
                return new LinkedList<IAction>();
            appActions = new ArrayList<IAction>(10);

            appActions.add(new SecureAction(bean.getClazz(),
                "environment",
                IAction.MODE_UNDEFINED,
                false,
                "icons/cascade.png") {
                @Override
                public Object action() throws Exception {
                    return "./";
                }
            });

            appActions.add(new SecureAction(bean.getClazz(),
                "reset",
                IAction.MODE_UNDEFINED,
                false,
                "icons/reload.png") {
                @Override
                public Object action() throws Exception {
                    reset();
                    return page("configuration refreshed");
                }
            });
        }
        return appActions;
    }

    /**
     * creates extended actions like 'print', 'help', 'export', 'select-all', 'deselect-all' etc.
     */
    public Collection<IAction> getSessionActions() {
        if (sessionActions == null) {
            if (bean == null)
                return new LinkedList<IAction>();
            sessionActions = new ArrayList<IAction>(1);
            sessionActions.add(new SecureAction(bean.getClazz(),
                "logout",
                IAction.MODE_UNDEFINED,
                false,
                "icons/turnoff.png") {
                @Override
                public Object action() throws Exception {
                    Environment.persist();
                    BeanDefinition.dump();
                    return page("user logged out!");
                }
            });
        }
        return sessionActions;
    }

    /**
     * creates extended actions like 'print', 'help', 'export', 'select-all', 'deselect-all' etc.
     */
    public Collection<IAction> getPageActions() {
        if (pageActions == null) {
            if (bean == null)
                return new LinkedList<IAction>();
            pageActions = new ArrayList<IAction>(10);

            if (bean.isMultiValue()) {
                final BeanCollector<?, T> collector = (BeanCollector<?, T>) bean;
                if (BeanContainer.instance().isPersistable(collector.getType())) {
                    pageActions.add(new SecureAction(bean.getClazz(),
                        "back",
                        IAction.MODE_UNDEFINED,
                        false,
                        "icons/back.png") {
                        @Override
                        public Object action() throws Exception {
                            collector.getBeanFinder().previous();
                            return bean;
                        }

                        @Override
                        public boolean isEnabled() {
                            return super.isEnabled() && collector.getCurrentData().size() > 0;
                        }
                    });

                    pageActions.add(new SecureAction(bean.getClazz(),
                        "forward",
                        IAction.MODE_UNDEFINED,
                        false,
                        "icons/forward.png") {
                        @Override
                        public Object action() throws Exception {
                            collector.getBeanFinder().next();
                            return bean;
                        }

                        @Override
                        public boolean isEnabled() {
                            return super.isEnabled() && collector.getCurrentData().size() > 0;
                        }
                    });
                }
                pageActions.add(new SecureAction(bean.getClazz(),
                    "selectall",
                    IAction.MODE_UNDEFINED,
                    false,
                    "icons/cascade.png") {
                    @Override
                    public Object action() throws Exception {
                        ISelectionProvider<T> selectionProvider =
                            ((IBeanCollector<?, T>) bean).getSelectionProvider();
                        if (selectionProvider != null) {
                            selectionProvider.getValue().clear();
                            selectionProvider.getValue().addAll(((IBeanCollector<?, T>) bean).getCurrentData());
                        }
                        return bean;
                    }

                    @Override
                    public boolean isEnabled() {
                        return super.isEnabled() && ((IBeanCollector<?, T>) bean).getCurrentData().size() > 0;
                    }
                });

                pageActions.add(new SecureAction(bean.getClazz(),
                    "deselectall",
                    IAction.MODE_UNDEFINED,
                    false,
                    "icons/new.png") {
                    @Override
                    public Object action() throws Exception {
                        ISelectionProvider<T> selectionProvider =
                            ((IBeanCollector<?, T>) bean).getSelectionProvider();
                        if (selectionProvider != null) {
                            selectionProvider.getValue().clear();
                        }
                        return bean;
                    }

                    @Override
                    public boolean isEnabled() {
                        return super.isEnabled() && ((IBeanCollector<?, T>) bean).getCurrentData().size() > 0;
                    }
                });

                pageActions.add(new SecureAction(bean.getClazz(),
                    "switchrelations",
                    IAction.MODE_UNDEFINED,
                    false,
                    "icons/links.png") {
                    @Override
                    public Object action() throws Exception {
                        collector.setMode(NumberUtil.toggleBits(collector.getWorkingMode(),
                            IBeanCollector.MODE_SHOW_MULTIPLES));
                        collector.getSearchAction().activate();
                        return bean;
                    }

                    @Override
                    public boolean isEnabled() {
                        return super.isEnabled() && ((IBeanCollector<?, T>) bean).getCurrentData().size() > 0;
                    }
                });
            }
            pageActions
                .add(new SecureAction(bean.getClazz(), "print", IAction.MODE_UNDEFINED, false, "icons/print.png") {
                    @Override
                    public Object action() throws Exception {
                        return Environment.get(IPageBuilder.class).build(bean, null, false);
                    }
                });

            pageActions
                .add(new SecureAction(bean.getClazz(), "export", IAction.MODE_UNDEFINED, false, "icons/view.png") {
                    @Override
                    public Object action() throws Exception {
                        return getSimpleTextualPresentation();
                    }
                });

            pageActions.add(new SecureAction(bean.getClazz(),
                "document",
                IAction.MODE_UNDEFINED,
                false,
                "icons/images_all.png") {
                File exportFile = new File(Environment.get(bean.getName() + ".export.file",
                    Environment.getConfigPath() + bean.getName() + ".rtf"));

                @Override
                public Object action() throws Exception {
                    //TODO: file selection, and ant-variable insertion...
                    String content = String.valueOf(FileUtil.getFileData(exportFile.getPath(), null));
                    content = StringUtil.insertProperties(content, BeanUtil.toValueMap(((Bean) bean).getInstance()));
                    FileUtil.writeBytes(content.getBytes(), exportFile.getParent() + ".new", false);
                    return "document '" + exportFile.getPath() + "' was filled with data of bean " + bean;
                }

                @Override
                public String getLongDescription() {
                    return "exporting (see environment.properties) to: " + exportFile.getPath();
                }

                @Override
                public boolean isEnabled() {
                    return exportFile.canRead();
                }
            });

            pageActions.add(new SecureAction(bean.getClazz(),
                "help",
                IAction.MODE_UNDEFINED,
                false,
                "icons/trust_unknown.png") {
                String helpFileName = Environment.getConfigPath() + bean.getName().toLowerCase() + ".help.html";

                @Override
                public Object action() throws Exception {
                    String helpFile = null;
                    if (new File(helpFileName).canRead())
                        helpFile = String.valueOf(FileUtil.getFileData(helpFileName, null));
                    return helpFile != null ? helpFile : page("No help found (" + helpFileName + ")");
                }

                @Override
                public boolean isEnabled() {
                    return new File(helpFileName).canRead();
                }
            });

        }
        return pageActions;
    }

    /**
     * override this method to define a special command handler to be used e.g. by BeanCollector.editItem() - perhaps
     * opening a detail dialog.
     * 
     * @param beanToEdit instance to be edit in a detail dialog.
     * @return result of bean editing.
     */
    public <E> E startUICommandHandler(final E beanToEdit) {
        LOG.debug("beancollector.edit: no commandhandler defined in environment to edit a bean - doing nothing!");
        return beanToEdit;
    }

    protected final boolean matches(String patternKey, boolean any) {
        return bean.toString().matches(Environment.get(patternKey, any ? ".*" : "XXXXXXXXXX"));
    }

    /**
     * createPresentable
     * 
     * @return
     */
    public IPresentable createPresentable() {
        return new Presentable();
    }

    /**
     * createPresentable
     * 
     * @param attr
     * @return
     */
    public IPresentable createPresentable(AttributeDefinition<?> attr) {
        return new Presentable(attr);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bean + ")";
    }
}
