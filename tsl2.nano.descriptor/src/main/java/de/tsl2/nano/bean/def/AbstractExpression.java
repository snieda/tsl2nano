package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Base Attribute Expression as descriptor of a real (extending) expression. can instantiate a
 * real expression through registered expression patterns.
 * 
 * @param <T>
 * @author Tom
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public abstract class AbstractExpression<T> implements IValueExpression<T>, IAttribute<T>, Serializable {
    private static final long serialVersionUID = 2798715915354958266L;

    private static final Log LOG = LogFactory.getLog(AbstractExpression.class);
    
    /** declaringClass of first attribute in chain */
    @Attribute(required = false)
    protected Class<?> declaringClass;
    /** attribute type */
    @Attribute(required = false)
    protected Class<T> type;
    @Element(data = true)
    protected String expression;

    /**
     * should be the same as used in beans attribute definitions. if undefined, the last part of the expression will be
     * used.
     */
    //@Attribute
    protected transient String name;

    /**
     * framework extensions can register own value-expression extensions. this extensions will be used on calling
     * {@link #setExpression(String)}. the extensions must have a constuctor with two arguments: Class declaringClass,
     * String expression.
     */
    @SuppressWarnings("rawtypes")
    protected static final Map<String, Class<? extends AbstractExpression>> registeredExtensions =
        new HashMap<String, Class<? extends AbstractExpression>>();

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
        if (type == null) {
            type = (Class<T>) BeanDefinition.UNDEFINED.getClass();
        }
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
        if (valueExpression == null || (getExpressionPattern() != null && !valueExpression.matches(getExpressionPattern()))) {
            throw new IllegalArgumentException("The expression '" + valueExpression
                + "' has to match the regular expression '" + getExpressionPattern() + "'");
        }
        //if name was calculated, reset it
        if (this.name != null && this.expression != null && this.expression.contains(name))
            this.name = null;
        this.expression = valueExpression;
    }

    /**
     * @return Returns the declaringClass.
     */
    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(Class<?> declaringClass) {
        this.declaringClass = declaringClass;
    }
    
    @Override
    public String toString() {
        return (declaringClass != null ? declaringClass.getSimpleName() + ": " : "") + expression;
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
        return (getDeclaringClass() != null ? getDeclaringClass().getSimpleName().toLowerCase() : "[unknown]") + "." + getExpression();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        if (name == null) {
            name = expression;
        }
        return expression;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    
    public static String createRegExpOnAllRegistered() {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        Collection<String> regExps = registeredExtensions.keySet();
        for (String e : regExps) {
            buf.append("(" + e + ")" + "|");
        }
        buf.deleteCharAt(buf.length()-1);
        buf.append(")");
        return buf.toString();
    }
    
    /**
     * see {@link #registeredExtensions}, {@link #getImplementation(String)} and {@link #getBeanValue(Object, String)}.
     * 
     * @param attributeRegEx regular expression, defining a specialized attribute
     * @param extension type to handle the specialized attribute.
     */
    public static final void registerExpression(Class<? extends AbstractExpression> extension) {
        try {
            String pattern = extension.newInstance().getExpressionPattern();
            LOG.info("registering expression class " + extension + " for pattern: " + pattern);
            registeredExtensions.put(pattern, extension);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * getExtension
     * 
     * @param expression attribute expression to check
     * @return registered extension or null - if standard
     */
    protected static Class<? extends AbstractExpression> getImplementation(String expression) {
        Set<String> regExs = registeredExtensions.keySet();
        for (String attrRegEx : regExs) {
            if (expression.matches(attrRegEx)) {
                return registeredExtensions.get(attrRegEx);
            }
        }
        return null;
    }

}