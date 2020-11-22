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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.log.LogFactory;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public class ObjectUtil extends ByteUtil {
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
        return type.isInterface() || STD_TYPE_PKGS.contains(p);
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
     * @param interfaze interface to search a generic type for.
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

    /**
     * @deprecated: use {@link #getGenericInterfaceType(Object, Class)} instead getGenericType
     * 
     * @param clazz class of field
     * @return first generic type of given class - or null
     * @throws ClassCastException, if type arguments not castable to Class
     */
    public static Class<?> getGenericType(Class<?> clazz) {
        try {
            Type genericType = clazz.getGenericSuperclass();
            //try to get the type through the first defined generic interface
            if (genericType == null) {
                if (clazz.getGenericInterfaces().length > 0) {
                    genericType = clazz.getGenericInterfaces()[0];
                } else {
                    return null;
                }
            }
            return getGeneric(genericType, 0);
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

    /**
     * sometimes the value is easily convertable to the desired type, like String-->File etc. respects primitives and
     * wrapperType {@link Class}.
     * 
     * @param value to be wrapped into an instance of wrapperType.
     * @param wrapperType having a constructor with parameter value.getClass()
     * @return wrapped instance or value itself
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(Object value, Class<T> wrapperType) {
        if (value != null && wrapperType.isAssignableFrom(value.getClass()))
            return (T) value;
        LOG.debug("trying to convert '" + value + "' to " + wrapperType);
        // check, if constructor for value is available in wrapper type
        try {
            if (value != null && !PrimitiveUtil.isAssignableFrom(wrapperType, value.getClass())) {
                if (Class.class.isAssignableFrom(wrapperType))
                    return (T) BeanClass.load(StringUtil.substring(value.toString(), "class ", "@"));
                else {
                    if (PrimitiveUtil.isPrimitiveOrWrapper(wrapperType))
                        return PrimitiveUtil.convert(value, wrapperType);
                    else if (ByteUtil.isByteStream(wrapperType))
                        return ByteUtil.toByteStream((byte[])value, wrapperType);
                    else if (Collection.class.isAssignableFrom(wrapperType))
                        return (T )new ListSet(value);
                    else if ((wrapperType.isInterface() || wrapperType.equals(Properties.class)) 
                            && Map.class.isAssignableFrom(wrapperType))
                        return (T) MapUtil.toMapType((Map)value, (Class<Map>)wrapperType);
                    else if (hasValueOfMethod(wrapperType, value))
                    	return (T) BeanClass.call(wrapperType, "valueOf", false, value);
                    else if (isInstanceable(wrapperType))
                        //IMPROVE: what's about FormatUtil.parse() <-- ObjectUtil.wrap() is called in FormatUtil!
                        return BeanClass.createInstance(wrapperType, value);
                    else
                        LOG.warn("unknown wrapping of " + value.getClass() + value.hashCode() + " to " + wrapperType);
                }
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return (T) value;
    }

    private static boolean hasValueOfMethod(Class<?> wrapperType, Object value) {
		try {
			return wrapperType.getDeclaredMethod("valueOf", new Class[] {value.getClass()}) != null;
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


    private static String OBJ_TOSTRING;
    static {
        try {
            OBJ_TOSTRING = Object.class.getMethod("toString", new Class[0]).toString();
        } catch (final Exception e) {
            ManagedException.forward(e);
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
        try {
            if (cls.isInterface()) {
                return false;
            }
            final Method method = cls.getMethod("toString", new Class[0]);
            //pure objects, representating there instance id
            return !method.toString().equals(OBJ_TOSTRING);
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

	public static Object loadClass(String clsName) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try {
			return clsName.contains(".") || clsName.startsWith("[") ? loader.getClass().forName(clsName)
			        : PrimitiveUtil.getPrimitiveClass(clsName);
		} catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
		}
	}
}
