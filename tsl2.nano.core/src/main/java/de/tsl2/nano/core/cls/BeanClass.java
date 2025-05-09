/*
 * Copyright © 2002-2024 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.core.cls;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.AnnotationProxy;
import de.tsl2.nano.core.util.BitUtil;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FieldUtil;
import de.tsl2.nano.core.util.MethodUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.parser.Serial;
import de.tsl2.nano.core.util.parser.StructParser.ASerializer;

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

    protected static final IPredicate<IAttribute<?>> FILTER_SINGLEVALUE_ATTRIBUTES = new IPredicate<IAttribute<?>>() {
        @Override
        public boolean eval(IAttribute<?> arg0) {
            return ObjectUtil.isSingleValueType(arg0.getType());
        }
    };
    protected static final IPredicate<IAttribute<?>> FILTER_MULTIVALUE_ATTRIBUTES = new IPredicate<IAttribute<?>>() {
        @Override
        public boolean eval(IAttribute<?> arg0) {
            return !ObjectUtil.isSingleValueType(arg0.getType());
        }
    };

    /** flag to indicate a code enhancing class */
    public static final String ENHANCER_PREATTACHMENT = "$$";

    /**
     * constructor to be serializable
     */
    protected BeanClass() {
        super();
    }

    /**
     * Constructor
     */
    protected BeanClass(Class<T> beanClass) {
        this.clazz = beanClass;
    }

    /**
     * delegates to {@link #getBeanClass(Class)} using
     * {@link #getDefiningClass(Class)} to evaluate the real class of given
     * instance.
     */
    public static final <C> BeanClass<C> getBeanClass(C instance) {
        return (BeanClass<C>) getBeanClass(instance.getClass());
    }

    /**
     * uses an internal cache through {@link CachedBeanClass} to perform on getting
     * all attributes
     * 
     * @param <C>       bean type
     * @param beanClass bean type
     * @return new or cached instance
     */
    public static final <C> BeanClass<C> getBeanClass(Class<C> beanClass) {
        return getBeanClass(beanClass, true);
    }

    public static final <C> BeanClass<C> getBeanClass(Class<C> beanClass, boolean evalDefiningClass) {
        return CachedBeanClass.getCachedBeanClass(evalDefiningClass ? getDefiningClass(beanClass) : beanClass);
    }

    /**
     * convenience delegating to {@link #getField(Object, String)}.
     * 
     * @param cls             class holding the static field
     * @param staticFieldName static field of cls
     * @return instance of static field
     */
    public static final Object getStatic(Class<?> cls, String staticFieldName) {
        return CachedBeanClass.getCachedBeanClass(cls).getField(null, staticFieldName);
    }

    /**
     * @param type field type
     * @return all field names of class hierarchy that are of the given type.
     */
    public String[] getFieldNames(Class<?> type, boolean staticOnly) {
        return FieldUtil.getFieldNamesInHierarchy(type,
                f -> type.isAssignableFrom(((Field) f).getType())
                        && (!staticOnly || Modifier.isStatic(f.getModifiers())));
    }

    /**
     * @param type return type
     * @return all method names of class hierarchy that return of the given type.
     */
    public Method[] getMethods(Class<?> type, boolean staticOnly) {
    	return staticOnly ? getMethods(type, Modifier.STATIC) : getMethods(type);
    }
    public Method[] getMethods(Class<?> type, int...modifiers) {
        Set<Method> result = new LinkedHashSet<Method>();
        filterMethods(result, clazz.getMethods(), type, modifiers);
        filterMethods(result, clazz.getDeclaredMethods(), type, modifiers);
        return result.toArray(new Method[0]);
    }

	protected void filterMethods(Set<Method> result, Method[] methods, Class<?> type, int...modifiers) {
		for (int i = 0; i < methods.length; i++) {
            if (type.isAssignableFrom(methods[i].getReturnType())
                    && (modifiers.length == 0 || BitUtil.hasBit(methods[i].getModifiers(), modifiers))) {
                result.add(methods[i]);
            }
        }
	}

    /**
     * getName
     * 
     * @return simple class name
     */
    public String getName() {
        return getName(clazz, false);
    }

    /**
     * delegates to {@link #getName(Class, boolean)} with full = false
     */
    public static final String getName(Class clazz) {
        return getName(clazz, false);
    }

    /**
     * evaluates the full enclosing simple name
     * 
     * @param cls class to get the simple name for
     * @return simple-name with all enclosing classes as prefix
     */
    public static String getSimpleName(Class<?> cls) {
        StringBuilder clsName = new StringBuilder(cls.getSimpleName());
        while ((cls = cls.getEnclosingClass()) != null) {
            clsName.insert(0, cls.getSimpleName() + "$");
        }
        return clsName.toString();
    }

    /**
     * returns the simple class name. if class is a proxy, we return the simple name
     * of the first interface.
     * 
     * @param clazz normal class or proxy class
     * @return simple class name
     */
    public static final String getName(Class clazz, boolean path) {
        return path ? getDefiningClass(clazz).getName() : getSimpleName(getDefiningClass(clazz));
    }

    /**
     * delegates to {@link #getPath(Class)}
     */
    public String getPath() {
        return getPath(clazz);
    }

    /**
     * returns the package path of class.
     * 
     * @param clazz normal class or proxy class
     * @return simple class name
     */
    public static final String getPath(Class clazz) {
        return getDefiningClass(clazz).getPackage().getName();
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
    public List<IAttribute> getAttributes() {
        return getAttributes(false);
    }

    /**
     * returns all bean attributes. the collection is a linkedlist, the order is the
     * same as returned by {@linkplain Class#getMethods()}. The elements in the
     * array returned are not sorted and are not in any particular order, but in
     * most cases, they will be ordered equal to their definitions in their source
     * files.
     * 
     * @param readAndWriteAccess if false, only a public read access method must be
     *                           defined.
     * @return list of methods having at least an read access method.
     */
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        /*
         * performance: extensions of BeanClass will not be stored in cache, so define a
         * new cached-class to get the methods from there.
         */
        BeanClass cachedBC = CachedBeanClass.getCachedBeanClass(clazz);
        if (cachedBC != null && cachedBC != this) {
            return cachedBC.getAttributes(readAndWriteAccess);
        }

        final Method[] allMethods = clazz.getMethods();
        Arrays.sort(allMethods, new DeclaredMethodComparator());
        final LinkedList<String> accessedMethods = new LinkedList<String>();
        final List<IAttribute> beanAccessMethods = new LinkedList<IAttribute>();
        for (int i = 0; i < allMethods.length; i++) {
            if (allMethods[i].getParameterTypes().length == 0
                    && (allMethods[i].getName().startsWith(BeanAttribute.PREFIX_READ_ACCESS)
                            || allMethods[i].getName().startsWith(BeanAttribute.PREFIX_BOOLEAN_READ_ACCESS))) {
                if (allMethods[i].getName().equals("getClass")) {
                    continue;
                }
                // an attribute can be defined in more than one interface --> create only one
                // bean attribute
                if (accessedMethods.contains(allMethods[i].getName())) {
                    continue;
                }
                if (!readAndWriteAccess || hasWriteAccessMethod(allMethods[i])) {
                    BeanAttribute attr = new BeanAttribute(allMethods[i]);
                    // check, if attribute is BeanAttribute-compatible - but only if not
                    // boolean-type (because of get and is)
                    if (isAssignableFrom(Boolean.class, allMethods[i].getReturnType())
                            || BeanAttribute.hasExpectedName(allMethods[i])) {
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

    public IAttribute getAttribute(String name) {
        return getAttribute(name, false);
    }

    public IAttribute getAttribute(String name, boolean throwException) {
        List<IAttribute> attrs = getAttributes();
        for (IAttribute a : attrs) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        if (throwException)
            throw new IllegalArgumentException("attribute '" + name + "' not available in " + clazz
                    + "\n\tavailable attributes are:\n" + StringUtil.toFormattedString(attrs, -1, true));
        else
            return null;
    }

    /**
     * getAttribute
     * 
     * @param type to of attribute
     * @return first found attribute having given type
     */
    public IAttribute getAttribute(Class type) {
        List<IAttribute> attrs = getAttributes();
        for (IAttribute a : attrs) {
            if (type.isAssignableFrom(BeanMap.typeOf(a))) {
                return a;
            }
        }
        return null;
    }

    /**
     * returns all bean attributes wrapped into a sorted set.
     * 
     * @see #getAttributes(boolean)
     * 
     * @param readAndWriteAccess if false, only a public read access method must be
     *                           defined.
     * @return list of methods having at least an read access method.
     */
    public SortedSet<IAttribute> getSortedAttributes(boolean readAndWriteAccess) {
        return new TreeSet<IAttribute>(getAttributes(readAndWriteAccess));
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
     * @param readAndWriteAccess if true, only attributes having getter and setter
     *                           will be returned
     * @return available attribute names
     */
    public String[] getAttributeNames(boolean readAndWriteAccess) {
        final Collection<IAttribute> beanAttributes = getAttributes(readAndWriteAccess);
        final String[] names = new String[beanAttributes.size()];
        int i = 0;
        for (final IAttribute beanAttribute : beanAttributes) {
            names[i++] = beanAttribute.getName();
        }
        return names;
    }

    /**
     * all obtained attributes will be filtered through the given filter. the
     * filter-parameters must be of type {@linkplain BeanAttribute}.
     * 
     * @param filter attribute filter
     * @return filtered attributes
     */
    public List<IAttribute> getFilteredAttributes(IPredicate<IAttribute<?>> filter) {
        final List<IAttribute> beanAttributes = getAttributes();
        for (final Iterator<IAttribute> iterator = beanAttributes.iterator(); iterator.hasNext();) {
            final IAttribute beanAttribute = iterator.next();
            if (!filter.eval(beanAttribute)) {
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
    public Collection<Method> getFilteredMethods(IPredicate<Method> filter) {
        Method[] methods = clazz.getMethods();
        ArrayList<Method> methodList = new ArrayList<Method>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            if (filter.eval(methods[i])) {
                methodList.add(methods[i]);
            }
        }
        return methodList;
    }

    /**
     * tries to find methods with given annotation. if not existing, return empty
     * list. it is a generic method with poor performance.
     * 
     * @param annotationType annotation to find attributes for (will look on methods
     *                       and declared field!)
     * @return annotation attributes.
     */
    public Collection<BeanAttribute> findAttributes(Class<? extends Annotation> annotationType) {
        LOG.debug("evaluate attributes with annotation :" + annotationType);
        final Collection<BeanAttribute> attributes = new LinkedList<BeanAttribute>();
        final Method[] methods = clazz.getMethods();
        BeanAttribute attr;
        for (final Method m : methods) {
            if (AnnotationProxy.getAnnotation(m.getAnnotations(), annotationType) != null) {
                attr = BeanAttribute.getBeanAttribute(clazz, BeanAttribute.getName(m));
                if (attr != null)
                    attributes.add(attr);
                else
                    LOG.warn("method " + m + " is annotated with " + annotationType
                            + " but no BeanAttribute could be found on it!");
            }
        }
        // on a declared or base (perhaps private) field?
        Class<?> hierClass = clazz;
        while (hierClass != null) {
            final Field[] fields = hierClass.getDeclaredFields();
            for (final Field f : fields) {
                if (AnnotationProxy.getAnnotation(f.getAnnotations(), annotationType) != null) {
                    LOG.debug("declared field with annotation found: " + f);
                    attr = BeanAttribute.getBeanAttribute(hierClass, f.getName());
                    if (attr != null)
                        attributes.add(attr);
                    else
                        LOG.warn("field " + f + " is annotated with " + annotationType
                                + " but no BeanAttribute could be found on it!");
                }
            }
            // it's not possible to call a private field from extending class
            hierClass = null;// hierClass.getSuperclass();
        }
        return attributes;
    }

    /**
     * return only single-value-attributes
     * 
     * @return attributes
     */
    public List<IAttribute> getSingleValueAttributes() {
        return getFilteredAttributes(FILTER_SINGLEVALUE_ATTRIBUTES);
    }

    /**
     * return only single-value-attributes
     * 
     * @return attributes
     */
    public List<IAttribute> getMultiValueAttributes() {
        return getFilteredAttributes(FILTER_MULTIVALUE_ATTRIBUTES);
    }

    /**
     * searches for the write access method belonging to the given read access
     * method.
     * 
     * @param readAccessMethod
     * @return true, if there is a setter method for the given getter method.
     */
    boolean hasWriteAccessMethod(Method readAccessMethod) {
        return BeanAttribute.getBeanAttribute(readAccessMethod).hasWriteAccess();
    }

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will
     * not use objects reference, but class.getName(). so, different classloaders
     * will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public <A extends Annotation> Class<? extends Annotation> getAnnotation(Class<A> annotationClass) {
        final Annotation annotation = AnnotationProxy.getAnnotation(clazz.getAnnotations(), annotationClass);
        return annotation != null ? annotation.annotationType() : null;
    }

    /**
     * soft evaluation of annotation values. uses reflection instead of direct calls
     * on annotation properties.
     * <p/>
     * evaluates annotation values and returns an object array holding the
     * annotation values in the same order like the member names where given
     * 
     * @param annotationClass type of annotation to be found
     * @param memberNames     annotation members to evaluate the values for
     * @return values of given member names or null, if no annotation of given type
     *         was found
     */
    public <A extends Annotation> Object[] getAnnotationValues(Class<A> annotationClass, String... memberNames) {
        Class a = getAnnotation(annotationClass);
        if (a == null) {
            return null;
        }
        BeanClass bc = getBeanClass(a);
        Object[] values = new Object[memberNames.length];
        for (int i = 0; i < memberNames.length; i++) {
            values[i] = bc.callMethod(a, memberNames[i]);
        }
        return values;
    }

    /**
     * asks for the given annotation. this is a 'soft' implementation, means: will
     * not use objects reference, but class.getName(). so, different classloaders
     * will not be respected!
     * 
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public boolean isAnnotationPresent(Class<? extends Annotation>... annotations) {
        return Arrays.stream(annotations).anyMatch(a -> clazz.isAnnotationPresent(a));
    }

    public boolean isInstanceOf(Class cls) {
        return cls.isAssignableFrom(clazz);
    }

    public static boolean isExtendsionOf(Class<?> cls, Class<?> baseClass) {
        Class<?> superClass = cls.getSuperclass();
        if (superClass == null)
            return false;
        else if (superClass.equals(baseClass))
            return true;
        else
            return isExtendsionOf(superClass, baseClass);
    }

    /**
     * getField
     * 
     * @param instance  object instance having the field
     * @param fieldName fields name
     * @return field value
     */
    public Object getField(T instance, String fieldName) {
        try {
            return getField(instance, fieldName, false);
        } catch (Exception e) {
            try {
                return getField(instance, fieldName, true);
            } catch (Exception e1) {
                return ManagedException.forward(e1);
            }
        }
    }

    /**
     * getField
     * 
     * @param instance  object instance having the field
     * @param fieldName fields name
     * @return field value
     */
    public Object getField(T instance, String fieldName, boolean declared) {
        try {
            Field field = declared ? clazz.getDeclaredField(fieldName) : clazz.getField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            return ManagedException.forward(e, declared);
        }
    }
    
   /**
    * @param <F> type of field
    * @param typeOfField field type
    * @return first found field of given type or null
    */
    public <F> F getField(T instance, Class<F> typeOfField) {
    	return (F) Util.trY(() -> {Field f = getField(typeOfField); f.setAccessible(true); return f.get(instance);});
    }
   
    public Field getField(Class<?> typeOfField) {
    	return Arrays.stream(clazz.getDeclaredFields()).filter(f -> typeOfField.isAssignableFrom(f.getType())).findFirst().orElse(null);
    }
    
    /**
     * setField
     * 
     * @param instance  object instance having the field
     * @param fieldName fields name
     * @param value     fields new value
     */
    public void setField(T instance, String fieldName, Object value) {
        setField(instance, fieldName, value, false);
    }

    /**
     * setField
     * 
     * @param instance  object instance having the field
     * @param fieldName fields name
     * @param value     fields new value
     */
    public void setField(T instance, String fieldName, Object value, boolean declared) {
        try {
            Field field = declared ? clazz.getDeclaredField(fieldName) : clazz.getField(fieldName);
            field.setAccessible(true);
            field.set(instance, ObjectUtil.wrap(value, field.getType()));
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * @param expression like org.myorg.MyClass.myMethod(fixedArg, {0}, fixedArg2,
     *                   {1})
     * @param args       arguments to be filled in
     * @return result of method call
     */
    public static Object callEx(String expression, Object... args) {
        return call(expression, true, args);
    }

    public static Object call(String expression, boolean usePrimitives, Object... args) {
        String cmd = MessageFormat.format(expression, args);
        String cls = StringUtil.substring(cmd, null, "(");
        String mtd = StringUtil.substring(cls, ".", null, true);
        cls = StringUtil.substring(cls, null, ".", true);
        String[] cmdArgs = StringUtil.substring(cmd, "(", ")").split("[,]+");
        if (mtd.equals("main"))
            args = new Object[] { StringUtil.trim(cmdArgs) };
        else if (usePrimitives)
            args = PrimitiveUtil.string2Wrapper(cmdArgs);
        else
            args = StringUtil.trim(cmdArgs);
        return BeanClass.callStatic(cls, mtd, usePrimitives, args);
    }

    /**
     * simple method reflection call without parameter
     * 
     * @param instance   object
     * @param methodName method name to call
     * @return method call result
     */
    public Object callMethod(Object instance, String methodName) {
        return callMethod(instance, methodName, new Class[0], new Object[0]);
    }

    public static Object callStatic(String type, String staticMethodName, Object... args) {
        return createBeanClass(type).callMethod(null, staticMethodName, null, false, args);
    }

    public static Object call(Class<?> type, String staticMethodName, boolean usePrimitives, Object... args) {
        return getBeanClass(type).callMethod(null, staticMethodName, null, usePrimitives, args);
    }

    public static Object callStatic(String type, String staticMethodName, boolean usePrimitives, Object... args) {
        return getBeanClass(load(type)).callMethod(null, staticMethodName, null, usePrimitives, args);
    }

    public static Object call(Class<?> type, String staticMethodName, Class[] par, Object... args) {
        return getBeanClass(type).callMethod(null, staticMethodName, par, args);
    }

    public static Object call(Object instance, String methodName) {
        return getBeanClass(instance.getClass()).callMethod(instance, methodName, new Class[0]);
    }

    /**
     * delegates to {@link #callMethod(Object, String, Class[], Object...)} -
     * instance must not be null!
     */
    public static Object call(Object instance, String methodName, Class[] par, Object... args) {
        return getBeanClass(instance.getClass()).callMethod(instance, methodName, par, args);
    }

    public Object callMethod(Object instance, String methodName, Class[] par, Object... args) {
        return callMethod(instance, methodName, par, false, args);
    }

    /**
     * simple method reflection call
     * 
     * @param instance      object
     * @param methodName    method name to call
     * @param par           (optional, if null, args-classes will be used) method
     *                      parameter
     * @param usePrimitives if par is null and usePrimitives is true, all evaluated
     *                      parameters will primitives if possible!
     * @param args          method argument objects
     * @return method call result
     */
    public Object callMethod(Object instance, String methodName, Class[] par, boolean usePrimitives, Object... args) {
        // if par is null we try to fill through the objects
        // but no object should be null!
        if (par == null) {
            par = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    par[i] = usePrimitives ? PrimitiveUtil.getPrimitive(args[i].getClass()) : args[i].getClass();
                } else {
                    par[i] = Object.class;
                }
            }
        } else {// wrap argument objects to have the right types
            if (args.length == 1 && par.length == 1 && par[0].isArray()) {
                // args = BeanAttribute.wrap(args, par[0]);
            } else {
                for (int i = 0; i < args.length; i++) {
                    if (par.length <= i)
                        break;
                    args[i] = ObjectUtil.wrap(args[i], par[i]);
                }
            }
        }
        Method method;
        try {
            LOG.debug("calling " + (clazz.getClassLoader() != null ? clazz.getClassLoader().toString() + ":" : "")
                    + clazz.getName() + "." + methodName + " with parameters:" + StringUtil.toString(par, 80));
            method = clazz.getMethod(methodName, par);
            LOG.trace(method);
            method.setAccessible(true);
            return method.invoke(instance, args);
        } catch (NoSuchMethodException e1) {
            try {
                method = clazz.getDeclaredMethod(methodName, par);
                LOG.trace(method);
                method.setAccessible(true);
                return method.invoke(instance, args);
            } catch (Exception e2) {
                ManagedException.forward(e2 instanceof NoSuchMethodException ? e1 : e2);
                return null;
            }
        } catch (final Exception e) {
            ManagedException.forward(e);
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

    public static Object getValue(Object bean, String... path) {
        IValueAccess access = ValuePath.getValueAccess(bean, path);
        return access != null ? access.getValue() : null;
    }

    /**
     * is able to set a value of a bean.
     * 
     * @param instance      bean instance
     * @param attributeName attribute name
     * @param value         new attribute value
     */
    public void setValue(T instance, String attributeName, Object value) {
        getAttribute(attributeName, true).setValue(instance, value);
    }

    /**
     * @deprecated not really working on instance path. use {@link ValuePath#getValueAccess(Object, String...)} or {@link #getValue(Object, String...)} instead.
     * 
     * @param attributePath can be given as e.g: myAttr1.myAttr2 or as String array
     *                      MYATTR1, MYATTR2
     * @return last attribute in given path
     */
    public IAttribute getAttributePath(String... attributePath) {
        if (Util.isEmpty(attributePath))
            return null;
        if (attributePath.length == 1) // if attributePath was not given as array but as 'myattr1.myaatr2'
            attributePath = attributePath[0].split("\\.");

        IAttribute attr = getAttribute(attributePath[0]);
        if (attributePath.length == 1) {
            return attr;
        }
        return new BeanClass(BeanMap.typeOf(attr)).getAttributePath(CollectionUtil.copyOfRange(attributePath, 1));
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

    public boolean hasDefaultConstructor(boolean mustBePublic) {
        return hasDefaultConstructor(clazz, mustBePublic);
    }

    /**
     * @delegates to {@link #hasDefaultConstructor(Class, boolean)} with
     *            mustBePublic = false
     */
    public static boolean hasDefaultConstructor(Class<?> clazz) {
        return hasDefaultConstructor(clazz, false);
    }

    public boolean isDefaultInstanceable() {
        return Util.isInstanceable(clazz) && hasDefaultConstructor(clazz);
    }

    /**
     * hasDefaultConstructor
     * 
     * @param clazz        to be checked for the default constructor
     * @param mustBePublic if true, the constructors accessible has to be true
     * @return true, if default constructor available
     */
    public static boolean hasDefaultConstructor(Class<?> clazz, boolean mustBePublic) {
        try {
            Constructor c;
            return (c = clazz.getDeclaredConstructor(new Class[0])) != null && (!mustBePublic || c.isAccessible());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * hasStringConstructor
     * 
     * @param clazz
     * @return
     */
    public static boolean hasStringConstructor(Class<?> clazz) {
    	return hasConstructor(clazz, String.class);
    }

    public static boolean hasConstructor(Class<?> clazz, Class<?> ...parmTypes) {
        try {
            return clazz.getDeclaredConstructor(parmTypes) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * delegates to {@link #createInstance(Class, Object...)} using
     * {@link #load(String, ClassLoader)}.
     */
    public static <T> T createInstance(String clsName, Object... args) {
        return (T) createInstance(load(clsName, null), args);
    }

    /**
     * creating a new instance of clsName. searches the constructor inside the
     * declared constructors of that class.
     * 
     * @param parmTypes constructor parameter types
     * @param args      constructor arguments
     * @return
     */
    public static <T> T createInstance(String clsName, Class[] parmTypes, Object... args) {
        try {
            return (T) load(clsName, null).getDeclaredConstructor(parmTypes).newInstance(args);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * creates a new instance through the given arguments. if you don't call the
     * default constructor, the performance will go down on searching the right
     * constructor. if the given class is a primitive the wrapper class will be used
     * instead. if it is an array the args must specify the dimensions of that
     * array.
     * 
     * @param args constructor arguments
     * @return new bean instance
     */
    public static <T> T createInstance(Class<T> clazz, Object... args) {
        T instance = null;
        clazz = ObjectUtil.getDefaultImplementation(clazz);
        if (clazz.isPrimitive()) {
            clazz = PrimitiveUtil.getWrapper(clazz);
        }
        if (clazz.isArray()) {// fill the new array with given args
            // create a new multi-dim array with args as dimensions

            if (!Integer.class.isAssignableFrom(clazz.getComponentType()) && numbers(args)) {
                int[] dims = new int[args.length];
                for (int i = 0; i < dims.length; i++) {
                    dims[i] = (Integer) args[i];
                }
                instance = (T) Array.newInstance(clazz.getComponentType(), dims);
            } else {// simply fill the array
                instance = (T) Array.newInstance(clazz.getComponentType(), args.length);
                for (int i = 0; i < args.length; i++) {
                    Array.set(instance, i, ObjectUtil.wrap(args[i], clazz.getComponentType()));
                }
            }
            return instance;
        }
        if (args.length == 0) {// --> default constructor
            if (PrimitiveUtil.isPrimitiveOrWrapper(clazz)) {
                instance = PrimitiveUtil.getDefaultValue(clazz);
            } else {
                try {
                    Constructor<T> constructor = clazz.getConstructor(new Class[0]);
                    instance = Util.withAccessAquired(constructor, () -> constructor.newInstance());
                    // instance = (T) clazz.newInstance();
                } catch (final Exception e) {
                    // ok, try it on declared constructors
                    try {
                        Constructor<T> constructor = clazz.getDeclaredConstructor(new Class[0]);
                        if (constructor != null) {
                            constructor.setAccessible(true);
                            instance = constructor.newInstance();
                        }
                    } catch (final Exception e1) {
                        LOG.error("couldn't instantiate " + clazz + " with args: " + Arrays.toString(args));
                        ManagedException.forward(e1, false);
                    }
                }
            }
        } else {// searching for the right constructor
            final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (int i = 0; i < constructors.length; i++) {
                Class<?>[] pars = constructors[i].getParameterTypes();
                if (pars.length == args.length) {
                    for (int j = 0; j < args.length; j++) {
                        // if one argument type mismatches, we search for another constructor
                        if (args[j] != null && !isAssignableFrom(pars[j], args[j].getClass())) {
                            pars = null;
                            break;
                        }
                    }
                    if (pars != null) {
                        try {
                            constructors[i].setAccessible(true);
                            instance = (T) constructors[i].newInstance(args);
                            break;
                        } catch (final Exception e) {
                            ManagedException.forward(e, false);
                        }
                    }
                }
            }
            if (instance == null) {
                throw ManagedException.implementationError(
                        "BeanClass could not create the desired instance of " + clazz, Arrays.toString(args),
                        (Object[]) constructors);
            }
        }
        return instance;
    }

    private static boolean numbers(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null || !PrimitiveUtil.isAssignableFrom(Number.class, args[i].getClass()))
                return false;
        }
        return true;
    }

    /**
     * delegates to {@link #createInstance(Class, Object...)} using Threads current
     * classloader
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
        return getBeanClass(clazz);
    }

    public static String arrayClassName(String className) {
    	String first = StringUtil.substring(className, ".", "[]", true).substring(0,1);
		return "[" + (first.toLowerCase().equals(first) ? first.toUpperCase(): "L" + StringUtil.substring(className, null, "[]") + ";"); 
    }
    public static Class loadArrayClass(String className) {
    	return Util.trY( () -> Class.forName(arrayClassName(className)));
    }
    /**
     * load
     * 
     * @param className
     * @return
     */
    public static Class load(String className) {
        return load(className, null);
    }

    static Class load(String className, ClassLoader classloader) {
        return load(className, classloader, true);
    }

    /**
     * load
     * 
     * @param className   class to load
     * @param classloader (optional) if null, the threads context classloader will
     *                    be used.
     * @return loaded class
     */
    public static Class load(String clsName, ClassLoader classloader, boolean logException) {
        if (classloader == null) {
            classloader = Util.getContextClassLoader();
        }
        try {
            LOG.debug("loading class " + clsName + " through classloader " + classloader);
            return clsName.contains(".") || clsName.startsWith("[") ? classloader.loadClass(clsName)
                    : PrimitiveUtil.getPrimitiveClass(clsName);
        } catch (Exception e) {
            ManagedException.forward(e, logException);
            return null;
        }
    }

    /**
     * delegates to {@link #copyValues(Object, Object, String...)} using all
     * destination attributes having a setter.
     */
    public static <D> D copyValues(Object src, D dest, boolean onlyDestAttributes) {
        final BeanClass destClass = getBeanClass(dest.getClass(), false);
        String[] attributeNames = destClass.getAttributeNames(true);
        return copyValues(src, dest, attributeNames);
    }

    /**
     * delegates to {@link #copyValues(Object, Object, boolean, String...)} with
     * onlyIfNotNull = false.
     * 
     */
    public static <D> D copyValues(Object src, D dest, String... attributeNames) {
        return copyValues(src, dest, false, false, attributeNames);
    }

    /**
     * copy equal-named attributes from src to dest. if no attributeNames are
     * defined, all source attributes will be copied.
     * <p>
     * Warning: not performance optimized!
     * 
     * @param src              source bean
     * @param dest             destination bean (may be an extension of source bean
     *                         - or simply a bean with some equal-named attributes)
     * @param onlyIfNotNull    if true, the source values will not overwrite
     *                         existing destination-values with null.
     * @param onlyIfDestIsNull if true, the source values will not overwrite
     *                         existing destination-values.
     * @param attributeNames   (optional) fix attribute names
     * @return destination object
     */
    public static <D> D copyValues(Object src, D dest, boolean onlyIfNotNull, boolean onlyIfDestIsNull,
            String... attributeNames) {
        int copied = 0;
        if (attributeNames.length == 0) {
            final BeanClass srcClass = getBeanClass(src.getClass(), false);
            attributeNames = srcClass.getAttributeNames();
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("copying " + attributeNames.length + " attributes from " + src.getClass().getSimpleName() + " to "
                    + dest.getClass().getSimpleName());
        }
        Collection<String> unavailable = new LinkedList<String>();
        for (int i = 0; i < attributeNames.length; i++) {
            final BeanAttribute srcAttribute = BeanAttribute.getBeanAttribute(src.getClass(), attributeNames[i], false);
            if (srcAttribute == null)
                continue; // virtual attributes cannot be found through BeanAttribute.getBeanAttribute()
            BeanAttribute destAttribute = BeanAttribute.getBeanAttribute(dest.getClass(), attributeNames[i], false);
            if (destAttribute == null) {
                unavailable.add(attributeNames[i]);
                continue;
            }
            if (destAttribute.hasWriteAccess()) {
                Object value = srcAttribute.getValue(src);
                if ((!onlyIfNotNull || value != null)
                        && (!onlyIfDestIsNull || (destAttribute.getValue(dest) != null))) {
                    destAttribute.setValue(dest, value);
                    copied++;
                }
            } else {
                unavailable.add(attributeNames[i]);
            }
        }
        if (copied < attributeNames.length) {
            LOG.warn("couldn't set all values for " + dest.getClass().getSimpleName() + "! unavailable attributes: "
                    + StringUtil.toString(unavailable, 200));
        }
        return dest;
    }

    /**
     * sets all attributes from src to the value of a new created instance (mostly
     * null values). if no attributeNames are defined, all source attributes will be
     * copied.
     * <p>
     * f Warning: Works only, if src class provides a default constructor. not
     * performance optimized!
     * 
     * @param src            source bean
     * @param attributeNames (optional) fixed attribute names
     * @return reseted source object
     */
    public static <S> S resetValues(S src, String... attributeNames) {
        return copyValues(createInstance(src.getClass()), src, false);
    }

    public boolean hasField(String name) {
        try {
            return clazz.getField(name) != null;
        } catch (NoSuchFieldException | SecurityException e) {
            return false;
        }
    }

    public static Field[] fieldsOf(Class cls) {
        return fieldsOf(cls, null);
    }

    /**
     * collects recursive all fields of this class and all of its super classes
     * 
     * @param cls class to search all fields for
     * @param fields container to fill the fields into.
     * @return all fields of given class
     */
    protected static Field[] fieldsOf(Class cls, List<Field> fields) {
        if (fields == null) {
            fields = new ArrayList<Field>();
        }
        if (cls.getSuperclass() != null) {
            fieldsOf(cls.getSuperclass(), fields);
        }
        fields.addAll(Arrays.asList(cls.getDeclaredFields()));
        return fields.toArray(new Field[0]);
    }

    public static <D> D copy(Object src, D dest, String... noCopy) {
        return copy(src, dest, false, noCopy);
    }

    /**
     * deep copy (deep = true) of all fields from src to dest (if available in dest). src class should be assignable
     * from dest or vice versa.
     * <p>
     * Warning: not performance optimized! Matches fields by names without checking type!
     * 
     * @param src source bean
     * @param dest destination bean (may be an extension of source bean - or simply a bean with some equal-named fields)
     * @param noCopy (optional) field names to not be copied
     * @param deepCopy if true, all serializable values of not-final classes will be copied through serialization
     * @return destination object
     */
    public static <D> D copy(Object src, D dest, boolean deep, String... noCopy) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("copying all fields from " + src.getClass().getSimpleName()
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
            Object v;
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                if (noCopyList.contains(name)) {
                    continue;
                }
                int di = destFieldNames.indexOf(name);
                if (di != -1) {
                    if (!BitUtil.hasBit(destFields[di].getModifiers(), Modifier.FINAL, Modifier.STATIC)) {
                        fields[i].setAccessible(true);
                        destFields[di].setAccessible(true);
                        v = fields[i].get(src);
                        if (deep && v instanceof Serializable && !isFinal(v.getClass()))
                            v = ByteUtil.copy(v);
                        destFields[di].set(dest, v);
                        c++;
                    }
                }
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace(c + " fields copied");
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return dest;
    }

    public static boolean isFinal(Class<? extends Object> cls) {
        return BitUtil.hasBit(cls.getModifiers(), Modifier.FINAL);
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
     * isFinal
     * 
     * @return true, if given type is final
     */
    public boolean isFinal() {
        return Modifier.isFinal(clazz.getModifiers());
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
        while ((superClass = superClass.getSuperclass()) != null) {
            allInterfaces.addAll(Arrays.asList(superClass.getInterfaces()));
        }
        return allInterfaces.toArray(new Class[0]);
    }

    public static final <C> Class<C> getDefiningClass(C obj) {
    	return (Class<C>) getDefiningClass(obj.getClass());
    }
    /**
     * useful on anonymous/inner classes, proxies or bytecode-enhancing
     * 
     * @param cls class to analyse
     * @return defining class - means on anonymous classes, proxies or enhancing it returns the the super class or
     *         interface.
     */
    public static final <C> Class<C> getDefiningClass(Class<C> cls) {
        //TODO: how to check for enhancing class
        return (Class<C>) (cls.isEnum() 
        	? cls 
        		: Proxy.isProxyClass(cls) 
        			? cls.getInterfaces()[0]
        				: ((cls.getEnclosingClass() != null && !Modifier.isPublic(cls.getModifiers())) || cls.getSimpleName().contains("$") || cls.isSynthetic()) && cls.getSuperclass() != null 
        					? getDefiningClass(cls.getSuperclass())
        						: cls);
    }

    /**
     * checks for minimal name conventions to avoid dependency loading on non-classes like resources.
     * 
     * @param name name to check
     * @return true, if at least one package is given and class name starts with upper case - and contains letters or
     *         numbers (no enhanced or member/anonymous classes).
     */
    public static boolean isPublicClassName(String name) {
        return name.matches("([a-zA-Z0-9_]+[.])+[A-Z]\\w*[a-zA-Z0-9]$");
    }

    public static boolean isPublicClass(String name) {
    	try {
			Class cls = BeanClass.load(name, null, false);
			return Modifier.isPublic(cls.getModifiers());
		} catch (Exception e) {
			return false;
		}
    }
    public boolean isSingleton() {
    	Field f;
    	return ObjectUtil.isInstanceable(clazz) && !hasPublicConstructor() && (f = getField(clazz)) != null && Modifier.isStatic(f.getModifiers()); 
    }
    
    public boolean hasPublicConstructor() {
		return Arrays.stream(clazz.getConstructors()).anyMatch( c -> c.isAccessible());
	}

    public static boolean isStatic(Member member) {
        return Modifier.isStatic(member.getModifiers());
    }

    /**
     * getPackageName
     * 
     * @param name full class name
     * @return first part of class name
     */
    public static String getPackageName(String name) {
        return StringUtil.substring(name, null, ".", true);
    }

    /**
     * delegating to {@link CachedBeanClass#clear()}
     * 
     * @return amount of cleared objects
     */
    public static int clearCache() {
        return CachedBeanClass.clear();
    }

    public BeanMap map() {
        return new BeanMap();
    }

    @Override
    public String toString() {
        return Util.toString(this.getClass(), clazz);
    }

    public class BeanMap {
        public static final String KEY_REF = "REF";

        public T fromValueMap(Map<String, Object> values) {
            return fromValueMap(createInstanceFromValueMap(values), values);
        }

        private T createInstanceFromValueMap(Map<String, Object> values) {
            if (hasDefaultConstructor(false)) {
                return createInstance();
            } else if (values.size() == 1) {
                if (clazz.equals(Class.class) || clazz.equals(Method.class))
                    return (T) ObjectUtil.wrap(values, clazz);
            }
            return createInstance(values.values().toArray());
        }

    public T fromValueMap(Map<String, Object> values, Map<Map, Object> selfReferences) {
        Object v;
        if (values.size() == 1 && (v = values.get(KEY_REF)) != null && selfReferences.containsKey(v)) {
            return (T) selfReferences.get(v);
        }
        return fromValueMap(createInstanceFromValueMap(values), values, selfReferences);
    }

    public T fromValueMap(T instance, Map<String, Object> values) {
        return fromValueMap(instance, values, new HashMap<>());
    }

    T fromValueMap(T instance, Map<String, Object> values, Map<Map, Object> references) {
        references.put(values, instance);
        for (String name : values.keySet()) {
            final IAttribute attr = getAttribute(name, false);
            if (attr != null && attr.hasWriteAccess()) {
                final Serial serial = ASerializer.Proprietizer.serial(attr, true);
                Object value = values.get(name);
                if (Util.isEmpty(value) && attr.getValue(instance) == null)
                    continue;
                else if (serial != null && serial.formatter() != null
                        && ObjectUtil.isInstanceable(serial.formatter().getClass()) && value instanceof String) {
                    String sValue = (String) value;
                    Util.trY(() -> attr.setValue(instance, createInstance(serial.formatter()).parseObject(sValue)));
                } else if (Date.class.isAssignableFrom(typeOf(attr)) && value instanceof String
                        && ENV.get("bean.format.date.iso8601", false)) {
                    value = DateUtil.fromISO8601UTC((String) value);
                } else if (value instanceof Map && !Map.class.isAssignableFrom(typeOf(attr))) {
                    if (value == values) {// avoid stackoverflow
                        value = null;
                    } else if (references.containsKey(value)) {
                        value = references.get(value);
                    } else {
                        references.put((Map) value, null);
                        Object v = getBeanClass(typeOf(attr)).map().fromValueMap((Map) value, references);
                        references.put((Map) value, v);
                        value = v;
                    }
                } else if (value != null && !typeOf(attr).isAssignableFrom(value.getClass())) {
                    value = ObjectUtil.wrap(value, typeOf(attr));
                }
                if (attr instanceof IValueAccess)
                    ((IValueAccess) attr).setValue(value);
                else {
                    fillRecursiveList(attr, value, references);
                    attr.setValue(instance, value);
                }
            } else {
                LOG.warn("ignoring value of " + name + " - it is not an attribute of " + getClazz());
            }
        }
        return instance;
    }

    static Class typeOf(IAttribute attr) {
        Serial serial = ASerializer.Proprietizer.serial(attr, true);
        return serial != null && serial.type() != null ? serial.type() : attr.getType();
    }

    private void fillRecursiveList(IAttribute attr, Object value, Map<Map, Object> references) {
        if (!(value instanceof List))
            return;
        Class<?> serialType = ASerializer.Proprietizer.serial(attr, true).type();
        Class<?> gtype = serialType != null ? serialType : MethodUtil.getGenericType(attr.getAccessMethod(), 0);
        if (gtype == null || gtype.equals(Object.class))
            return;
        fillList(gtype, (List) value, references);
    }

    public static <T> List<T> fillList(Class<T> type, List listOfMaps) {
        return fillList(type, listOfMaps, new HashMap<>());
    }

    private static <T> List<T> fillList(Class<T> type, List listOfMaps, Map<Map, Object> references) {
        Object item;
        for (int i = 0; i < listOfMaps.size(); i++) {
            item = listOfMaps.get(i);
            if (item instanceof Map) {
                listOfMaps.set(i, references.containsKey(item) ? references.get(item)
                        : getBeanClass(type).map().fromValueMap((Map<String, Object>) item, references));
            }
        }
        return listOfMaps;
    }

    public static Map<String, Object> toValueMap(Object instance) {
        return toValueMap(instance, "", a -> true);
    }

    public static Map<String, Object> toValueMap(Object instance, String keyPrefix, Predicate<IAttribute> filter) {
        List<IAttribute> attributes = getBeanClass(instance.getClass()).getAttributes();
        final Map<String, Object> map = new LinkedHashMap<String, Object>(attributes.size());
        attributes.stream()
                .filter(filter)
                .forEachOrdered(a -> map.put(keyPrefix + a.getName(), Util.trY(() -> getValue(instance, a), false)));
        return map;
    }

    private static Object getValue(Object instance, IAttribute attr) {
        if (attr instanceof IValueAccess)
            return ((IValueAccess) attr).getValue();
        else {
            Object value = attr.getValue(instance);
            if (ENV.get("bean.format.date.iso8601", false) && value instanceof Date) {
                value = DateUtil.toISO8601UTC((Date) value);
            }
            return value;
        }
    }
}

}

/**
 * this class is used by internal caching - while other extensions of {@link BeanClass} will cache their own attribute
 * instances, this cache is intended to be used by internal helper methods like
 * {@link #copyValues(Object, Object, boolean, String...)} etc.! this helper methods wont change the set of attributes!
 * 
 * @param <T> class defining the bean
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
class CachedBeanClass<T> extends BeanClass<T> {
    private static final Log LOG = LogFactory.getLog(CachedBeanClass.class);
    /** serialVersionUID */
    private static final long serialVersionUID = -7034920800778017069L;

    /** cached bean attributes */
    private transient List<IAttribute> attributes;
    /**
     * stores the last call on {@link #getAttributes(boolean)}. if the next call differs, the list will be rebuilt. *
     */
    private transient boolean readAndWriteAttributes;

    /** on each call to {@link #getAnnotation(Class)} */
    private transient Map<Class<? extends Annotation>, Collection<BeanAttribute>> annotatedAttributes;

    /** cache to avoid multiple calls on Class.getMethods() and creation of BeanAttribute instances */
    protected transient static final Map<Class, BeanClass> bcCache = new Hashtable<Class, BeanClass>();

    protected CachedBeanClass(Class<T> beanClass) {
        super(beanClass);
    }

    @SuppressWarnings({ "unchecked" })
    static final <C> BeanClass<C> getCachedBeanClass(Class<C> beanClass) {
        BeanClass bc = bcCache.get(beanClass);
        if (bc == null) {
            bc = new CachedBeanClass(beanClass);
            bcCache.put(beanClass, bc);
            if (LOG.isDebugEnabled()) {
                if (bcCache.size() % 1000 == 0) {
                    LOG.debug("internal beanclass cache has " + bcCache.size() + " elements");
                }
            }
        }
        return bc;
    }

    @Override
    public List<IAttribute> getAttributes(boolean readAndWriteAccess) {
        if (attributes == null || this.readAndWriteAttributes != readAndWriteAccess) {
            attributes = super.getAttributes(this.readAndWriteAttributes = readAndWriteAccess);
        }
        return new ArrayList<IAttribute>(attributes);
    }

    @Override
    public Collection<BeanAttribute> findAttributes(Class<? extends Annotation> annotationType) {
        if (annotatedAttributes == null)
            annotatedAttributes = new HashMap<Class<? extends Annotation>, Collection<BeanAttribute>>();
        Collection<BeanAttribute> result;
        if ((result = annotatedAttributes.get(annotationType)) == null) {
            result = super.findAttributes(annotationType);
            annotatedAttributes.put(annotationType, result);
        }
        return result;
    }

    /**
     * clear cache
     */
    static final int clear() {
        int cleared = bcCache.size();
        LOG.info("clearing beanclass cache of " + cleared + " elements");
        bcCache.clear();
        return cleared;
    }
}
