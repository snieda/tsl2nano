/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 03.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.parser.JSon;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public class ObjectUtil extends MethodUtil {
    private static final Log LOG = LogFactory.getLog(ObjectUtil.class);
    private static final List<String> STD_TYPE_PKGS;

    static {
        STD_TYPE_PKGS = new ArrayList<String>(5);
        STD_TYPE_PKGS.add("sun.management");
        STD_TYPE_PKGS.add("java.lang");
        STD_TYPE_PKGS.add("java.util");
        STD_TYPE_PKGS.add("java.math");
        STD_TYPE_PKGS.add("java.sql");
    };

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Map<Class, Class> STD_IMPLEMENTATIONS = MapUtil.asMap(
    		Number.class, Double.class,
			Collection.class, LinkedList.class,
			Iterable.class, LinkedList.class,
			List.class, LinkedList.class,
			Set.class, LinkedHashSet.class,
			SortedSet.class, TreeSet.class,
			Map.class, HashMap.class,
			SortedMap.class, TreeMap.class,
			CharSequence.class, String.class);

    /**
     * delegates to {@link BeanClass#copy(Object, Object)}.
     * <p/>
     * to copy only not-null values, use {@link #addValues(Object, Object, String...)}.
     */
    public static <D> D copy(Object src, D dest, String... noCopy) {
        return BeanClass.copy(src, dest, noCopy);
    }

    /**
     * delegates to {@link BeanClass#copyValues(Object, Object, String...)}.
     * <p/>
     * to copy only not-null values, use {@link #addValues(Object, Object, String...)}.
     */
    public static <D> D copyValues(Object src, D dest, String... attributeNames) {
        return BeanClass.copyValues(src, dest, false, false, attributeNames);
    }

    /**
     * copies all not-null values to dest. if overwrite is true, existing dest values will be overwritten.
     * <p/>
     * delegates to {@link BeanClass#copyValues(Object, Object, String...)}.
     */
    public static <D> D merge(Object src, D dest, boolean overwrite, String... attributeNames) {
        return BeanClass.copyValues(src, dest, true, !overwrite, attributeNames);
    }

    /**
     * delegates to {@link BeanClass#copyValues(Object, Object, boolean)}.
     * <p/>
     * to copy only not-null values, use {@link #addValues(Object, Object, String...)}.
     */
    public static <D> D copyValues(Object src, D dest, boolean destValuesOnly) {
        return BeanClass.copyValues(src, dest, destValuesOnly);
    }


    /**
     * delegates to {@link BeanClass#resetValues(Object, String...)}.
     */
    public static <S> S resetValues(S src) {
        return BeanClass.resetValues(src);
    }

    /**
     * delegates to {@link #copyValues(Object, Object, String...)}, creating a new instance and copying all values (no
     * deep copy! see {@link #copy(Object)}).
     */
    public static <T> T clone(T src) {
        try {
            return (T) BeanClass.copyValues(src, BeanClass.createInstance(src.getClass()));
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * Call this method only, if you want to extend the framework to use specific data extensions! The standard type
     * packages list (see {@link #STD_TYPE_PKGS} is only used by {@link #isStandardType(Class)}. E.g., if you extend and
     * use your own type my.Date as extension of java.util.Date, you would add 'my' to the standard type packages.
     * 
     * @param stdTypePackage package path to add - containg standard type extensions.
     */
    public static void addStandardTypePackages(String stdTypePackage) {
        STD_TYPE_PKGS.add(stdTypePackage);
    }

    /**
     * @see #isStandardType(Class) evaluating the given objects class
     */
    public static boolean isStandardType(Object object) {
        return object instanceof Class ? isStandardType((Class) object) : object != null ? isStandardType(object
            .getClass())
            : false;
    }

    /**
     * evaluates, if the given type is a basic data type like String, Date, Time, Number. If you have own, specific data
     * implementations, you are able to add their packages through {@link #addStandardTypePackages(String)} - but be
     * careful, this change will be used by the framework! Please see {@link #isStandardInterface(Class)}, too.
     * 
     * @param type class to analyze
     * @return true, if type is a 'java.lang' or 'java.util' class.
     */
    public static boolean isStandardType(Class<?> type) {
        //if type is root object, it will be an special extension - not a standard type
        //TODO: whats about interfaces like comparable - see isStandardInterface()?
        if (type.getName().equals(Object.class.getName())) {
            return false;
        }
        //on array types, the package is null!
        String p = type.getPackage() != null ? type.getPackage().getName() : type.getClass().getName();
        p = StringUtil.extract(p, "\\w+[.]\\w+");
        return type.isPrimitive() || STD_TYPE_PKGS.contains(p);
    }

    /**
     * evaluates, if the given type is a basic interface like Comparable, Clonable, etc. If you have own, specific data
     * implementations, you are able to add their packages through {@link #addStandardTypePackages(String)} - but be
     * careful, this change will be used by the framework! Please see {@link #isStandardType(Class)}, too.
     * 
     * @param type class to analyze
     * @return true, if type is a 'java.lang' or 'java.util' interface.
     */
    public static boolean isStandardInterface(Class<?> type) {
        //if type is root object, it will be an special extension - not a standard type
        //on array types, the package is null!
        String p = type.getPackage() != null ? type.getPackage().getName() : type.getClass().getName();
        p = StringUtil.extract(p, "\\w+[.]\\w+");
        return type.isInterface() && STD_TYPE_PKGS.contains(p);
    }

    /**
     * isSingleValueType
     * 
     * @param type class to analyze
     * @return true, if type is not a map, collection or array.
     */
    public static boolean isSingleValueType(Class<?> type) {
        return !(type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)
            || isByteStream(type));
    }

    public static boolean isAbstract(Class<?> type) {
        return Modifier.isAbstract(type.getModifiers());
    }

    /**
     * if all fields are null, the bean is empty
     * 
     * @param bean instance
     * @param filterAttributes attributes to ignore
     * @return true, if all attributes are null.
     */
    public static boolean isEmpty(Object bean, String... filterAttributes) {
        final BeanClass bc = BeanClass.getBeanClass(bean.getClass());
        final Collection<BeanAttribute> attributes = bc.getAttributes();
        final Collection ignore = Arrays.asList(filterAttributes);
        for (final BeanAttribute beanAttribute : attributes) {
            if (beanAttribute.getValue(bean) != null && !ignore.contains(beanAttribute.getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * evaluates the type name of the given instance
     * 
     * @param instance instance or class
     * @return simple class name or "null"
     */
    public static String getName(Object instance) {
        return instance instanceof Class ? BeanClass.getName((Class) instance) : instance != null ? BeanClass
            .getName(instance.getClass()) : "null";
    }

    /**
     * looks for the given interface in the hierarchy of the given object type and tries to get the generic type for
     * that interface.
     * 
     * @param cls object type
     * @param interfaze interface to search a generic type for (if null, getGenericSuperClass() will be used)
     * @return generic type for interfaze
     */
    public static Class<?> getGenericInterfaceType(Class cls, Class interfaze, int pos) {
        Type[] interfaces = cls.getGenericInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaze.isAssignableFrom(getGenericInterface(interfaces[i])))
                return getGeneric(interfaces[i], pos);
        }
        if (cls.getGenericSuperclass() != null)//TODO: leider gehen hier die generic-infos verloren
            return ((Class) getGenericInterfaceType(
                (Class) ((ParameterizedType) cls.getGenericSuperclass()).getRawType(), interfaze, pos));
        throw new IllegalArgumentException("the given class " + cls + " has no generic interface: " + interfaze);
    }

    protected static Class<?> getGenericInterface(Type type) {
        return (Class<?>) (ParameterizedType.class.isAssignableFrom(type.getClass())
            ? ((ParameterizedType) type).getRawType() : type);
    }

    /**
     * getClass
     * 
     * @param genericType
     * @return
     */
    protected static Class<?> getGeneric(Type genericType, int pos) {
        Type type = ((ParameterizedType) genericType).getActualTypeArguments()[pos];
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof TypeVariable) {
            return (Class<?>) ((TypeVariable) type).getGenericDeclaration().getTypeParameters()[0]
                .getGenericDeclaration();
        }
        return type instanceof Class ? (Class<?>) type : null;
    }

    /**
     * getGenericType
     * 
     * @param clazz class of field
     * @param fieldName field name
     * @return first generic type of given field
     */
    public static Class<?> getGenericType(Class<?> clazz, String fieldName) {
        try {
            return (Class<?>) ((ParameterizedType) clazz.getDeclaredField(fieldName).getGenericType())
                .getActualTypeArguments()[0];
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public static Class<?> getGenericType(Class<?> clazz) {
        return getGenericType(clazz, 0);
    }

    /**
     * @deprecated: use {@link #getGenericInterfaceType(Object, Class)} instead getGenericType
     * 
     * @param clazz class of field
     * @return first generic type of given class - or null
     * @throws ClassCastException, if type arguments not castable to Class
     */
    public static Class<?> getGenericType(Class<?> clazz, int pos) {
        try {
            Type genericType = clazz.getGenericSuperclass();
            //try to get the type through the first defined generic interface
            if (genericType == null) {
                if (clazz.getGenericInterfaces().length > pos) {
                    genericType = clazz.getGenericInterfaces()[pos];
                } else {
                    return null;
                }
            }
            return getGeneric(genericType, pos);
        } catch (Exception e) {
            LOG.warn(e.toString());
            return null;
        }
    }

    /**
     * creates a default instance of the given primitive
     * 
     * @param <T> primitive type
     * @param standardType primitive type
     * @return instanceof primitives immutable
     */
    public static <T> T getDefaultValue(Class<T> standardType) {
        return PrimitiveUtil.getDefaultValue(standardType);
    }

    /**
     * calls the internal {@link Object#clone()} method. Throws a CloneNotSupportedException, if src class does not implement Cloneable!
     * 
     * @param src source to copy
     * @return copied object
     */
    public static <T> T cloneObject(T src) {
        return (T) new PrivateAccessor<T>(src).call("clone", null);
    }

    // TODO: old style -> encapsulate into a pool of formatters/wrappers/converters
    /**
     * sometimes the value is easily convertable to the desired type, like String-->File etc. respects primitives and
     * wrapperType {@link Class}.
     * 
     * @param value to be wrapped into an instance of wrapperType.
     * @param wType having a constructor with parameter value.getClass()
     * @return wrapped instance or value itself
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(Object value, Class<T> wrapperType) {
        if (value == null || (value != null && wrapperType.isAssignableFrom(value.getClass())))
            return (T) value;
        LOG.debug("trying to convert '" + value + "' to " + wrapperType);
        // check, if constructor for value is available in wrapper type
        try {
            if (value != null && !PrimitiveUtil.isAssignableFrom(wrapperType, value.getClass())) {
                final Class<T> wType = wrapperType.isInterface() || isAbstract(wrapperType)
                        ? getDefaultImplementation(wrapperType)
                        : wrapperType;

                if (Class.class.isAssignableFrom(wType)) {
                    return (T) BeanClass.load(
                            StringUtil.subRegex(value.toString(), "(class |name\"?\\s*[=:]\s*\"?)", "(@|\"|\\})", 0,
                                    true, false));
                    // StringUtil.substring(
                    //         StringUtil.substring(StringUtil.substring(
                    //                 StringUtil.substring(value.toString(), "class ", "@"), "name\":", "}"),
                    //                 "name=", "}"),
                    //         "\"", "\""));
                } else if (Method.class.isAssignableFrom(wType)) {
                    return (T) MethodUtil.fromGenericString(
                            StringUtil.subRegex(value.toString(), "(method |name\"?\\s*[=:]\s*\"?)", "(@|\"|\\})", 0,
                                    true, false));
                    // StringUtil.substring(StringUtil.substring(
                    //         StringUtil.substring(StringUtil.substring(value.toString(), "method ", "@"),
                    //                 "name\":",
                    //                 "\"}"),
                    //         "name=", "}"),
                    //         "\"", "\""));
                } else {
                    if (value instanceof CharSequence && CharSequence.class.isAssignableFrom(wType))
                        return String.class.isAssignableFrom(wType) ? (T) value.toString()
                                : BeanClass.createInstance(wType, value);
                    if (PrimitiveUtil.isPrimitiveOrWrapper(wType))
                        return PrimitiveUtil.convert(value, wType);
                    else if (ByteUtil.isByteStream(wType))
                        return ByteUtil.toByteStream(ByteUtil.getBytes(value), wType);
                    else if (Collection.class.isAssignableFrom(wType))
                        return (T) (value instanceof Collection ? new ListSet((Collection)value) : new ListSet(value));
                    else if (value instanceof Collection && wType.isArray())
                        if (wType.getComponentType().isPrimitive())
                            // return (T) ((Collection) value).stream().map(i -> wrap(i, wType.getComponentType())).toArray();
                            return (T) PrimitiveUtil.toArray(((Collection) value).stream(), wType.getComponentType(),
                                    ((Collection) value).size());
                        else
                            return (T) ((Collection) value)
                                    .toArray((Object[]) Array.newInstance(wType.getComponentType(), 0));
                    else if (value instanceof Map && (wType.isInterface() || wType.equals(Properties.class))
                            && Map.class.isAssignableFrom(wType)) {
                        return (T) MapUtil.toMapType((Map) value, (Class<Map>) wType);
                    } else if (wType.isArray() && value instanceof String) {
                        if (JSon.isJSon(value.toString()))
                            return (T) new JSon().toArray(wType.getComponentType(), value.toString());
                        else
                            return (T) MapUtil.asArray(wType.getComponentType(), (String) value);
                    } else if (wType.isArray()
                            && PrimitiveUtil.isAssignableFrom(wType.getComponentType(), value.getClass())) {
                        return (T) MapUtil.asArray(wType.getComponentType(), value);
                    } else if (wType.isArray() && isInstanceable(wType.getComponentType())) {
                        return (T) MapUtil.asArray(wType.getComponentType(),
                                BeanClass.createInstance(wType.getComponentType(), value));
                    } else if (value instanceof String && JSon.isJSon((String) value)) {
                        if (Map.class.isAssignableFrom(wType)) {
                            Map jsonMap = MapUtil.fromJSon((String)value);
                            return (T) (wType.isInterface() ? MapUtil.toMapType(jsonMap, (Class<Map>) wType) : jsonMap);
                        } else if (Collection.class.isAssignableFrom(wType)) {
                            return (T) new JSon().toList(
                                    ObjectUtil.getGenericInterfaceType(wType, Collection.class, 0),
                                    (String) value);
                        } else if (wType.isInterface()) {
                            return (T) AdapterProxy.create(wType, MapUtil.fromJSon((String) value));
                        } else {
                            new JSon().toObject(wType, (String) value);
                        }
                    } else if (wType.isEnum() && value instanceof Number)
                        return (T) wType.getEnumConstants()[((Number) value).intValue()];
                    else if (value instanceof String && isSimpleType(wType))
                        return FormatUtil.parse(wType, (String) value);
                    else if (String.class.isAssignableFrom(wType) && isSimpleType(value.getClass()))
						return (T) FormatUtil.getDefaultFormat(value, true).format(value);
                    else if (hasValueOfMethod(wType, value))
                        return (T) BeanClass.call(wType, "valueOf", true, value);
                    else if (wType.isInterface() && value instanceof Map)
                        return (T) AdapterProxy.create(wType, (Map) value);
                    else if (isInstanceable(wType)/* && BeanClass.hasConstructor(wrapperType, value.getClass())*/)
						try {
                            return BeanClass.createInstance(wType, value);
						} catch (Exception e) {
                            return BeanClass.createInstance(wType, value.toString());
						}
					else
                        LOG.warn("unknown wrapping of " + value.getClass() + value.hashCode() + " to " + wType);
                }
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public static Object fromListOfWrappers(Class primitiveType, List list) {
        Object array = Array.newInstance(primitiveType, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, wrap(list.get(i), primitiveType));
        }
        return array;
    }

    @SuppressWarnings("unchecked")
	public static <T> Class<T> getDefaultImplementation(Class<T> wrapperType) {
		return STD_IMPLEMENTATIONS.containsKey(wrapperType) ? STD_IMPLEMENTATIONS.get(wrapperType) : wrapperType;
	}

	private static boolean hasValueOfMethod(Class<?> wrapperType, Object value) {
		try {
			return wrapperType.getDeclaredMethod("valueOf", new Class[] {PrimitiveUtil.getPrimitive(value.getClass())}) != null;
		} catch (NoSuchMethodException | SecurityException e) {
			//ok, no problem
			return false;
		}
	}

	/**
     * wraps (see {@link #wrap(Object, Class)}) the given value through the castInfo information to the desired cast.
     * 
     * @param value to wrap into an object defined by castInfo
     * @param castInfo (optional) any text or name containing a part with: (<classpath>)
     * @return if castInfo with cast to class was found: the wrapped (see {@link #wrap(Object, Class)}) value, otherwise
     *         the value itself
     */
    @SuppressWarnings("unchecked")
    public static Object cast(Object value, String castInfo) {
        String cast = castInfo != null ? StringUtil.substring(castInfo, "(", ")", false, true) : null;
        return cast != null ? wrap(value, BeanClass.createBeanClass(cast).getClazz()) : value;
    }

    enum ObjectMethods {
    	TOSTRING("toString"),
    	HASHCODE("hashCode"),
    	EQUALS("equals", Object.class);
//    	CLONE("clone"),
//    	FINALIZE("finalize");
    	
    	Method method;
    	ObjectMethods(String methodName, Class...args) {
    		method = Util.trY(() -> Object.class.getMethod(methodName, args));
    	}
    	Method method() {
    		return method;
    	}
    }

    public static boolean hasToString(Object obj) {
        return obj != null && hasToString(obj.getClass());
    }

    /**
     * checks, whether the class of the given object implements 'toString()' itself.
     * 
     * @param obj instance of class to evaluate
     * @return true, if class of object overrides toString()
     */
    public static boolean hasToString(Class cls) {
    	return isOverridden(cls, ObjectMethods.TOSTRING);
    }
    public static boolean hasEquals(Class cls) {
    	return isOverridden(cls, ObjectMethods.EQUALS);
    }
    public static boolean hasHashcode(Class cls) {
    	return isOverridden(cls, ObjectMethods.HASHCODE);
    }
    public static boolean isOverridden(Class cls, ObjectMethods objMethod) {
        try {
            if (cls.isPrimitive() || cls.isInterface()) {
                return false;
            }
            final Method method = cls.getMethod(objMethod.method().getName(), objMethod.method().getParameterTypes());
            //pure objects, representating their instance id
            return !method.toString().equals(objMethod.method().toString());
        } catch (final Exception e) {
            ManagedException.forward(e);
            return false;
        }
    }

	@SuppressWarnings("unchecked")
	public static <T> T createDefaultInstance(Class<T> gtype) {
        if (isStandardType(gtype)) {
            if (BeanClass.hasDefaultConstructor(gtype)) {
                return BeanClass.createInstance(gtype);
            } else if (NumberUtil.isNumber(gtype)) {
                return (T) NumberUtil.getDefaultInstance((Class<Number>) gtype);
            } else if (Time.class.isAssignableFrom(gtype)) {
            	return (T) new Time(System.currentTimeMillis());
            } else if (Timestamp.class.isAssignableFrom(gtype)) {
            	return (T) new Timestamp(System.currentTimeMillis());
            }
        }
		return null;
	}

    public static Class<?> loadClass(String clsName) {
        return loadClass(clsName, Thread.currentThread().getContextClassLoader());
    }

    public static Class<?> loadClass(String clsName, ClassLoader loader) {
		try {
			return clsName.contains(".") || clsName.startsWith("[") ? loader.getClass().forName(clsName)
			        : PrimitiveUtil.getPrimitiveClass(clsName);
		} catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
		}
	}

    public static boolean isObject(Object o) {
        return o != null && !o.getClass().isArray() && !o.getClass().isPrimitive();
    }
}
