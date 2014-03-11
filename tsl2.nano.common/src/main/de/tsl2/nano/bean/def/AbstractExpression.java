package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;

import de.tsl2.nano.core.cls.IAttribute;

@Default(value = DefaultType.FIELD, required = false)
public abstract class AbstractExpression<T> implements IValueExpression<T>, IAttribute<T>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 2798715915354958266L;

    /** declaringClass of first attribute in chain */
    @Attribute
    protected Class<?> declaringClass;
    /** attribute type */
    @Attribute(required=false)
    protected Class<T> type;
    @Element(data=true)
    protected String expression;

    /**
     * framework extensions can register own value-expression extensions. this extensions will be used on calling
     * {@link #setExpression(String)}. the extensions must have a constuctor with two arguments: Class declaringClass,
     * String expression.
     */
    @SuppressWarnings("rawtypes")
    protected static final Map<String, Class<? extends IValueExpression>> registeredExtensions =
        new HashMap<String, Class<? extends IValueExpression>>();

    static {
        //TODO: how to use IValueExpression.getExpressionPattern
        registerExpression(".*\\.*", PathExpression.class);
    }

    public AbstractExpression() {
        super();
    }

    /**
     * constructor
     * 
     * @param declaringClass
     * @param expression
     * @param type
     */
    protected AbstractExpression(Class<?> declaringClass, String expression, Class<T> type) {
        super();
        setExpression(expression);
        this.declaringClass = declaringClass;
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getType() {
        if (type == null)
            type = (Class<T>) BeanDefinition.UNDEFINED.getClass();
        return type;
    }

    /**
     * setType
     * 
     * @param type
     */
    public void setType(Class<T> type) {
        this.type = type;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public void setExpression(String valueExpression) {
        if (valueExpression == null || !valueExpression.matches(getExpressionPattern()))
            throw new IllegalArgumentException("The expression '" + valueExpression
                + "' has to match the regular expression '" + getExpressionPattern() + "'");
        this.expression = valueExpression;
    }

    /**
     * @return Returns the declaringClass.
     */
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }
    
    @Override
    public String toString() {
        return declaringClass.getSimpleName() + ":" + expression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(IAttribute<T> o) {
        return getName().compareTo(o.getName());
    }


    @Override
    public String getId() {
        return getDeclaringClass().getSimpleName() + ":" + getExpression();
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
    public boolean hasWriteAccess() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Method getAccessMethod() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVirtual() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void setExpression(String valueExpression) {
//        /*
//         * is it a special value like a value-path or a rule?
//         */
//        Class<? extends IValueExpression<T>> extensionClass =
//            (Class<? extends IValueExpression<T>>) getImplementation(valueExpression);
//        IValueExpression<T> extension;
//        if (extensionClass != null) {
//            extension = BeanClass.createInstance(extensionClass, valueExpression);
//        } else {
//            extension = new ValueExpression<T>(valueExpression, getType());
//        }
//        setValueExpression(extension);
//    }
//
    /**
     * see {@link #registeredExtensions}, {@link #getImplementation(String)} and {@link #getBeanValue(Object, String)}.
     * 
     * @param attributeRegEx regular expression, defining a specialized attribute
     * @param extension type to handle the specialized attribute.
     */
    public static final void registerExpression(String attributeRegEx, Class<? extends IValueExpression> extension) {
        registeredExtensions.put(attributeRegEx, extension);
    }

    /**
     * getExtension
     * 
     * @param attributeName attribute name to check
     * @return registered extension or null - if standard
     */
    protected static Class<? extends IValueExpression> getImplementation(String attributeName) {
        Set<String> regExs = registeredExtensions.keySet();
        for (String attrRegEx : regExs) {
            if (attributeName.matches(attrRegEx))
                return registeredExtensions.get(attrRegEx);
        }
        return null;
    }

}