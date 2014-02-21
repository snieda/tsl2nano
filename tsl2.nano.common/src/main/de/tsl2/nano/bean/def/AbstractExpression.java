package de.tsl2.nano.bean.def;

import java.io.Serializable;

import org.simpleframework.xml.Attribute;

public abstract class AbstractExpression<T> implements IValueExpression<T>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 2798715915354958266L;

    /** declaringClass of first attribute in chain */
    @Attribute
    protected Class<?> declaringClass;
    /** attribute type */
    @Attribute
    protected Class<T> type;
    @Attribute
    protected String expression;

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
    @Override
    public Class<T> getType() {
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
        if (!valueExpression.matches(getExpressionPattern()))
            throw new IllegalArgumentException("The expression '" + valueExpression
                + "' has to match the regular expression '" + getExpressionPattern() + "'");
        this.expression = valueExpression;
    }

    /**
     * @return Returns the declaringClass.
     */
    Class<?> getDeclaringClass() {
        return declaringClass;
    }
    
    @Override
    public String toString() {
        return declaringClass.getSimpleName() + ":" + expression;
    }
}