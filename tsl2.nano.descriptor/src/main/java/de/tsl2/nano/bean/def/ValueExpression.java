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
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.GenericTypeMatcher;
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
public class ValueExpression<TYPE> implements
        IValueExpression<TYPE>,
        IConverter<TYPE, String>,
        IInputAssist<TYPE>,
        Serializable {
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
     * {birthday}, {date}, {medium}
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
    @Attribute(required=false)
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

    transient Comparator<TYPE> comparator;

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
        isMessageFormat = !expression.startsWith(PREFIX_PRINTF_FORMAT);
        if (isMessageFormat()) {
            //ok, thats not 'hinreichend'
            hasArguments = expression.contains("{") && expression.contains("}");
            attributes = extractAttributeNamesMF(expression);
            if (type != null) {
                attributeSplitters = extractAttributeSplittersMF(expression);
                this.type = type;
            }
        } else {
            //ok, thats not 'hinreichend'
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
    public String getName() {
        return expression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TYPE from(String toValue) {
        checkType();
        if (Util.isEmpty(toValue)) {
            return null;
        }
        //if type is object we try to get a type through string patterns
        if (type.isAssignableFrom(Object.class) || type.isAssignableFrom(Serializable.class)) {
            return (TYPE) ENV.get(GenericTypeMatcher.class).materialize(toValue);
        }

        boolean isPersistable = type != null && BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(type);
        TYPE exampleBean = createExampleBean(toValue, isPersistable);

        if (isPersistable) {//check for unique!
            Collection<TYPE> beansByExample = BeanContainer.instance().getBeansByExample(exampleBean);
            if (beansByExample.size() > 1) {
                LOG.warn("string-to-object-parser: found more than one object:\n"
                    + StringUtil.toFormattedString(beansByExample, 100, true) + "\n... --> using a simpel transient one!");
                //on creating search beans, multiple items should not be a problem
//                throw new ManagedException("tsl2nano.multiple.items", new Object[] { toValue, type, type });
                return exampleBean;
            } else if (beansByExample.size() == 0) {
                LOG.error("string-to-object-parser: found no object:\n"
                    + StringUtil.toFormattedString(BeanUtil.toValueMap(exampleBean), 100, true));
            }

            return beansByExample.size() > 0 ? beansByExample.iterator().next() : null;
        } else {
            return exampleBean;
        }
    }

    private void checkType() {
        if (type == null) {
            throw ManagedException
                .implementationError(
                    "The conversion from string to object is only available, if the ValueExpression was created with a class type argument!",
                    "type of value-expression '" + toString() + "' is null");
        }
        //ManagedException.assertion("instanceable", Util.isInstanceable(type), type);
    }

    public TYPE createExampleBean(String toValue) {
        return createExampleBean(toValue, false);
    }

    /**
     * createExampleBean
     * 
     * @param toValue
     * @param addSearchPostfix if true, an '*' will be added on attributes of type {@link CharSequence}.
     * @return example bean holding attributes given by toValue
     */
    public TYPE createExampleBean(String toValue, boolean addSearchPostfix) {
    	assureInit();
        TYPE exampleBean = createInstance(toValue);
        if (String.class.isAssignableFrom(getType())) // the following Bean.getBean() doesn't know, that the string is an instance and not a name!
        	return exampleBean;
        //TODO: how-to extract the attribute-name information from expression?
        Bean<TYPE> b = (Bean<TYPE>) Bean.getBean(exampleBean);
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
            	if (attributeValues.length > i && attributeValues[i] != null) {
	                b.setParsedValue(
	                    attributes[i],
	                    attributeValues[i]
	                        + (addSearchPostfix
	                            && (attr.getType() == null || CharSequence.class.isAssignableFrom(attr.getType())) ? "*"
	                            : ""));
            	}
            }
        }
        return b.instance;
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
        checkType();
        TYPE instance;
        if (type.isInterface()) {
            instance = BeanProxy.createBeanImplementation(type, null, null, Thread.currentThread().getContextClassLoader());
        } else if (BeanUtil.isStandardType(type)) {
            instance = PrimitiveUtil.create(type, toValue);
        } else if (BeanClass.hasStringConstructor(type)) {
            instance = BeanClass.createInstance(type, toValue);
        } else if (ByteUtil.isByteStream(type)) {
            instance = (TYPE) toValue.getBytes();
        } else {
            instance = BeanClass.createInstance(type);
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String to(TYPE fromValue) {
    	assureInit();
        if (fromValue == null) {
            return "";
        }
        if (hasArguments) {
            Map<String, Object> valueMap = BeanUtil.toValueMap(fromValue, false, false, true, attributes);
            if (valueMap.size() == 0) {
            	Bean bean = Bean.getBean(fromValue);
            	ManagedException.handleError("wrong value expression: %s not contained in %s :  with attributes: %s"
            			, attributes, bean.getName(), bean.getAttributeNames());
                return "?" + Util.asString(fromValue) + "?";
            }
            Object[] args = mapToAttributeOrder(valueMap);
            StringUtil.replaceNulls(args, false);
            //check for entity beans to resolve their format recursive through it's valueexpression
            preformatBeans(args);
            //if object 'fromValue' is empty or not filled, we fill questionmarks to the formatted text
            if (args.length < attributes.length) {
                Object[] arr = new Object[attributes.length];
                Arrays.fill(arr, "?");
                args = arr;
            }
            String txt = isMessageFormat() ? MessageFormat.format(format, args) : String.format(format, args);
            /*
             * workaround for new instances of composite beans, having a value expression of its id attribute.
             * this would result in an empty string on new instances.
             */
            return txt.isEmpty() ? "[new: " + fromValue + "]" : txt;
        } else if (fromValue instanceof Class) {
        	return ((Class) fromValue).getName();
        } else {
            //lazy workaround...
            return getWorkaroundFormat(fromValue);
        }
    }

    private String getWorkaroundFormat(TYPE fromValue) {
        return !Util.isEmpty(format) && !format.equals("Object") ? format : FormatUtil.getDefaultFormat(fromValue,
            false).format(fromValue);//Util.asString(fromValue);
    }

    private Object[] mapToAttributeOrder(Map<String, Object> valueMap) {
        Object[] mapped = new Object[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            mapped[i] = valueMap.get(attributes[i]);
        }
        return mapped;
    }

    protected void preformatBeans(Object[] args) {
        if (!BeanContainer.isInitialized()) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (BeanContainer.instance().isPersistable(args[i].getClass())) {
                    args[i] = Bean.getBean(args[i]).toString();
                } else if (args[i] instanceof Collection) {
                    Collection<?> c = (Collection<?>) args[i];
                    if (c.size() > 0 && BeanContainer.instance().isPersistable(c.iterator().next().getClass())) {
                        StringBuilder buf = new StringBuilder();
                        for (Object obj : c) {
                            buf.append(Bean.getBean(obj).toString() + ";");
                        }
                        args[i] = buf.toString();
                    }
                }
            }
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
            if (i == 0) {
                i = expr.indexOf("$");
            }
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
            if (i == 0) {
                i = expr.indexOf("}");
            }
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
        if (Util.isEmpty(attributeSplitters)) {
            return new String[] { toValue };
        }
        String splittedValues[] = new String[attributeSplitters.length + 1];
        String from = null;
        String to = null;
        int j = 0;
        for (int i = 0; i <= attributeSplitters.length; i++) {
            to = i < attributeSplitters.length ? attributeSplitters[i] : null;
            splittedValues[i] = StringUtil.substring(toValue, from, to, j);
            if (i == attributeSplitters.length || toValue.indexOf(to, j) == -1) {
                break;
            }
            from = to;
            j = toValue.indexOf(to) + to.length();
        }
        return splittedValues;
    }

    @Override
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
        return getAttributeNames().contains(attribute);
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), expression);
    }

    @Override
    public String getExpressionPattern() {
        return null;
    }

    @Override
    public Collection<TYPE> matchingObjects(Object prefix) {
        String input = Util.asString(prefix).trim();
        TYPE exampleBean = createExampleBean(input, true);
        Collection<TYPE> values =
            BeanContainer.instance().getBeansByExample(exampleBean, true, 0,
                ENV.get("websocket.intputassist.maxitemcount", 20));
        return values;
    }

    @Override
    public Collection<String> availableValues(Object prefix) {
        Collection<TYPE> values = matchingObjects(prefix);
        Collection<String> result = new ArrayList<String>(values.size());
        if (values.size() > 0) {
            for (TYPE t : values) {
                result.add(to(t));
            }
        }
        return result;
    }

    /**
     * getComparator
     * 
     * @return comparator through value expression strings
     */
    public Comparator<TYPE> getComparator() {
        if (comparator == null) {
            comparator = new Comparator<TYPE>() {
                @Override
                public int compare(TYPE o1, TYPE o2) {
                    return to(o1).compareTo(to(o2));
                }
            };
        }
        return comparator;
    }

	public List<String> getAttributeNames() {
		assureInit();
		return Arrays.asList(attributes);
	}

	private final void assureInit() {
		if (attributes == null)
			init(expression, type);
	}

    public void setType(Class<TYPE> type) {
        this.type = type;
    }
}
