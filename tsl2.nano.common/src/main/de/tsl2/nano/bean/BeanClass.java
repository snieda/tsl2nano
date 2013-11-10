/*
 * SVN-INFO: $Id: BeanClass.java,v 1.0 07.12.2008 11:36:00 15:03:02 ts Exp $ 
 * 
 * Copyright © 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.bean;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * used to constrain a pojo class to its bean class properties.
 * 
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 * 
 */
@Default(value = DefaultType.FIELD, required = false)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BeanClass<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 8387513854853569951L;

    private static final Log LOG = LogFactory.getLog(BeanClass.class);

    /** bean class */
    @Attribute
    protected Class<T> clazz;

    protected static final IAction<Boolean> FILTER_SINGLEVALUE_ATTRIBUTES = new CommonAction<Boolean>() {
        @Override
        public Boolean action() throws Exception {
            return BeanUtil.isSingleValueType(((BeanAttribute) getParameter()[0]).getType());
        }
    };
    protected static final IAction<Boolean> FILTER_MULTIVALUE_ATTRIBUTES = new CommonAction<Boolean>() {
        @Override
        public Boolean action() throws Exception {
            return !BeanUtil.isSingleValueType(((BeanAttribute) getParameter()[0]).getType());
        }
    };

    /**
     * constructor to be serializable
     */
    protected BeanClass() {
        super();
    }

    /**
     * Constructor
     */
    public BeanClass(Class<T> beanClass) {
        this.clazz = beanClass;
    }

    /**
     * getName
     * 
     * @return simple class name
     */
    public String getName() {
        return getName(clazz);
    }

    /**
     * returns the simple class name. if class is a proxy, we return the simple name of the first interface.
     * 
     * @param clazz normal class or proxy class
     * @return simple class name
     */
    public static final String getName(Class clazz) {
        return getDefiningClass(clazz).getSimpleName();
    }

    /**
     * @return class of bean
     */
    public Class<T> getClazz() {
        return clazz;
    }

    /**
     * at least read access methods
     * 
     * @see #getAttributes(boolean)
     */
    public List<BeanAttribute> getAttributes() {
        return getAttributes(false);
    }

    /**
     * returns all bean attributes. the collection is a linkedlist, the order is the same as returned by
     * {@linkplain Class#getMethods()}. The elements in the array returned are not sorted and are not in any particular
     * order, but in most cases, they will be ordered equal to their definitions in their source files.
     * 
     * @param readAndWriteAccess if false, only a public read access method must be defined.
     * @return list of methods having at least an read access method.
     */
    public List<BeanAttribute> getAttributes(boolean readAndWriteAccess) {
        final Method[] allMethods = clazz.getMethods();
        final LinkedList<String> accessedMethods = new LinkedList<String>();
        final List<BeanAttribute> beanAccessMethods = new LinkedList<BeanAttribute>();
        for (int i = 0; i < allMethods.length; i++) {
            if (allMethods[i].getParameterTypes().length == 0 && (allMethods[i].getName()
                .startsWith(BeanAttribute.PREFIX_READ_ACCESS) || allMethods[i].getName()
                .startsWith(BeanAttribute.PREFIX_BOOLEAN_READ_ACCESS))) {
                if (allMethods[i].getName().equals("getClass")) {
                    continue;
                }
                //an attribute can be defined in more than one interface --> create only one bean attribute
                if (accessedMethods.contains(allMethods[i].getName())) {
                    continue;
                }
                if (!readAndWriteAccess || hasWriteAccessMethod(allMethods[i])) {
                    BeanAttribute attr = new BeanAttribute(allMethods[i]);
                    //check, if attribute is BeanAttribute-compatible - but only if not boolean-type (because of get and is)
                    if (isAssignableFrom(Boolean.class, allMethods[i].getReturnType()) || BeanAttribute.hasExpectedName(allMethods[i])) {
                        beanAccessMethods.add(attr);
                        accessedMethods.add(allMethods[i].getName());
                    } else {
                        LOG.warn("method " + allMethods[i]
                            + " doesn't respect uppercase-starting getter ==> will be ignored!");
                    }

                }
            }
        }
        return beanAccessMethods;
    }

    /**
     * returns all bean attributes wrapped into a sorted set.
     * 
     * @see #getAttributes(boolean)
     * 
     * @param readAndWriteAccess if false, only a public read access method must be defined.
     * @return list of methods having at least an read access method.
     */
    public SortedSet<BeanAttribute> getSortedAttributes(boolean readAndWriteAccess) {
        return new TreeSet<BeanAttribute>(getAttributes(readAndWriteAccess));
    }

    /**
     * getAttributeNames
     * 
     * @return all attribute names
     */
    public String[] getAttributeNames() {
        return getAttributeNames(false);
    }

    /**
     * getAttributeNames
     * 
     * @param readAndWriteAccess if true, only attributes having getter and setter will be returned
     * @return available attribute names
     */
    public String[] getAttributeNames(boolean readAndWriteAccess) {
        final Collection<BeanAttribute> beanAttributes = getAttributes(readAndWriteAccess);
        final String[] names = new String[beanAttributes.size()];
        int i = 0;
        for (final BeanAttribute beanAttribute : beanAttributes) {
            names[i++] = beanAttribute.getName();
        }
        return names;
    }

    /**
     * all obtained attributes will be filtered through the given filter. the filter-parameters must be of type
     * {@linkplain BeanAttribute}.
     * 
     * @param filter attribute filter
     * @return filtered attributes
     */
    public List<BeanAttribute> getFilteredAttributes(IAction<Boolean> filter) {
        final List<BeanAttribute> beanAttributes = getAttributes();
        final Object[] args = new Object[1];
        filter.setParameter(args);
        for (final Iterator iterator = beanAttributes.iterator(); iterator.hasNext();) {
            final BeanAttribute beanAttribute = (BeanAttribute) iterator.next();
            args[0] = beanAttribute;
            if (!filter.activate()) {
                iterator.remove();
            }
        }
        return beanAttributes;
    }

    /**
     * getFilteredMethods
     * 
     * @param filter
     * @return
     */
    public Collection<Method> getFilteredMethods(IAction<Boolean> filter) {
        Method[] methods = clazz.getMethods();
        ArrayList<Method> methodList = new ArrayList<Method>(methods.length);
        Object[] args = new Object[1];
        filter.setParameter(args);
        for (int i = 0; i < methods.length; i++) {
            args[0] = methods[i];
            if (filter.activate())
                methodList.add(methods[i]);
        }
        return methodList;
    }

    /**
     * tries to find methods with given annotation. if not existing, return empty list. it is a generic method with poor
     * performance.
     * 
     * @param annotationType annotation to find attributes for (will look on methods and declared field!)
     * @return annotation attributes.
     */
    public Collection<BeanAttribute> findAttributes(Class<? extends Annotation> annotationType) {
        LOG.debug("evaluate attributes with annotation :" + annotationType);
        final Collection<BeanAttribute> attributes = new LinkedList<BeanAttribute>();
        final Method[] methods = clazz.getMethods();
        for (final Method m : methods) {
            if (getAnnotation(m.getAnnotations(), annotationType) != null) {
                attributes.add(BeanAttribute.getBeanAttribute(clazz, BeanAttribute.getName(m)));
            }
        }
        //on a declared or base (perhaps private) field?
        Class<?> hierClass = clazz;
        while (hierClass != null) {
            final Field[] fields = hierClass.getDeclaredFields();
            for (final Field f : fields) {
                if (getAnnotation(f.getAnnotations(), annotationType) != null) {
                    LOG.debug("declared field with annotation found: " + f);
                    attributes.add(BeanAttribute.getBeanAttribute(hierClass, f.getName()));
                }
            }
            // it's not possible to call a private field from extending class
            hierClass = null;//hierClass.getSuperclass();
        }
        return attributes;
    }

    /**
     * return only single-value-attributes
     * 
     * @return attributes
     */
    public List<BeanAttribute> getSingleValueAttributes() {
        return getFilteredAttributes(FILTER_SINGLEVALUE_ATTRIBUTES);
    }

    /**
     * return only single-value-attributes
     * 
     * @return attributes
     */
    public List<BeanAttribute> getMultiValueAttributes() {
        return getFilteredAttributes(FILTER_MULTIVALUE_ATTRIBUTES);
    }

    /**
     * searches for the write access method belonging to the given read access method.
     * 
     * @param readAccessMethod
     * @return true, if there is a setter method for the given getter method.
     */
    boolean hasWriteAccessMethod(Method readAccessMethod) {
        return new BeanAttribute(readAccessMethod).hasWriteAccess();
    }

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will not use objects reference, but
     * class.getName(). so, different classloaders will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public <A extends Annotation> Class<? extends Annotation> getAnnotation(Class<A> annotationClass) {
        final Annotation annotation = getAnnotation(clazz.getAnnotations(), annotationClass);
        return annotation != null ? annotation.annotationType() : null;
    }

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will not use objects reference, but
     * class.getName(). so, different classloaders will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    static final <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }

        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().getName().equals(annotationClass.getName())) {
                return (A) annotations[i];
            }
        }
        return null;
    }

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will not use objects reference, but
     * class.getName(). so, different classloaders will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * getField
     * 
     * @param instance object instance having the field
     * @param fieldName fields name
     * @return field value
     */
    public Object getField(T instance, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * setField
     * 
     * @param instance object instance having the field
     * @param fieldName fields name
     * @param value fields new value
     */
    public void setField(T instance, String fieldName, Object value) {
        try {
            Field field = clazz.getField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            ForwardedException.forward(e);
        }
    }

    /**
     * simple method reflection call without parameter
     * 
     * @param instance object
     * @param methodName method name to call
     * @return method call result
     */
    public Object callMethod(Object instance, String methodName) {
        return callMethod(instance, methodName, new Class[0], new Object[0]);
    }

    /**
     * delegates to {@link #callMethod(Object, String, Class[], Object...)} - instance must not be null!
     */
    public static Object call(Object instance, String methodName, Class[] par, Object... args) {
        return new BeanClass(instance.getClass()).callMethod(instance, methodName, par, args);
    }

    /**
     * simple method reflection call
     * 
     * @param instance object
     * @param methodName method name to call
     * @param par (optional, if null, args-classes will be used) method parameter
     * @param args method argument objects
     * @return method call result
     */
    public Object callMethod(Object instance, String methodName, Class[] par, Object... args) {
        //if par is null we try to fill through the objects
        //but no object should be null!
        if (par == null) {
            par = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    par[i] = args[i].getClass();
                } else {
                    par[i] = Object.class;
                }
            }
        }
        try {
            LOG.debug("calling " + clazz.getName()
                + "."
                + methodName
                + " with parameters:"
                + StringUtil.toString(par, 80));
            return clazz.getMethod(methodName, par).invoke(instance, args);
        } catch (final Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * evaluate the value of the given bean attribute path
     * 
     * @param bean starting instance
     * @param path full relation path, separated by '.'
     * @return attribute value or null
     */
    public static Object getValue(Object bean, String path) {
        return getValue(bean, path.split("\\."));
    }

    /**
     * evaluate the value of the given bean attribute path
     * 
     * @param bean starting instance
     * @param path full relation path
     * @return attribute value or null
     */
    public static Object getValue(Object bean, String... path) {
        Object value = bean;
        BeanValue beanValue;
        for (int i = 0; i < path.length; i++) {
            try {
                beanValue = BeanValue.getBeanValue(value, path[i]);
            } catch (final Exception ex) {
                throw new ForwardedException("Error on attribute path '" + StringUtil.toString(path, 1000)
                    + "'! Attribute '"
                    + path[i]
                    + "' not available!", ex);
            }
            if (beanValue.getValue() == null) {
                LOG.info("attribute '" + path[i] + "' of full path '" + StringUtil.toString(path, 1000) + "' is null");
                return null;
            }
            value = beanValue.getValue();
        }
        return value;
    }

    /**
     * is able to set a value of a bean.
     * 
     * @param instance bean instance
     * @param attributeName attribute name
     * @param value new attribute value
     */
    public void setValue(T instance, String attributeName, Object value) {
        final String methodName = BeanAttribute.PREFIX_WRITE_ACCESS + attributeName.substring(0, 1).toUpperCase()
            + attributeName.substring(1);
        final Class<?> argType = value != null ? value.getClass() : Object.class;
        try {
            LOG.debug("calling " + methodName + "(" + value + ")");
            final Method method = clazz.getMethod(methodName, new Class[] { argType });
            method.invoke(instance, value);
        } catch (final Exception e) {
            ForwardedException.forward(e);
        }
    }

    /**
     * creates a new instance through the given arguments
     * 
     * @param args constructor arguments
     * @return new bean instance
     */
    public T createInstance(Object... args) {
        return createInstance(clazz, args);
    }

    /**
     * hasDefaultConstructor
     * 
     * @param clazz
     * @return
     */
    public static boolean hasDefaultConstructor(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor(new Class[0]) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * creates a new instance through the given arguments. if you don't call the default constructor, the performance
     * will go down on searching the right constructor.
     * 
     * @param args constructor arguments
     * @return new bean instance
     */
    public static <T> T createInstance(Class<T> clazz, Object... args) {
        T instance = null;
        if (args.length == 0) {//--> default constructor
            try {
                Constructor<T> constructor = clazz.getConstructor(new Class[0]);
                constructor.setAccessible(true);
                instance = constructor.newInstance();
//                instance = (T) clazz.newInstance();
            } catch (final Exception e) {
                //ok, try it on declared constructors
                try {
                    Constructor<T> constructor = clazz.getDeclaredConstructor(new Class[0]);
                    constructor.setAccessible(true);
                    instance = constructor.newInstance();
                } catch (final Exception e1) {
                    ForwardedException.forward(e1);
                }
            }
        } else {//searching for the right constructor
            final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (int i = 0; i < constructors.length; i++) {
                Class<?>[] pars = constructors[i].getParameterTypes();
                if (pars.length == args.length) {
                    for (int j = 0; j < args.length; j++) {
                        //if one argument type mismatches, we search for another constructor
                        if (args[j] != null && !pars[j].isAssignableFrom(args[j].getClass())) {
                            pars = null;
                            break;
                        }
                    }
                    if (pars != null) {
                        try {
                            constructors[i].setAccessible(true);
                            instance = (T) constructors[i].newInstance(args);
                        } catch (final Exception e) {
                            ForwardedException.forward(e);
                        }
                    }
                }
            }
            if (instance == null) {
                throw FormattedException.implementationError("BeanClass could not create the desired instance of type " + clazz,
                    args,
                    constructors);
            }
        }
        return instance;
    }

    /**
     * delegates to {@link #createInstance(Class, Object...)} using Threads current classloader
     */
    public static BeanClass createBeanClass(String className) {
        return createBeanClass(className, null);
    }

    /**
     * createBeanClass
     * 
     * @param className class for beanclass instance
     * @return new beanclass instance
     */
    public static BeanClass createBeanClass(String className, ClassLoader classLoader) {
        Class clazz = load(className, classLoader);
        return new BeanClass(clazz);
    }

    /**
     * load
     * 
     * @param className class to load
     * @param classloader (optional) if null, the threads context classloader will be used.
     * @return loaded class
     */
    static Class load(String className, ClassLoader classloader) {
        if (classloader == null)
            classloader = Thread.currentThread().getContextClassLoader();
        try {
            LOG.debug("loading class " + className + " through classloader " + classloader);
            return classloader.loadClass(className);
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * delegates to {@link #copyValues(Object, Object, String...)} using all destination attributes having a setter.
     */
    public static <D> D copyValues(Object src, D dest, boolean onlyDestAttributes) {
        final BeanClass destClass = new BeanClass(dest.getClass());
        String[] attributeNames = destClass.getAttributeNames(true);
        return copyValues(src, dest, attributeNames);
    }

    /**
     * delegates to {@link #copyValues(Object, Object, boolean, String...)} with onlyIfNotNull = false.
     * 
     */
    public static <D> D copyValues(Object src, D dest, String... attributeNames) {
        return copyValues(src, dest, false, attributeNames);
    }

    /**
     * copy equal-named attributes from src to dest. if no attributeNames are defined, all source attributes will be
     * copied.
     * <p>
     * Warning: not performance optimized!
     * 
     * @param src source bean
     * @param dest destination bean (may be an extension of source bean - or simply a bean with some equal-named
     *            attributes)
     * @param onlyIfNotNull if true, the source values will not overwrite existing destination-values with null.
     * @param attributeNames (optional) fixed attribute names
     * @return destination object
     */
    public static <D> D copyValues(Object src, D dest, boolean onlyIfNotNull, String... attributeNames) {
        int copied = 0;
        if (attributeNames.length == 0) {
            final BeanClass srcClass = new BeanClass(src.getClass());
            attributeNames = srcClass.getAttributeNames();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("copying " + attributeNames.length
                + " attributes from "
                + src.getClass().getSimpleName()
                + " to "
                + dest.getClass().getSimpleName());
        }
        Collection<String> unavailable = new LinkedList<String>();
        for (int i = 0; i < attributeNames.length; i++) {
            final BeanAttribute srcAttribute = BeanAttribute.getBeanAttribute(src.getClass(), attributeNames[i]);
            BeanAttribute destAttribute;
            destAttribute = BeanAttribute.getBeanAttribute(dest.getClass(), attributeNames[i], false);
            if (destAttribute == null) {
                unavailable.add(attributeNames[i]);
                continue;
            }
            if (destAttribute.hasWriteAccess()) {
                Object value = srcAttribute.getValue(src);
                if (!onlyIfNotNull || value != null) {
                    destAttribute.setValue(dest, value);
                    copied++;
                }
            } else {
                unavailable.add(attributeNames[i]);
            }
        }
        if (copied < attributeNames.length) {
            LOG.warn("couldn't set all values for " + dest.getClass().getSimpleName()
                + "! unavailable attributes: "
                + StringUtil.toString(unavailable, 200));
        }
        return dest;
    }

    /**
     * wraps all attributes having a collection as value into a new {@link ListSet} instance to unbind a
     * {@link #clone()} instance.
     * 
     * @param src instance to wrap the attribute values for
     * @return the instance itself
     */
    public static <S> S createOwnCollectionInstances(S src) {
        BeanClass<S> bc = new BeanClass(src.getClass());
        List<BeanAttribute> attributes = bc.getAttributes();
        for (BeanAttribute a : attributes) {
            if (Collection.class.isAssignableFrom(a.getType())) {
                Collection v = (Collection) a.getValue(src);
                if (v != null) {
                    LOG.debug("creating own collection instance for " + a.getName() + " with" + v.size() + " elements");
                    a.setValue(src, new ListSet(v));
                }
            }
        }
        return src;
    }

    /**
     * sets all attributes from src to the value of a new created instance (mostly null values). if no attributeNames
     * are defined, all source attributes will be copied.
     * <p>
     * f Warning: Works only, if src class provides a default constructor. not performance optimized!
     * 
     * @param src source bean
     * @param attributeNames (optional) fixed attribute names
     * @return reseted source object
     */
    public static <S> S resetValues(S src, String... attributeNames) {
        return (S) copyValues(createInstance(src.getClass()), src, false);
    }

    /**
     * collects recursive all fields of this class and all of its super classes
     * 
     * @param cls class to search all fields for
     * @param fields container to fill the fields into.
     * @return all fields of given class
     */
    protected static Field[] fieldsOf(Class cls, List<Field> fields) {
        if (fields == null)
            fields = new ArrayList<Field>();
        if (cls.getSuperclass() != null)
            fieldsOf(cls.getSuperclass(), fields);
        fields.addAll(Arrays.asList(cls.getDeclaredFields()));
        return fields.toArray(new Field[0]);
    }

    /**
     * deep copy of all fields from src to dest (if available in dest). src class should be assignable from dest or vice
     * versa.
     * <p>
     * Warning: not performance optimized! Matches fields by names without checking type!
     * 
     * @param src source bean
     * @param dest destination bean (may be an extension of source bean - or simply a bean with some equal-named fields)
     * @param noCopy (optional) field names to not be copied
     * @return destination object
     */
    public static <D> D copy(Object src, D dest, String... noCopy) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("copying all" + " fields from "
                + src.getClass().getSimpleName()
                + " to "
                + dest.getClass().getSimpleName());
        }
        Field[] fields = fieldsOf(src.getClass(), null);
        Field[] destFields = fieldsOf(dest.getClass(), null);
        List<String> destFieldNames = new ArrayList<String>(destFields.length);
        List<String> noCopyList = Arrays.asList(noCopy);
        for (int i = 0; i < destFields.length; i++) {
            destFieldNames.add(destFields[i].getName());
        }
        try {
            int c = 0;
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                if (noCopyList.contains(name))
                    continue;
                int di = destFieldNames.indexOf(name);
                if (di != -1) {
                    if (!NumberUtil.hasBit(destFields[di].getModifiers(), Modifier.FINAL)) {
                        fields[i].setAccessible(true);
                        destFields[di].setAccessible(true);
                        destFields[di].set(dest, fields[i].get(src));
                        c++;
                    }
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(c + " fields copied");
            }
        } catch (Exception e) {
            ForwardedException.forward(e);
        }
        return dest;
    }

    static final String ACTION_PREFIX = "action";

    /**
     * getBeanActions
     * 
     * @return all methods (wrapped into actions) starting with 'action' and having no arguments.
     */
    public Collection<IAction> getActions() {
        final Method[] methods = clazz.getMethods();
        final Collection<IAction> actions = new ArrayList<IAction>();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().startsWith(ACTION_PREFIX) && methods[i].getParameterTypes().length == 0) {
                final Method m = methods[i];
                final String name = m.getName().substring(ACTION_PREFIX.length());
                actions.add(new CommonAction<Object>(m.toGenericString(), name, m.toGenericString()) {
                    @Override
                    public Object action() throws Exception {
                        return m.invoke(getParameter()[0], new Object[0]);
                    }
                });
            }
        }
        return actions;
    }

    /**
     * extends the {@link Class#isAssignableFrom(Class)} to check for primitives and their immutables
     * 
     * @param cls1 to be assignable from cls2
     * @param cls2 cls2
     * @return true, if cls.{@link Class#isAssignableFrom(Class)} returns true or the cls1 is primitive of cls2 or vice
     *         versa.
     */
    public static boolean isAssignableFrom(Class<?> cls1, Class<?> cls2) {
        return PrimitiveUtil.isAssignableFrom(cls1, cls2);
    }

    /**
     * checks the inheritance of object o1 and o2.
     * 
     * @param o1 first object
     * @param o2 second object
     * @return true, if o1 or o2 is null or if o1 is an instance of o2 or vice versa.
     */
    public static final boolean isInheritance(Object o1, Object o2) {
        return o1 == null || o2 == null
            || o1.getClass().isAssignableFrom(o2.getClass())
            || o2.getClass().isAssignableFrom(o1.getClass());
    }

    /**
     * As {@link Class#getInterfaces()} will not return the interfaces of the hierarchy and {@link Class#getClasses()}
     * will return all member classes (which is not usable for proxy declarations), this method returns all interfaces
     * of this class and it's hierarchy.
     * 
     * @return all interfaces of the class hierarchy.
     */
    public Class[] getInterfaces() {
        Collection<Class> allInterfaces = new LinkedHashSet<Class>();
        allInterfaces.addAll(Arrays.asList(clazz.getInterfaces()));
        Class<?> superClass = clazz;
        while ((superClass = superClass.getSuperclass()) != null)
            allInterfaces.addAll(Arrays.asList(superClass.getInterfaces()));
        return allInterfaces.toArray(new Class[0]);
    }

    /**
     * useful on anonymous/inner classes, proxies or bytecode-enhancing
     * 
     * @param cls class to analyse
     * @return defining class - means on anonymous classes, proxies or enhancing it returns the the super class or
     *         interface.
     */
    public static final Class<?> getDefiningClass(Class<?> cls) {
        //TODO: how to check for enhancing class
        return (cls.getEnclosingClass() != null || cls.getSimpleName().contains("$")) && cls.getSuperclass() != null ? getDefiningClass(cls.getSuperclass())
            : Proxy.isProxyClass(cls) ? cls.getInterfaces()[0] : cls;
    }
}
