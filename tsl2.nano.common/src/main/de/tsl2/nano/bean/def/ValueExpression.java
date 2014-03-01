/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 2, 2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.Environment;
import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.PrimitiveUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.format.FormatUtil;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.Util;
import de.tsl2.nano.util.operation.IConverter;

/**
 * Is able to parse a string to create an object. the string will be split into one or more attribute values to be found
 * by example through the {@link BeanContainer}. Please see {@link #ValueExpression(String)} for more informations about
 * the {@link #expression}.
 * <p/>
 * TODO: implement and test printf format
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class ValueExpression<TYPE> implements IValueExpression<TYPE>, IConverter<TYPE, String>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 9157362251663475852L;
    /** optional expression prefix to define the kind of expression */
    private static final String PREFIX_PRINTF_FORMAT = "printf:";

    private static final Log LOG = LogFactory.getLog(ValueExpression.class);

    /**
     * the expression to get the attribute names from - to be used as 'toString' output and to create an object from a
     * unique string. the expression is a message expression of type {@link MessageFormat}. If you give the prefix
     * {@link #PREFIX_PRINTF_FORMAT}, the expression has to be of style {@link Formatter}. In both cases the argument
     * variables have to be replaced by attribute names!
     * <p/>
     * Example using {@link MessageFormat}:
     * 
     * <pre>
     * { birthday, date, medium }
     * </pre>
     * 
     * Example using {@link Formatter}:
     * 
     * <pre>
     * %birthday$TD
     * </pre>
     */
    @Attribute
    String expression;

    /** format, the transformed expression */
    transient String format;

    /** only used to convert/parse from string to object */
    @Attribute
    Class<TYPE> type;
    /** attributes, extracted from expression */
    transient String[] attributes;
    /** separation characters between attributes - extracted from expression */
    transient String[] attributeSplitters;
    /**
     * true, if expression is a standard message format of {@link MessageFormat}, false , if it starts with
     * {@link #PREFIX_PRINTF_FORMAT}
     */
    transient boolean isMessageFormat;
    /** true, if at least one attribute were found in expression */
    transient boolean hasArguments;

    /** should be true, if {@link #type} is persistable (see {@link BeanContainer#isPersistable(Class)} */
    transient boolean isPersistable = false;

    /**
     * constructor to be serializable
     */
    protected ValueExpression() {
        super();
    }

    public ValueExpression(String expression) {
        this(expression, null);
    }

    /**
     * constructor
     * 
     * @param expression please see {@link #expression} for more informations.
     */
    public ValueExpression(String expression, Class<TYPE> type) {
        super();
        init(expression, type);
    }

    @Commit
    private void initDeserialization() {
        init(expression, type);
    }

    /**
     * init
     * 
     * @param expression see {@link #expression}
     * @param type
     */
    private void init(String expression, Class<TYPE> type) {
        this.expression = expression;
        isPersistable = type != null && BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(type);
        isMessageFormat = !expression.startsWith(PREFIX_PRINTF_FORMAT);
        if (isMessageFormat()) {
            hasArguments = expression.contains("{") && expression.contains("}");
            attributes = extractAttributeNamesMF(expression);
            if (type != null) {
                attributeSplitters = extractAttributeSplittersMF(expression);
                this.type = type;
            }
        } else {
            hasArguments = expression.contains("%") && expression.contains("$");
            expression = expression.substring(PREFIX_PRINTF_FORMAT.length());
            attributes = extractAttributeNames(expression);
            if (type != null) {
                attributeSplitters = extractAttributeSplitters(expression);
                this.type = type;
            }
        }
        LOG.debug("new ValueExpression for type " + type + ": " + expression);
    }

    /**
     * Extension for {@link Serializable}
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(expression, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TYPE from(String toValue) {
        if (type == null)
            throw FormattedException
                .implementationError(
                    "The conversion from string to object is only available, if the ValueExpression was created with a class type argument!",
                    "type of value-expression '" + toString() + "' is null");
        //if type is object we return the value itself - it's an instanceof Object
        if (type.isAssignableFrom(Object.class))
            return (TYPE) toValue;
        if (Util.isEmpty(toValue))
            return null;
        TYPE exampleBean = createInstance(toValue);
        //TODO: how-to extract the attribute-name information from expression?
        Bean<TYPE> b = (Bean<TYPE>) Bean.getBean((Serializable) exampleBean);
        String[] attributeValues = getAttributeValues(toValue);
        for (int i = 0; i < attributes.length; i++) {
            IValueDefinition<?> attr = b.getAttribute(attributes[i]);
            //if attribute is a relation, we resolve it
            if (BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(attr
                .getType())) {
                Object v =
                    BeanDefinition.getBeanDefinition(attr.getType()).getValueExpression().from(attributeValues[i]);
                b.setValue(attributes[i], v);
            } else {//here we are able to directly parse the string to a value
                b.setParsedValue(attributes[i], attributeValues[i]);
            }
        }

        if (isPersistable) {//check for unique!
            Collection<TYPE> beansByExample = BeanContainer.instance().getBeansByExample(exampleBean);
            if (beansByExample.size() > 1) {
                LOG.error("string-to-object-parser: found more than one object:\n"
                    + StringUtil.toFormattedString(beansByExample, 100, true));
                throw new FormattedException("tsl2nano.multiple.items", new Object[] { toValue, type, type });
            }
            return beansByExample.size() > 0 ? beansByExample.iterator().next() : null;
        } else {
            return exampleBean;
        }
    }

    /**
     * implements the interface method {@link #getValue(Object)} by {@link IValueExpression} and delegates directly to
     * {@link #from(String)} using object instance {@link #toString()} as value. thrown.
     */
    @Override
    public TYPE getValue(Object instance) {
        return from(Util.asString(instance));
    }

    /**
     * creates a new instance from 'toValue' (user-input).
     * 
     * @param toValue
     * @return
     */
    protected TYPE createInstance(String toValue) {
        TYPE instance;
        if (type.isInterface())
            instance = BeanProxy.createBeanImplementation(type, null, null, Environment.get(ClassLoader.class));
        else if (BeanUtil.isStandardType(type))
            instance = PrimitiveUtil.create(type, toValue);
        else if (BeanClass.hasStringConstructor(type))
            instance = (TYPE) BeanClass.createInstance(type, toValue);
        else
            instance = (TYPE) BeanClass.createInstance(type);
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String to(TYPE fromValue) {
        if (fromValue == null)
            return "";
        if (hasArguments) {
            Map<String, Object> valueMap = BeanUtil.toValueMap(fromValue, false, false, true, attributes);
            Object[] args = valueMap.values().toArray();
            StringUtil.replaceNulls(args, false);
            //check for entity beans to resolve their format recursive through it's valueexpression
            preformatBeans(args);
            //if object 'fromValue' is empty or not filled, we fill questionmarks to the formatted text
            if (args.length < attributes.length) {
                Object[] arr = new Object[attributes.length];
                Arrays.fill(arr, "?");
                args = arr;
            }
            return isMessageFormat() ? MessageFormat.format(format, args) : String.format(format, args);
        } else {
            //lazy workaround...
            return !Util.isEmpty(format) && !format.equals("Object") ? format : FormatUtil.getDefaultFormat(fromValue,
                false).format(fromValue);//Util.asString(fromValue);
        }
    }

    protected void preformatBeans(Object[] args) {
        if (!BeanContainer.isInitialized())
            return;
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null && BeanContainer.instance().isPersistable(args[i].getClass()))
                args[i] = Bean.getBean((Serializable) args[i]).toString();
        }
    }

    private final boolean isMessageFormat() {
        return isMessageFormat;
    }

    /**
     * extractAttributeNames
     * 
     * @param expression to extract bean attribute names from
     * @return attribute names, contained in expression
     */
    protected String[] extractAttributeNames(String expression) {
        Collection<String> attributes = new ArrayList<String>();
        int i = 1;
        String attrName;
        StringBuilder expr = new StringBuilder(expression);
        while ((attrName = StringUtil.extract(expr, "%" + BeanAttribute.REGEXP_ATTR_NAME + "\\$", "%" + i + "$"))
            .length() > 0) {
            attributes.add(attrName.substring(1, attrName.length() - 1));
            i++;
        }
        this.format = expr.toString();
        return attributes.toArray(new String[0]);
    }

    /**
     * extractAttributeSplitters
     * 
     * @param expression extracts the placeholders between the attribute names
     * @return array of placeholders
     */
    protected String[] extractAttributeSplitters(String expression) {
        Collection<String> splitters = new ArrayList<String>();
        int i = 0;
        String splitter;
        StringBuilder expr = new StringBuilder(expression);
        while (i != -1 && (splitter = StringUtil.substring(expr, "$", "%", i)).length() > 0) {
            splitters.add(splitter);
            if (i == 0)
                i = expr.indexOf("$");
            i = expr.indexOf("$", i + 1);
        }
        return splitters.toArray(new String[0]);
    }

    /**
     * extractAttributeNames for {@link MessageFormat}
     * 
     * @param expression to extract bean attribute names from
     * @return attribute names, contained in expression
     */
    protected String[] extractAttributeNamesMF(String expression) {
        Collection<String> attributes = new ArrayList<String>();
        int i = 0;
        String attrName;
        StringBuilder expr = new StringBuilder(expression);
        while ((attrName = StringUtil.extract(expr, "\\{" + BeanAttribute.REGEXP_ATTR_NAME + "\\}", "{" + i + "}"))
            .length() > 0) {
            attributes.add(attrName.substring(1, attrName.length() - 1));
            i++;
        }
        this.format = expr.toString();
        return attributes.toArray(new String[0]);
    }

    /**
     * extractAttributeSplitters for {@link MessageFormat}
     * 
     * @param expression extracts the placeholders between the attribute names
     * @return array of placeholders
     */
    protected String[] extractAttributeSplittersMF(String expression) {
        Collection<String> splitters = new ArrayList<String>();
        int i = 0;
        String splitter;
        StringBuilder expr = new StringBuilder(expression);
        while (i != -1 && (splitter = StringUtil.substring(expr, "}", "{", i)).length() > 0) {
            splitters.add(splitter);
            if (i == 0)
                i = expr.indexOf("}");
            i = expr.indexOf("}", i + 1);
        }
        return splitters.toArray(new String[0]);
    }

    /**
     * splits the given value into values for all attributes in expression.
     * 
     * @param toValue value to split
     * @return attribute values
     */
    protected String[] getAttributeValues(String toValue) {
        if (attributeSplitters.length == 0)
            return new String[] { toValue };
        String splittedValues[] = new String[attributeSplitters.length + 1];
        String from = null;
        String to = null;
        int j = 0;
        for (int i = 0; i <= attributeSplitters.length; i++) {
            to = i < attributeSplitters.length ? attributeSplitters[i] : null;
            splittedValues[i] = StringUtil.substring(toValue, from, to, j);
            if (i == attributeSplitters.length)
                break;
            from = to;
            j = toValue.indexOf(to) + to.length();
        }
        return splittedValues;
    }

    public Class<TYPE> getType() {
        return type;
    }

    /**
     * @return Returns the expression.
     */
    @Override
    public String getExpression() {
        return expression;
    }

    /**
     * @param expression The expression to set.
     */
    @Override
    public void setExpression(String expression) {
        this.expression = expression;
        init(expression, type);
    }

    /**
     * isExpressionPart
     * 
     * @param attribute attribute to check
     * @return true, if given attribute is part of value expression
     */
    public boolean isExpressionPart(String attribute) {
        return Arrays.asList(attributes).contains(attribute);
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), expression);
    }

    @Override
    public String getExpressionPattern() {
        throw new UnsupportedOperationException();
    }
}
