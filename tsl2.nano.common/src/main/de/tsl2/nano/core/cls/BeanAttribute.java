/*
 * SVN-INFO: $Id: BeanAttribute.java,v 1.0 07.12.2008 18:19:11 15:03:02 ts Exp $ 
 * 
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.core.cls;

import java.io.Serializable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * used by the class {@link BeanClass} to represent its bean attributes. The bean attributes are handled through its
 * read- and write access methods.
 * 
 * on default, only a read access method is required to define a bean attribute.
 * 
 * @author ts 07.12.2008
 * @version $SVN_REV: 1.0 $
 * 
 */
@Default(value = DefaultType.FIELD, required = false)
@SuppressWarnings("rawtypes")
public class BeanAttribute<T> implements IAttribute<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -5107086042716326477L;

    /**
     * only for serialization and de-serialization we need class+name information to recreate the non-serializable
     * Method instance. See {@link #writeObject(java.io.ObjectOutputStream)} and
     * {@link #readObject(java.io.ObjectInputStream)}.
     */
    @Attribute
    Class<?> declaringClass;
    /** see {@link #declaringClass} */
    @Attribute
    String name;

    /** getter method of the bean attribute */
    protected transient Method readAccessMethod;
    /** setter method of the bean attribute */
    transient Method writeAccessMethod;

    public static final String PREFIX_READ_ACCESS = "get";
    static final String PREFIX_BOOLEAN_READ_ACCESS = "is";
    public static final String PREFIX_WRITE_ACCESS = "set";
    static final String PREFIX_CLASS = "class";

    static final Object[] EMPTY_ARG = new Object[0];
    static final Class<?>[] EMPTY_CLS_ARG = new Class[0];

    public static final String ATTR_ENUM_NAME = "name";
    public static final String REGEXP_ATTR_NAME = "[a-z][a-zA-Z0-9_]*";

    private static final Log LOG = LogFactory.getLog(BeanAttribute.class);

    /**
     * @return see {@link #getBeanAttribute(Class, String, boolean)} with throwException=true
     */
    public static final BeanAttribute getBeanAttribute(Class<?> clazz, String attributeName) {
        return getBeanAttribute(clazz, attributeName, true);
    }

    /**
     * creates a bean attribute representation - if at least a 'get' or 'is' method is available for the given attribute
     * name. if only a 'set' method is available, this factory method fails. if you need to change a bean attribute
     * through its setter method, but no getter is available, use {@link BeanClass#setValue(Object, String, Object)}
     * instead.
     * 
     * @param clazz class of this attribute
     * @param attributeName name of attribute
     * @param throwException if true, an exception will be thrown, if no access method found
     * @return an instance of BeanAttribute with the given attributeName of clazz - or null (if throwException = false).
     */
    public static final BeanAttribute getBeanAttribute(Class<?> clazz, String attributeName, boolean throwException) {
        /*
         * performance: extensions of BeanClass will not be stored in cache, so
         * define a new cached-class to get the methods from there.
         */
        BeanClass cachedBC = CachedBeanClass.getCachedBeanClass(clazz);
        if (cachedBC != null)
            return (BeanAttribute) cachedBC.getAttribute(attributeName);
        else {
            Method method = getReadAccessMethod(clazz, attributeName, throwException);
            return method != null ? new BeanAttribute(method) : null;
        }
    }

    /**
     * getReadAccessMethod
     * 
     * @param clazz class of this attribute
     * @param attributeName name of attribute
     * @param throwException if true, an exception will be thrown, if no access method found
     * @return an instance of Method with the given attributeName of clazz - or null.
     */
    protected static final Method getReadAccessMethod(Class<?> clazz, String attributeName, boolean throwException) {
        String methodName = getExpectedMethodName(attributeName);
        try {
            return clazz.getMethod(methodName, EMPTY_CLS_ARG);
        } catch (final Exception e) {
            methodName = PREFIX_BOOLEAN_READ_ACCESS + (attributeName.length() > 0 ? attributeName.substring(0, 1)
                .toUpperCase() + attributeName.substring(1) : "");
            try {
                return clazz.getMethod(methodName, EMPTY_CLS_ARG);
            } catch (final Exception e1) {
                if (throwException)
                    ManagedException.forward(e);
                else
                    LOG.debug("No access method for attribute '" + attributeName
                        + "' available on class "
                        + clazz.getName());
                return null;
            }
        }
    }

    private static final String getExpectedMethodName(String attributeName) {
        return PREFIX_READ_ACCESS
            + (attributeName.length() > 0 ? attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1)
                : "");
    }

    /**
     * enables reflection on getter/setter methods, if methods are not public
     */
    public void removeAccessCheck() {
        readAccessMethod.setAccessible(true);
        if (writeAccessMethod != null) {
            writeAccessMethod.setAccessible(true);
        }
    }

    /**
     * for internal use only!
     * 
     * @return getter method of current attribute
     */
    public Method getAccessMethod() {
        return readAccessMethod;
    }

    /**
     * searches for the write access method belonging to the given read access method.
     * 
     * @param readAccessMethod
     * @return the setter method for the given getter method or null if not available.
     */
    Method getWriteAccessMethod(Method readAccessMethod) {
        if (writeAccessMethod == null) {
            assert readAccessMethod.getName().startsWith(PREFIX_READ_ACCESS) || readAccessMethod.getName()
                .startsWith(PREFIX_BOOLEAN_READ_ACCESS) : "method has to start with " + PREFIX_READ_ACCESS;
            //use the generic name through readAccessMethod, because extension may override getName() returning a presentation name.
            final String attributeName = getName(readAccessMethod);
            try {
                writeAccessMethod = readAccessMethod.getDeclaringClass()
                    .getMethod(PREFIX_WRITE_ACCESS + toFirstUpper(attributeName),
                        new Class[] { readAccessMethod.getReturnType() });
            } catch (final SecurityException e) {
                ManagedException.forward(e);
                return null;
            } catch (final NoSuchMethodException e) {
                //ok --> no write acces method available!
            }
        }
        return writeAccessMethod;
    }

    /**
     * constructor to be serializable
     */
    protected BeanAttribute() {
        super();
    }

    /**
     * Constructor
     * 
     * @param readAccessMethod getter method to evaluate this attribute
     */
    public BeanAttribute(Method readAccessMethod) {
        super();
        this.readAccessMethod = readAccessMethod;
    }

    /**
     * @return simple class name + attribute name. the class name starts with a lower case to follow the same rules as
     *         used on generating presenters.
     */
    public String getId() {
        return toFirstLower(readAccessMethod.getDeclaringClass().getSimpleName()) + "." + getName();
    }

    /**
     * @param beanInstance bean
     * @return value of bean attribute for the given instance
     */
    @SuppressWarnings("unchecked")
    public T getValue(Object beanInstance) {
        try {
            readAccessMethod.setAccessible(true);
            return (T) readAccessMethod.invoke(beanInstance, EMPTY_ARG);
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * @param beanInstance bean
     * @param value value to set
     */
    public void setValue(Object beanInstance, Object value) {
        if (hasWriteAccess()) {
            try {
                writeAccessMethod.setAccessible(true);
                writeAccessMethod.invoke(beanInstance, new Object[] { value });
            } catch (final Exception e) {
                ManagedException.forward(e);
            }
        } else {
            LOG.warn("no write access for attribute value '" + getName() + "'! missing setter for: " + readAccessMethod);
        }
    }

    /**
     * @return attribute name
     */
    public String getName() {
        return getName(readAccessMethod);
    }

    /**
     * @param readAccessMethod getter method
     * @return attribute name
     */
    public static final String getName(Method readAccessMethod) {
        final String name = readAccessMethod.getName().startsWith(PREFIX_READ_ACCESS) ? readAccessMethod.getName()
            .substring(PREFIX_READ_ACCESS.length()) : readAccessMethod.getName()
            .substring(PREFIX_BOOLEAN_READ_ACCESS.length());
        return toFirstLower(name);
    }

    /**
     * convenience method to get an attribute name through its type
     * 
     * @param returnType attribute type
     * @return attribute name
     */
    public static String getAttributeName(Class returnType) {
        return toFirstLower(returnType.getSimpleName());
    }

    /**
     * @return attribute name with first upper
     */
    public String getNameFU() {
        return toFirstUpper(getName());
    }

    /**
     * @return type of attribute
     */
    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        if (readAccessMethod == null)
            initDeserializing();
        return (Class<T>) readAccessMethod.getReturnType();
    }

    /**
     * tries to get the generic type. if not defined, Object.class will be returned
     * 
     * @return generic type of attribute, or Object.class
     */
    public Class<?> getGenericType() {
        return getGenericType(readAccessMethod, 0);
    }

    /**
     * tries to get the generic type. if not defined, Object.class will be returned
     * 
     * @return generic type of attribute, or Object.class
     */
    public Class<?> getGenericType(int typePos) {
        return getGenericType(readAccessMethod, typePos);
    }

    /**
     * tries to get the generic type. if not defined, Object.class will be returned
     * 
     * @return generic type of attribute, or Object.class
     */
    public static Class<?> getGenericType(Method method, int typePos) {
        Object genType = method.getGenericReturnType();
        if (genType instanceof ParameterizedType)
            genType = ((ParameterizedType) genType).getActualTypeArguments()[typePos];
        return genType instanceof Class ? (Class<?>) genType : Object.class;
    }

    /**
     * @return whether there is a public setter defined
     */
    public boolean hasWriteAccess() {
        return getWriteAccessMethod(readAccessMethod) != null;
    }

    /**
     * getDeclaringClass
     * 
     * @return declaring class
     */
    public Class getDeclaringClass() {
        return readAccessMethod.getDeclaringClass();
    }

    /**
     * firstToUpperCase
     * 
     * @param string to convert
     * @return converted string
     */
    public static String toFirstUpper(String string) {
        return StringUtil.toFirstUpper(string);
    }

    /**
     * firstToUpperCase
     * 
     * @param string to convert
     * @return converted string
     */
    public static String toFirstLower(String string) {
        // must use the Java Beans Spec implementation ... since required by Eclipse Binding Framework
        // see also BeanObservableValue.getPropertyDescriptor(Class beanClass, String propertyName)
        // means we have the special case where "string" starts with 2 uppercase characters 
        // => the first character is NOT decapitalized ...
        return Environment.get("bean.attribute.decapitalize", true) ? /*Introspector.*/decapitalize(string) : string
            .substring(0, 1).toLowerCase() + string.substring(1);
    }

    /**
     * simple copy of oracle/sun jdk implementation of java.beans.Introspector.decapitalize(String name).<br/>
     * as the package 'java.beans' is not included in all vm's (like dalvik), we copied the simple algorithm to be
     * independent of that package.
     * <p/>
     * Original javadoc:<br/>
     * Utility method to take a string and convert it to normal Java variable name capitalization. This normally means
     * converting the first character from upper case to lower case, but in the (unusual) special case when there is
     * more than one character and both the first and second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays as "URL".
     * 
     * @param name The string to be decapitalized.
     * @return The decapitalized version of the string.
     */
    private static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(IAttribute<T> o) {
        //if o is null, the error occurred before!
        return getName().compareTo(o.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return readAccessMethod.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    /**
     * reads annotation from method or field
     * 
     * @param <A> annotation
     * @param annotationType
     * @return annotation of given type or null
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        final A methodAnn = getMethodAnnotation(annotationType);
        if (methodAnn != null) {
            return methodAnn;
        }
        //try the same on a field
        return getFieldAnnotation(annotationType);
    }

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will not use objects reference, but
     * class.getName(). so, different classloaders will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }

        //direct access
//        return readAccessMethod.getAnnotation(annotationClass);

        //indirect access
        return BeanClass.getAnnotation(readAccessMethod.getAnnotations(), annotationClass);
    }

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will not use objects reference, but
     * class.getName(). so, different classloaders will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public <A extends Annotation> A getFieldAnnotation(Class<A> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }

        Field f = null;
        try {
            f = readAccessMethod.getDeclaringClass().getDeclaredField(getName());
            //direct access
//            return f.getAnnotation(annotationClass);
        } catch (final Exception e) {
            if (f == null) {
                return null;
            } else {
                ManagedException.forward(e);
            }
        }
        //indirect access
        return BeanClass.getAnnotation(f.getAnnotations(), annotationClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return readAccessMethod.toGenericString();
    }

    /**
     * Extension for {@link Serializable}
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        //Method is not serializable, so we save informations to reconstruct it.
        initSerialization();
        out.defaultWriteObject();
    }

    @Persist
    private void initSerialization() {
        declaringClass = readAccessMethod.getDeclaringClass();
        name = getName(readAccessMethod);
    }

    /**
     * Extension for {@link Serializable}
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        //Method ist not serializable, so we use informations to reconstruct it.
        in.defaultReadObject();
        initDeserializing();
    }

    @Commit
    private void initDeserializing() {
        readAccessMethod = getReadAccessMethod(declaringClass, name, true);
    }

    public static boolean hasExpectedName(Method method) {
        String n = decapitalize(method.getName().substring(3));
        return getExpectedMethodName(n).equals(method.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVirtual() {
        return false;//IValueAccess.class.isAssignableFrom(getDeclaringClass());
    }

//    private void readObjectNoData() throws ObjectStreamException {
//
//    }
}
