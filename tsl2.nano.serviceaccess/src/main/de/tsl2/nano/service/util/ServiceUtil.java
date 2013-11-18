/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Apr 13, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.StringUtil;

/**
 * some utility functions to be used by services.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ServiceUtil {
    static final String OP_EQ = " = ";
    static final String OP_LT = " < ";
    static final String OP_GT = " > ";
    static final String OP_LE = " <= ";
    static final String OP_GE = " >= ";
    public static final String OP_LIKE = " like ";

    public static final String CLAUSE_SELECT = "select  ";
    public static final String CLAUSE_WHERE = " where ";
    public static final String CLAUSE_AND = " and ";
    public static final String CLAUSE_OR = " or ";
    public static final String CLAUSE_NOT = " and not ";

    static final String BRACKET_OPEN = " ( ";
    static final String BRACKET_CLOSE = " ) ";
    /** sql name of first sql result substitute (see {@link #createStatement(Class)} */
    public static final String SUBST_RESULTBEAN = "t";
    static final String VALUE_PH = "?";

    protected static final long MINTIME = -62167395600000l /* 01.01.0000 */;
    protected static final long MAXTIME = 32535126000000l /* 31.12.3000 */;
    protected static final String STR_MIN_VALUE = "MIN_VALUE";
    protected static final String STR_MAX_VALUE = "MAX_VALUE";

    private static final Log LOG = LogFactory.getLog(ServiceUtil.class);

    /**
     * @param beanType
     * @return base select query
     */
    public static StringBuffer createStatement(Class<?> beanType) {
        return createStatement(beanType, SUBST_RESULTBEAN);
    }

    /**
     * @param beanType
     * @return base select query
     */
    public static StringBuffer createStatement(Class<?> beanType, String substName) {
        substName = substName.endsWith(".") ? substName.substring(0, substName.length() - 1) : substName;
        return new StringBuffer("select " + substName
            + " from "
            + BeanClass.getName(beanType)
            + " "
            + substName
            + " \n");
    }

    /** creates an jpa-ql to find all beans with same attributes (only single value attributes!) as exampleBean */
    public static Collection<?> createExampleStatement(StringBuffer qStr,
            Object exampleBean,
            boolean useLike,
            boolean caseInsensitive) {
        if (qStr.length() == 0)
            qStr.append(createStatement(exampleBean.getClass()));
        final Collection parameter = new LinkedList();
        qStr = addAndConditions(qStr, exampleBean, useLike ? OP_LIKE : OP_EQ, parameter, caseInsensitive);
        LOG.debug(getLogInfo(qStr, parameter));
        return parameter;
    }

    /**
     * creates a jpa-ql statement to find all beans, having properties between firstBean and secondBean.
     * 
     * @param <T> beantype
     * @param qStr string buffer to fill with new statement. if qStr is empty the select statement result will hold all
     *            fields of the bean. if not, it has to be filled until the where clause.
     * @param firstBean minimum bean
     * @param secondBean maximum bean
     * @param caseInsensitive whether to search strings case insensitive
     * @return parameter list of created statement
     */
    public static <T> Collection<?> createBetweenStatement(StringBuffer qStr,
            T firstBean,
            T secondBean,
            boolean caseInsensitive) {
        prepareStringValuesForBetween(firstBean, secondBean);
        if (qStr.length() == 0)
            qStr.append(createStatement(firstBean.getClass()));
        final Collection parameter = new LinkedList();
        addBetweenConditions(qStr, firstBean, secondBean, parameter, caseInsensitive);
        LOG.debug(getLogInfo(qStr, parameter));
        return parameter;
    }

    /**
     * @deprecated use {@link #addAndConditions(StringBuffer, Object, String, Collection)} instead
     *             <p>
     *             creates an ejb-ql query using the valueBean and its attribute-names to add 'and' conditions to the
     *             where-clause. it is intelligent to find the right syntax for different attribute types. if an
     *             attribute value holds an '*', the operator {@linkplain #OP_LIKE} will be used instead of the given
     *             operator.
     * 
     * @param valueBean value holding bean
     * @param operator operator to be used on and conditions (see {@link #OP_EQ} etc.)
     * @return oql query statement
     */
    @Deprecated
    public static StringBuffer _addAndConditions(StringBuffer qStr, Object valueBean, String operator) {
        String and_cond = CLAUSE_AND;
        if (!qStr.toString().contains(CLAUSE_WHERE)) {
            and_cond = CLAUSE_WHERE;
        }
        final Class<?> clazz = valueBean.getClass();
        final BeanClass bclazz = new BeanClass(clazz);
        final Collection<BeanAttribute> attributes = bclazz.getSingleValueAttributes();
        final String prefix = "'";
        final String postfix = OP_LIKE.equals(operator) ? "%'" : "'";
        Object value;
        for (final BeanAttribute beanAttribute : attributes) {
            final IAttributeDef attributeDef = BeanContainerUtil.getAttributeDefinitions(beanAttribute);
            if (attributeDef != null) {//only attributes with column-defs will be used!
                String name = beanAttribute.getName();//getColumnName(beanAttribute);
                value = beanAttribute.getValue(valueBean);
                if (value == null) {
                    continue;
                }
                //on manyToOne mappings use the foreignkey column
                boolean isManyToOne = false;
                if (!BeanUtil.isStandardType(value.getClass())) {
                    final Collection<BeanAttribute> foreignIdAttributes = new BeanClass(value.getClass()).findAttributes(Id.class);
                    if (foreignIdAttributes.size() > 0) {
                        final BeanAttribute foreignKey = foreignIdAttributes.iterator().next();
                        value = foreignKey.getValue(value);
                        name = name + "." + foreignKey.getName();
                        isManyToOne = true;
                    }
                }
                if (!isManyToOne && value instanceof String) {
                    String strValue = String.valueOf(value).toLowerCase();
                    final boolean isLikeValue = strValue.endsWith("*");
                    if (isLikeValue) {
                        strValue = strValue.replace('*', '%');
                    } else {
                        if (operator.equals(OP_GE) || operator.equals(OP_GT)) {
                            strValue = StringUtil.fixString(strValue, attributeDef.length(), '0', true);
                        } else if (operator.equals(OP_LE) || operator.equals(OP_LT)) {
                            strValue = StringUtil.fixString(strValue, attributeDef.length(), 'z', true);
                        }
                    }
                    qStr.append(and_cond + "LOWER(t."
                        + name
                        + ") "
                        + (isLikeValue ? OP_LIKE : operator)
                        + prefix
                        + strValue
                        + postfix);
                } else if (isManyToOne && value instanceof String) {
                    final String strValue = String.valueOf(value);
                    final boolean isString = (value instanceof String) || (value instanceof Date);
                    qStr.append(and_cond + "t."
                        + name
                        + OP_EQ
                        + (isString ? prefix : "")
                        + strValue
                        + (isString ? prefix : ""));
                } else {// if no string, like is not possible
                    final String op = OP_LIKE.equals(operator) ? OP_EQ : operator;
                    if (value instanceof Date) {
                        final java.sql.Date sqlDate = (java.sql.Date) (value instanceof java.sql.Date ? value
                            : new java.sql.Date(((Date) value).getTime()));
                        qStr.append(and_cond + "t." + name + op + " '" + sqlDate + "' ");
                    } else {
                        qStr.append(and_cond + "t." + name + " " + op + " " + String.valueOf(value));
                    }
                }
                and_cond = " and ";
            }
        }
        return qStr;
    }

    public static StringBuffer addAndConditions(StringBuffer qStr,
            Object valueBean,
            String operator,
            Collection parameter,
            boolean caseInsensitive) {
        return addAndConditions(qStr, null, null, valueBean, operator, parameter, caseInsensitive);
    }

    public static StringBuffer addAndConditions(StringBuffer qStr,
            String attrPrefix,
            Object valueBean,
            String operator,
            Collection parameter,
            boolean caseInsensitive) {
        return addAndConditions(qStr, attrPrefix, null, valueBean, operator, parameter, caseInsensitive);
    }

    /**
     * creates an ejb-ql query using the valueBean and its attribute-names to add 'and' conditions to the where-clause.
     * it is intelligent to find the right syntax for different attribute types. if an attribute value holds an '*', the
     * operator {@linkplain #OP_LIKE} will be used instead of the given operator.
     * 
     * @param qStr query to fill
     * @param attrPrefix (optional) needed on recursive entity attribute calls
     * @param valueBean value holding bean
     * @param operator operator to be used on and conditions (see {@link #OP_EQ} etc.)
     * @param parameter statement parameter list to be filled by this call
     * @param caseInsensitive if true, strings will be search case insensitive --> performance will be lower!
     * @return oql query statement
     */
    public static StringBuffer addAndConditions(StringBuffer qStr,
            String attrPrefix,
            String and_cond,
            Object valueBean,
            String operator,
            Collection parameter,
            boolean caseInsensitive) {
        String bracket_close = "";
        if (and_cond == null)
            and_cond = CLAUSE_AND;
        if (!qStr.toString().contains(CLAUSE_WHERE)) {
            and_cond = CLAUSE_WHERE;
        }
        attrPrefix = attrPrefix == null ? SUBST_RESULTBEAN + "." : attrPrefix;
        final Class<?> clazz = valueBean.getClass();
        final BeanClass bclazz = new BeanClass(clazz);
        final Collection<BeanAttribute> attributes = bclazz.getSingleValueAttributes();
        Object value;
        for (final BeanAttribute beanAttribute : attributes) {
            final IAttributeDef attributeDef = BeanContainerUtil.getAttributeDefinitions(beanAttribute);
            if (attributeDef != null) {//only attributes with column-defs will be used!
                final String name = beanAttribute.getName();//getColumnName(beanAttribute);
                final String varName = caseInsensitive ? "LOWER(" + attrPrefix + name + ") " : attrPrefix + name + " ";
                value = beanAttribute.getValue(valueBean);
                if (value == null) {
                    continue;
                }
                //recursive entity-attributes on transient objects
                if (BeanContainerUtil.isPersistable(beanAttribute.getType()) && getId(value) == null) {
                    addAndConditions(qStr, attrPrefix + name + ".", value, operator, parameter, caseInsensitive);
                    qStr.append("\n");
                    and_cond = CLAUSE_AND;
                    continue;
                }
                //on manyToOne mappings use the foreignkey column
                final boolean isManyToOne = false;
//                if (!BeanUtil.isStandardType(value.getClass())) {
//                    Collection<BeanAttribute> foreignIdAttributes = new BeanClass(value.getClass()).findAttributes(Id.class);
//                    if (foreignIdAttributes.size() > 0) {
//                        BeanAttribute foreignKey = foreignIdAttributes.iterator().next();
//                        value = foreignKey.getValue(value);
//                        name = name + "." + foreignKey.getName();
//                        isManyToOne = true;
//                    }
//                }
                if (!isManyToOne && value instanceof String) {
                    String strValue = caseInsensitive ? String.valueOf(value).toLowerCase() : String.valueOf(value);
                    final boolean isLikeValue = strValue.endsWith("*");
                    if (isLikeValue) {
                        strValue = strValue.replace('*', '%');
                    } else {
                        if (operator.equals(OP_GE) || operator.equals(OP_GT)) {
                            strValue = StringUtil.fixString(strValue, attributeDef.length(), '0', true);
                            /*
                             * the between mechanism (e.g.: between abc000 and abczzz) doesn't respect the full match!
                             * so we have to add an and-condition (with brackets!) to the full match with EQUAL.
                             */
                            qStr.append(and_cond + BRACKET_OPEN + varName + OP_EQ + VALUE_PH);
                            and_cond = CLAUSE_OR;
                            parameter.add(caseInsensitive ? ((String) value).toLowerCase() : (String) value);
                        } else if (operator.equals(OP_LE) || operator.equals(OP_LT)) {
                            strValue = StringUtil.fixString(strValue, attributeDef.length(), 'z', true);
                            bracket_close = BRACKET_CLOSE;
                        }
                    }
                    qStr.append(and_cond + varName + (isLikeValue ? OP_LIKE : operator) + VALUE_PH + bracket_close);
                    value = strValue;
                } else if (isManyToOne && value instanceof String) {
                    qStr.append(and_cond + attrPrefix + name + OP_EQ + VALUE_PH);
                } else {// if no string, like is not possible
                    final String op = OP_LIKE.equals(operator) ? OP_EQ : operator;
                    qStr.append(and_cond + attrPrefix + name + op + VALUE_PH);
                }
                parameter.add(value);
                and_cond = CLAUSE_AND;
            }
        }
        return qStr;
    }

    /**
     * delegates to {@link #addBetweenConditions(StringBuffer, String, Object, Object, Collection, boolean)}.
     */
    public static StringBuffer addBetweenConditions(StringBuffer qStr,
            Object fromBean,
            Object toBean,
            Collection parameter,
            boolean caseInsensitive) {
        return addBetweenConditions(qStr, null, fromBean, toBean, parameter, caseInsensitive);
    }

    public static StringBuffer addBetweenConditions(StringBuffer qStr,
            String attrPrefix,
            Object fromBean,
            Object toBean,
            Collection parameter,
            boolean caseInsensitive) {
        return addBetweenConditions(qStr, attrPrefix, null, fromBean, toBean, parameter, caseInsensitive);
    }

    /**
     * creates an ejb-ql query using the valueBean and its attribute-names to add 'and' conditions to the where-clause.
     * it is intelligent to find the right syntax for different attribute types. if an attribute value holds an '*', the
     * operator {@linkplain #OP_LIKE} will be used instead of the given operator.
     * 
     * @param qStr select query to fill
     * @param attrPrefix (optional) needed on recursive calls to define the full attribute path
     * @param fromBean value holding bean
     * @param toBean value holding bean
     * @param parameter statement parameter list to be filled by this call
     * @return oql query statement
     */
    public static StringBuffer addBetweenConditions(StringBuffer qStr,
            String attrPrefix,
            String and_cond,
            Object fromBean,
            Object toBean,
            Collection parameter,
            boolean caseInsensitive) {
        String bracket_open = "", bracket_close = "";
        if (and_cond == null)
            and_cond = CLAUSE_AND;
        if (!qStr.toString().contains(CLAUSE_WHERE)) {
            and_cond = CLAUSE_WHERE;
        }
        attrPrefix = attrPrefix == null ? SUBST_RESULTBEAN + "." : attrPrefix;
        final Class<?> clazz = fromBean.getClass();
        final BeanClass bclazz = new BeanClass(clazz);
        final Collection<BeanAttribute> attributes = bclazz.getSingleValueAttributes();
        Object fromValue, toValue;
        String strValue;
        for (final BeanAttribute beanAttribute : attributes) {
            final IAttributeDef attributeDef = BeanContainerUtil.getAttributeDefinitions(beanAttribute);
            if (attributeDef != null) {//only attributes with column-defs will be used!
                final String name = beanAttribute.getName();//getColumnName(beanAttribute);
                final String varName = caseInsensitive ? "LOWER(" + attrPrefix + name + ") " : attrPrefix + name + " ";
                fromValue = beanAttribute.getValue(fromBean);
                toValue = beanAttribute.getValue(toBean);
                if (fromValue == null && toValue == null) {
                    continue;
                }
                //recursive entity-attributes on transient objects
                if (BeanContainerUtil.isPersistable(beanAttribute.getType()) && ((fromValue != null && getId(fromValue) == null) || (toValue != null && getId(toValue) == null))) {
                    addBetweenConditions(qStr, attrPrefix + name + ".", fromValue, toValue, parameter, caseInsensitive);
                    qStr.append("\n");
                    and_cond = CLAUSE_AND;
                    continue;
                }
                /*
                 * on different from, to values, we create a between
                 * strings will be different on using the filling mechanism (e.g.: sch0000 to schzzzz)
                 */
                if (fromValue == null || !fromValue.equals(toValue) || fromValue instanceof String) {
                    bracket_open = BRACKET_OPEN;
                    /*
                     * only for strings:
                     * the between mechanism (e.g.: between abc000 and abczzz) doesn't respect the full match!
                     * so we have to add an and-condition (with brackets!) to the full match with EQUAL.
                     */
                    if (fromValue instanceof String) {
                        strValue = fromValue != null ? (caseInsensitive ? fromValue.toString().toLowerCase()
                            : fromValue.toString()) : "";
                        strValue = StringUtil.fixString(strValue, attributeDef.length(), '0', true);
                        qStr.append(and_cond + bracket_open + varName + OP_EQ + VALUE_PH);
                        and_cond = CLAUSE_OR;
                        bracket_open = "";
                        parameter.add(caseInsensitive ? ((String) fromValue).toLowerCase() : (String) fromValue);
                        fromValue = strValue;
                    }
                    /*
                     * now, the standard between: start with greater equal
                     */
                    if (fromValue != null) {
                        qStr.append(and_cond + bracket_open + varName + OP_GE + VALUE_PH + bracket_close);
                        parameter.add(fromValue);
                        and_cond = CLAUSE_AND;
                        bracket_close = BRACKET_CLOSE;
                    } else {
                        bracket_close = "";
                    }
                    /*
                     * the end: lower equal
                     */
                    if (toValue instanceof String) {
                        strValue = toValue != null ? (caseInsensitive ? toValue.toString().toLowerCase()
                            : toValue.toString()) : "";
                        strValue = StringUtil.fixString(strValue, attributeDef.length(), 'z', true);
                        toValue = strValue;
                    }
                    if (toValue != null) {
                        qStr.append(and_cond + varName + OP_LE + VALUE_PH + bracket_close);
                        parameter.add(toValue);
                    } else {
                        qStr.append(bracket_close);
                    }
                } else {// no range: we create a simple equals
                    if (toValue != null) {
                        qStr.append(and_cond + attrPrefix + name + OP_EQ + VALUE_PH);
                        parameter.add(toValue);
                    }
                }
                and_cond = CLAUSE_AND;
                bracket_close = "";
            }
        }
        return qStr;
    }

    /**
     * delegates to {@link #addMemberExpression(StringBuffer, String, Object, Class, String)} using
     * {@link #SUBST_RESULTBEAN}.
     */
    public static <H, T> StringBuffer addMemberExpression(StringBuffer qStr,
            H holder,
            Class<T> beanType,
            String attributeName) {
        return addMemberExpression(qStr, SUBST_RESULTBEAN, 1, holder, beanType, attributeName);
    }

    /**
     * find all beans of type beanType beeing members of holder. useful if your beanType has no access to the holder.
     * <p>
     * 
     * <pre>
     * f.e.: 
     *   Parent (1) <-- (*) Child
     *   ==> but you want to get the parents children!
     * will result in:
     *   select t from Child t, Parent t1 
     *   where t1.ID = holder.ID 
     *   and t member of t1.{attributeName}
     * </pre>
     * 
     * @param <H> holder type
     * @param <T> member type
     * @param beanType member type to be collected
     * @param holder holder instance to get the members of (without direct access!)
     * @param attributeName
     * @return members of holder (member given by attributeName)
     */
    public static <H, T> StringBuffer addMemberExpression(StringBuffer qStr,
            String substName,
            int index,
            H holder,
            Class<T> beanType,
            String attributeName) {
        //the select must be prepared up to the first 'from'-clause-entry. mostly the string will end with a newline --> we delete that.
        qStr.deleteCharAt(qStr.length() - 1);
        final String idAttribute = getIdName(holder);
        String tm = "tm" + index;
        qStr.append(", " + holder.getClass().getSimpleName()
            + " "
            + tm
            + "\n where ("
            + tm
            + "."
            + idAttribute
            + " = ? and "
            + substName
            + " member of "
            + tm
            + "."
            + attributeName
            + ")");
        return qStr;
    }

    /**
     * delegates to {@link #addHolderExpression(StringBuffer, String, Object, Class, String)} using
     * {@link #SUBST_RESULTBEAN}.
     */
    public static <T, H> StringBuffer addHolderExpression(StringBuffer qStr,
            T member,
            Class<H> holderType,
            String attributeName) {
        return addHolderExpression(qStr, SUBST_RESULTBEAN, 1, member, holderType, attributeName);
    }

    /**
     * find all holders of the given member instance. useful if your member has no access to the holder. on composites
     * and aggregations you will get a collection holding only one instance.
     * <p>
     * 
     * <pre>
     * f.e.: 
     *   Parent (1) --> (*) Child
     *   ==> but you want to get a childs parent!
     * will result in:
     *   select t from Child t, Parent t1 
     *   where t.ID = member.ID 
     *   and t member of t1.{attributeName}
     * </pre>
     * 
     * @param <H> holder type
     * @param <T> member type
     * @param beanType member type to be collected
     * @param holder holder instance to get the members of (without direct access!)
     * @param attributeName
     * @return members of holder (member given by attributeName)
     */
    public static <T, H> StringBuffer addHolderExpression(StringBuffer qStr,
            String substName,
            int index,
            T member,
            Class<H> holderType,
            String attributeName) {
        //the select must be prepared up to the first 'from'-clause-entry. mostly the string will end with a newline --> we delete that.
        qStr.deleteCharAt(qStr.length() - 1);
        final String idAttribute = getIdName(member);
        String th = "th" + index;
        qStr.append(", " + member.getClass().getSimpleName()
            + " "
            + th
            + "\n where ("
            + th
            + "."
            + idAttribute
            + " = ? and "
            + th
            + " member of "
            + substName
            + "."
            + attributeName
            + ")");
        return qStr;
    }

    /**
     * addInSelection
     * 
     * @param qStr current select
     * @param substName table subst name
     * @param attribute attribute name to have value in selection
     * @param selection value selection for attribute
     * @return extended select
     */
    public static StringBuffer addInSelection(StringBuffer qStr,
            String clause,
            String substName,
            String attribute,
            Collection<?> selection) {
        return qStr.append(" " + clause + " " + substName + "." + attribute + " in (?)");
    }

    /**
     * tries to find the method with id-annotation. if not existing, return null. it is a generic method with poor
     * performance.
     * 
     * @param bean bean instance, holding an id.
     * @return id of bean or null.
     */
    public static Object getId(Object bean) {
        LOG.debug("evaluation bean-id for :" + bean);
        final Method m = getIdMethod(bean);
        if (m != null) {
            try {
                LOG.debug("invoking bean-id on : " + m);
                return m.invoke(bean, new Object[0]);
            } catch (final Exception e) {
                ForwardedException.forward(e);
            }
        }
        //on a field?
        final Field f = getIdField(bean);
        if (f != null) {
            try {
                LOG.debug("invoking bean-id on : " + f);
                if (f.isAccessible()) {
                    return f.get(bean);
                } else {
                    final BeanAttribute readAccess = BeanAttribute.getBeanAttribute(bean.getClass(), f.getName());
                    return readAccess.getValue(bean);
                }
            } catch (final Exception e) {
                LOG.error("The @Id field '" + f.getName() + " ' is not accessible!!!");
                ForwardedException.forward(e);
            }
        }
        return null;
    }

    public static Method getIdMethod(Object bean) {
        final Method[] methods = bean.getClass().getMethods();
        for (final Method m : methods) {
            if (m.isAnnotationPresent(javax.persistence.Id.class)) {
                LOG.debug("invoking bean-id on : " + m);
                return m;
            }
        }
        return null;
    }

    public static Field getIdField(Object bean) {
        final Field[] fields = bean.getClass().getDeclaredFields();
        for (final Field f : fields) {
            if (f.isAnnotationPresent(javax.persistence.Id.class)) {
                return f;
            }
        }
        /*
         * if a super class defines the id field, we will find it now - or null
         */
        Class superClass = bean.getClass().getSuperclass();
        return superClass != null ? getSuperIdField(superClass) : null;
    }

    public static Field getSuperIdField(Class clazz) {
        final Field[] fields = clazz.getDeclaredFields();
        for (final Field f : fields) {
            if (f.isAnnotationPresent(javax.persistence.Id.class)) {
                return f;
            }
        }
        Class superClass = clazz.getSuperclass();
        return superClass != null ? getSuperIdField(superClass) : null;
    }

    public static String getIdName(Object bean) {
        final BeanClass bc = new BeanClass(bean.getClass());
        final Collection<BeanAttribute> attributes = bc.findAttributes(Id.class);
        if (attributes.size() > 0) {
            return attributes.iterator().next().getName();
        } else {
            return null;
        }
    }

    /**
     * reads ejb annotations like {@link Column} and {@link JoinColumn}.
     * 
     * @param attr attribute
     * @return name of column
     */
    public static String getColumnName(BeanAttribute attr) {
        final Column c = attr.getAnnotation(Column.class);
        if (c != null) {
            return c.name();
        }
        final JoinColumn jc = attr.getAnnotation(JoinColumn.class);
        if (jc != null) {
            return jc.name();
        }
        return attr.getName();
    }

    /**
     * if the first bean has non null value of type string, the second value must have the same value. will later be
     * used to create a min/max range.
     * 
     * <pre>
     * e.g.: firstBean.myStrValue = 'Sch' and secondBean.myStrValue = null
     *       ==> secondBean.myStrValue = 'Sch'
     *       ==> query: ...between 'sch0000000' and 'schzzzzzzz'
     * </pre>
     * 
     * @param <T> bean type
     * @param firstBean min range
     * @param secondBean max range
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> void prepareStringValuesForBetween(T firstBean, T secondBean) {
        final Class<?> clazz = firstBean.getClass();
        final BeanClass bclazz = new BeanClass(clazz);
        final Collection<BeanAttribute> attributes = bclazz.getSingleValueAttributes();
        for (final BeanAttribute beanAttribute : attributes) {
            if (String.class.isAssignableFrom(beanAttribute.getType())) {
                final String v1 = (String) beanAttribute.getValue(firstBean);
                if (v1 != null) {
                    final String v2 = (String) beanAttribute.getValue(secondBean);
                    if (v2 == null) {
                        beanAttribute.setValue(secondBean, v1);
                    }
                }
            }
        }
    }

    /**
     * fills all attributes having no value to a define min or max value.
     * 
     * @param bean bean to change
     * @param maxValues if true, maximum values will be filled
     * @param useDatabaseFormat if true, the date will have a database format, the min numbers will be 0 and strings
     *            will be surrounded by "'".
     */
    public static void fillNullValues(Object bean, boolean maxValues, boolean useDatabaseFormat) {
        final Collection<BeanAttribute> singleValueAttributes = new BeanClass(bean.getClass()).getSingleValueAttributes();
        for (final BeanAttribute beanAttribute : singleValueAttributes) {
            if (beanAttribute.getValue(bean) != null) {
                continue;
            }
            if (BigDecimal.class.isAssignableFrom(beanAttribute.getType())) {
                if (maxValues) {
                    beanAttribute.setValue(bean, new BigDecimal(Integer.MAX_VALUE));
                } else {
                    beanAttribute.setValue(bean, new BigDecimal(Integer.MIN_VALUE));
                }
            } else if (Number.class.isAssignableFrom(beanAttribute.getType())) {
                String fieldName;
                if (maxValues) {
                    fieldName = STR_MAX_VALUE;
                } else {
                    fieldName = STR_MIN_VALUE;
                }
                Object newValue;
                try {
                    /*if (useDatabaseFormat && !maxValues) {
                        newValue = 0;
                    } else*/{
                        newValue = beanAttribute.getType().getField(fieldName).get(null);
                    }
                } catch (final Exception e) {
                    ForwardedException.forward(e);
                    return;
                }
                beanAttribute.setValue(bean, newValue);
            } else {//String or Date
                if (Date.class.isAssignableFrom(beanAttribute.getType())) {
                    if (useDatabaseFormat) {
                        //TODO: static dates use ORACLE specific format. use SQL-92
                        final Date dbDateValue = new Date((maxValues ? MAXTIME : MINTIME)) {
                            private static final long serialVersionUID = 1L;
                            private final String pattern = "yyyy-MM-dd";
                            private final/*static final */DateFormat sdf = new SimpleDateFormat(pattern);

                            @Override
                            public String toString() {
//                                return "to_date('" + sdf.format(this) + "','" + pattern + "')";
                                return "'" + sdf.format(this) + "'";
                            }
                        };
                        beanAttribute.setValue(bean, dbDateValue);
                    } else {
                        beanAttribute.setValue(bean, maxValues ? new Date(MAXTIME) : new Date(MINTIME));
                    }
                } else if (String.class.isAssignableFrom(beanAttribute.getType())) {
                    final String prefix = "";//useDatabaseFormat ? "'" : "";
                    final String postfix = "";//useDatabaseFormat ? "'" : "";
                    //TODO: evaluate length through JPA annotation
                    beanAttribute.setValue(bean, maxValues ? prefix + ""/*StringUtil.fixString("", 25, 'z', true)*/
                        + postfix : prefix + ""/*StringUtil.fixString("", 25, '0', true)*/+ postfix);
                }
            }
        }
    }

    public static final Object getLogInfo(StringBuffer qStr, Collection parameter) {
        return "\n" + qStr + "\n    parameter: " + parameter;
    }

    /**
     * WORKAROUND: TopLink is not able to insert a list of values to a query
     * 
     * @param qstr ejb ql string ending with 'IN' , but without starting bracket.
     * @param parameter parameter to insert
     * @return query
     */
    public static Query setCollectionParameter(EntityManager entityManager, String qstr, Collection<?> parameter) {
        assert parameter != null && parameter.size() > 0 : "parameters must contain at least one item";
        final String colName = "colpar";
        final String colPar = ":" + colName;
        final StringBuffer qstrBuf = new StringBuffer(qstr + " (");
        int i = 0;
        for (final Object par : parameter) {
            qstrBuf.append((i > 0 ? "," : "") + colPar + ++i);
        }
        qstr = qstrBuf.toString() + ")";
        Query query = entityManager.createQuery(qstr);
        i = 0;
        for (final Object par : parameter) {
            query = query.setParameter(colName + ++i, par);
        }
        return query;
    }

    /**
     * Workaround helper for statements with 'in(...)'. The jpa-implementor may not be able to replace placeholder with
     * a collection of values.
     * <p/>
     * Use-Case: select .....and beanvar in (?x) <-- Collection with parameter --> select .....and beanvar in ('mypar1',
     * 'mypar2', ...)
     * 
     * @param qstr origin query
     * @param matchExpression parameter name or '?' followed by a number (must exist in qstr)
     * @param parameter values to be inserted as comma-separated strings
     * @return qstr with filled values of parameter
     */
    public static final String getCollectionParameter(String qstr, String matchExpression, Collection<?> parameter) {
        StringBuilder qstrb = new StringBuilder(qstr);
        StringUtil.replace(qstrb,
            matchExpression,
            "'" + StringUtil.concat(new char[] { '\'', ',', '\'' }, parameter.toArray(new String[0])) + "'");
        return qstrb.toString();
    }

    /**
     * checks whether to use {@link #setNamedParameters(Query, Object...)} or {@link #setParameters(Query, Object...)}.
     * 
     * @param query query string to check
     * @return true, if query contains standardized name parameters like :par1.
     */
    public static final boolean useNamedParameters(String query) {
        return query.contains(":par1");
    }

    /**
     * assigns the given args to the given query - using parameters without names (placeholder: ? or ?digit). see
     * {@link #setNamedParameters(Query, Object...)}.
     * 
     * @param query query to set the parameters for
     * @param args parameters
     * @return query holding all given parameters
     */
    public static Query setParameters(Query query, Object... args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                //parameters are 1-based!
                query = query.setParameter(i + 1, args[i]);
            }
        }
        return query;
    }

    /**
     * assigns the given args to the given query - using parameters with standardized names (par1, par2 etc.).
     * 
     * @param query query to set the parameters for
     * @param args parameters
     * @return query holding all given parameters
     */
    public static Query setNamedParameters(Query query, Object... args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                query = query.setParameter("par" + (i + 1), args[i]);
            }
        }
        return query;
    }

    /**
     * setHints
     * @param query to set hints for
     * @param hints (optional) hints to set
     * @return changed query
     */
    public static Query setHints(Query query, Map<String, ?> hints) {
        if (hints != null) {
            Set<String> keySet = hints.keySet();
            for (String hint : keySet) {
                query = query.setHint(hint, hints.get(hint));
            }
        }
        return query;
    }

    /**
     * addMaxRowCountToQuery
     * 
     * @param originQuery query where to add the maxresult statement
     * @param maxresult maximum row count of query result
     * @return
     */
    public static String addMaxRowCountToQuery(String originQuery, int maxresult) {
        return "select * from (" + originQuery + ") where rownum <= " + maxresult;
    }

    /**
     * recursive method to find all occurrences of attributes annotated with 'annotation' having a value that equals
     * 'value'.
     * 
     * @param bean bean to be searched through all it's attributes
     * @param packagePrefix to filter classes to be checked
     * @param annotation annotation type
     * @param annotationValue can be null to search for null values
     * @param match (optional)
     * @param checkedInstances (optional)
     * @return all matches
     */
    public static Collection findAnnotationInEntityTree(Object bean,
            String packagePrefix,
            Class<? extends Annotation> annotation,
            Object annotationValue,
            Collection match,
            Collection checkedInstances) {
        if (bean == null)
            return match;
        if (checkedInstances == null)
            checkedInstances = new HashSet();
        if (match == null)
            match = new HashSet();
        if (bean instanceof Collection) {
            //loop over oneToMany collection ignoring lazyinit-exceptions
            try {
                Collection oneToMany = (Collection) bean;
                for (Object b : oneToMany) {
                    findAnnotationInEntityTree(b, packagePrefix, annotation, annotationValue, match, checkedInstances);
                    return match;
                }
            } catch (Exception ex) {
                LOG.debug(ex);
                return match;
            }
        }
        if (checkedInstances.contains(bean))
            return match;
        if (!bean.getClass().getPackage().getName().startsWith(packagePrefix))
            return match;
        LOG.debug("checking instance: " + bean);
        BeanClass<?> beanClass = new BeanClass(bean.getClass());
        Collection<BeanAttribute> ids = beanClass.findAttributes(annotation);
        if (ids != null && ids.size() > 0)
            if (ids.iterator().next().getValue(bean) == annotationValue || (annotationValue != null && annotationValue.equals(ids.iterator()
                .next()))) {
                LOG.info("matched value on bean: " + bean);
                match.add(bean);
            }
        checkedInstances.add(bean);
        for (BeanAttribute attr : beanClass.getAttributes()) {
            findAnnotationInEntityTree(attr.getValue(bean),
                packagePrefix,
                annotation,
                annotationValue,
                match,
                checkedInstances);
        }
        LOG.info("found matches for annotation " + annotation
            + " with value "
            + annotationValue
            + "\n"
            + StringUtil.toFormattedString(match, 200, true));
        return match;
    }

}
