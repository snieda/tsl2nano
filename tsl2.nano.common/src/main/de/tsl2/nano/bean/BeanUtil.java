/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.bean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * A Utility-Class for beans
 * 
 * @author ts 05.03.2009
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BeanUtil extends ByteUtil {
    private static final Log LOG = LogFactory.getLog(BeanUtil.class);
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
     * as BeanUtil.copyProperties(..) does only a shallow copy, this method does a deep recursive copy.
     * <p/>
     * uses lamdas of jdk 1.8. handles: iterable, enum, number - not handled yet: map, array
     */
/*    @SuppressWarnings({ "rawtypes", "unchecked" })
private static Object deepCopy(Object src, Object dest) throws Exception {
  BeanUtils.copyProperties(src, dest);
  PropertyDescriptor[] pdSrc = BeanUtils.getPropertyDescriptors(src.getClass());
  PropertyDescriptor pdDest;
  String name;
  Object vold, vnew;
  Class type;
  for (int i = 0; i < pdSrc.length; i++) {
    name = pdSrc[i].getName();
    vold = null;
    vnew = null;
    pdDest = BeanUtils.getPropertyDescriptor(dest.getClass(), name);
    if (pdDest != null && pdSrc[i].getReadMethod() != null && (vold = pdSrc[i].getReadMethod().invoke(src)) != null
        && (pdDest.getReadMethod() == null || (vnew = pdDest.getReadMethod().invoke(dest)) == null)) {
      type = pdDest.getReadMethod().getReturnType();
      //create instance of enum, number or entity
      vnew = type.isEnum() ? Enum.valueOf((Class) type, ((Enum) vold).name())
          : Number.class.isAssignableFrom(type) ? type.getConstructor(String.class).newInstance(vold.toString()) : type.newInstance();
      //set the new value and go into recursion
      pdDest.getWriteMethod().invoke(dest, deepCopy(vold, vnew));
    } else if (vold != null && Iterable.class.isAssignableFrom(type = pdDest.getReadMethod().getReturnType())) {
      //handle collections with recursion
      Collection it = (Collection) vold;
      Collection cnew = (Collection) vnew;
      Object genType = pdDest.getReadMethod().getGenericReturnType();
      if (genType instanceof ParameterizedType) {
        genType = ((ParameterizedType) genType).getActualTypeArguments()[0];
      }
      if (genType instanceof Class) {
        Class gtype = (Class) genType;
        boolean javaType = gtype.getPackage().getName().startsWith("java");
        it.forEach((Object o) -> {
          try {
            cnew.add(javaType ? o : deepCopy(o, gtype.newInstance()));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
      }
    }
  }
  return dest;
}
*/
    /**
     * creates new objects, cloned from defaultClone and with increasing attribute value - evaluated by first+count+step.
     * @param defaultClone
     * @param attributeName
     * @param first first attribute value - defaultClone can contain the value or this must not be null
     * @param count number of objects to create
     * @param step step of increase
     * @return list of new created objects
     */
    public static <S>  List<S> create(S defaultClone, String attributeName, Object first, int count, double step) {
        Object clone, value;
        ArrayList result = new ArrayList();
        if (first == null) {
            first = Bean.getBean(defaultClone).getAttribute(attributeName).getValue();
        }
        long start = NumberUtil.toNumber(first);
        long end = start + count;
        for (long i = start; i < end; i+=step) {
            clone = clone(defaultClone);
            value = NumberUtil.fromNumber(i, first.getClass());
            Bean.getBean(clone).getAttribute(attributeName).setValue(value);
            result.add(clone);
        }
        return result;
    }
    
    /**
     * wraps all attributes having a collection as value into a new {@link ListSet} instance to unbind a
     * {@link #clone()} instance.
     * 
     * @param src instance to wrap the attribute values for
     * @return the instance itself
     */
    public static <S> S createOwnCollectionInstances(S src) {
        BeanClass<S> bc = (BeanClass<S>) BeanClass.getBeanClass(src.getClass());
        List<IAttribute> attributes = bc.getAttributes();
        for (IAttribute a : attributes) {
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
     * calls the internal {@link Object#clone()} method.
     * 
     * @param src source to copy
     * @return copied object
     */
    public static <T> T cloneObject(T src) {
        return (T) new PrivateAccessor<T>(src).call("clone", null);
    }

    /**
     * calls a method through reflection
     * 
     * @param clazz class to load
     * @param method method to call
     * @param args optional arguments
     * @return result of method calling
     */
    public static Object call(String clazz, String method, Object... args) {
        return BeanClass.createBeanClass(clazz).callMethod(null, method, null, args);
    }

    /**
     * calls {@link #call(String, String, Object...)} to start any java method
     * 
     * @param args
     */
    public static final void main(String args[]) {
        System.out.println("calling BeanUtil.main");
        String clazz = args[0];
        String method = args[1];
        Object[] objs = CollectionUtil.copyOfRange(args, 2, args.length, Object[].class);
        call(clazz, method, objs);
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
     * fills a map with all bean-attribute-names and their values
     * 
     * @param o bean
     * @return see {@link Bean#toValueMap()}
     */
    public static Map<String, Object> toValueMap(Object o) {
        return toValueMap(o, false, false, false);
    }

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @param o bean
     * @param useClassPrefix if true, the class-name will be used as prefix for the key
     * @param onlySingleValues if true, collections will be ignored
     * @param onlyFilterAttributes if true, all other than filterAttributes will be ignored
     * @param filterAttributes attributes to be filtered (ignored, if onlyFilterAttributes)
     * @return see {@link Bean#toValueMap()}
     */
    public static Map<String, Object> toValueMap(Object o,
            boolean useClassPrefix,
            boolean onlySingleValues,
            boolean onlyFilterAttributes,
            String... filterAttributes) {
        BeanDefinition beandef;
//        if (onlyFilterAttributes && filterAttributes.length > 0) {
//            //attributes will be changed - so we have to use an own instance
//            beandef = new BeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//            beandef.setAttributeFilter(filterAttributes);
//        } else {
        beandef = BeanDefinition.getBeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//        }
        return beandef.toValueMap(o, useClassPrefix, onlySingleValues, onlyFilterAttributes, filterAttributes);
    }

    /**
     * delegates to {@link #toValueMap(Object, String, boolean, boolean, String...)} with onlyFilteredAttributes = true.
     */
    public static Map<String, Object> toValueMap(Object o,
            String keyPrefix,
            boolean onlySingleValues,
            String... filterAttributes) {
        return toValueMap(o, keyPrefix, onlySingleValues, true, filterAttributes);
    }

    /**
     * fills a map with all bean-attribute-names and their values
     * 
     * @param o java instance
     * @param keyPrefix key prefix to be used for each attribute name. must not be null - use an empty string instead!
     * @param onlySingleValues if true, collections will be ignored
     * @param onlyFilteredAttributes if true, only the filterAttributes will be filled - otherwise, all others will be
     *            filled.
     * @param filterAttributes attributes to be filtered
     * @return see {@link Bean#toValueMap()}
     */
    public static Map<String, Object> toValueMap(Object o,
            String keyPrefix,
            boolean onlySingleValues,
            boolean onlyFilteredAttributes,
            String... filterAttributes) {
        BeanDefinition beandef = BeanDefinition.getBeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//        if (onlyFilteredAttributes && filterAttributes.length > 0) {
//            //attributes will be changed - so we have to use an own instance
//            beandef = new BeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//            beandef.setAttributeFilter(filterAttributes);
//        } else {
//            beandef = BeanDefinition.getBeanDefinition(BeanClass.getDefiningClass(o.getClass()));
//        }
        return beandef.toValueMap(o, keyPrefix, onlySingleValues, onlyFilteredAttributes, filterAttributes);
    }

    /**
     * delegates to {@link #toFormattedMap(Object, String, boolean)}.
     */
    public static Map<String, Object> toFormattedMap(Object bean) {
        return toFormattedMap(bean, null, true, new DefaultFormat());
    }

    /**
     * provides a map containing all formatted single value attributes of the given bean
     * 
     * @param bean bean to evaluate
     * @param keyPrefix (optional) key name prefix (normally ending with a dot)
     * @param translateKeys if true, all keys will be translated
     * @param filterAttributes attributes to be filtered (ignored)
     * @param format formatter (use {@link DefaultFormat} if unknown)
     * @return map containing formatted values
     */
    public static Map<String, Object> toFormattedMap(Object bean,
            String keyPrefix,
            boolean translateKeys,
            Format format,
            String... filterAttributes) {
        final Map<String, Object> valueMap = keyPrefix != null ? BeanUtil.toValueMap(bean,
            keyPrefix,
            true,
            filterAttributes) : BeanUtil.toValueMap(bean, true, true, false, filterAttributes);
        final Set<String> keySet = valueMap.keySet();
        final Map<String, Object> formattedMap = new LinkedHashMap<String, Object>();
        for (final String k : keySet) {
            final String key = translateKeys ? Messages.getString(k) : k;
            formattedMap.put(key, format.format(valueMap.get(k)));
        }
        return formattedMap;
    }

    /**
     * convenience for {@link #fromFlatFile(Reader, Class, String...)}.
     */
    public static <T> Collection<T> fromFlatFile(String fileName, Class<T> rootType, String... attributeNames) {
        try {
            return fromFlatFile(new BufferedReader(new FileReader(new File(fileName))), rootType, null, attributeNames);
        } catch (final FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * convenience for {@link #fromFlatFile(Reader, Class, String...)}.
     */
    public static <T> Collection<T> fromFlatFile(String fileName,
            String separation,
            Class<T> rootType,
            String... attributeNames) {
        try {
            return fromFlatFile(new BufferedReader(new FileReader(new File(fileName))),
                separation,
                rootType,
                null,
                attributeNames);
        } catch (final FileNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * reads a flat (like csv) file and tries to put the values to the given bean type. the given bean type must have a
     * default constructor.
     * 
     * @param <T>
     * @param r normally a buffered reader.
     * @param rootType root bean type to be instantiated and filled to the result collection
     * @param attributeNames (optional) simple attribute names or point-separated relation expressions. use null to
     *            ignore the token
     * @return filled collection of beans
     */
    public static <T> Collection<T> fromFlatFile(Reader r,
            Class<T> rootType,
            Map<String, Format> formats,
            String... attributeNames) {
        return fromFlatFile(r, null, Bean.newBean(rootType), formats, attributeNames);
    }

    /**
     * reads a flat (like csv) file and tries to put the values to the given bean type. the given bean type must have a
     * default constructor.
     * 
     * @param <T>
     * @param r normally a buffered reader.
     * @param separation separation character. if this is null, the attributeNames must contain at least one
     *            column-index.
     * @param rootType root bean type to be instantiated and filled to the result collection
     * @param attributeNames (optional) simple attribute names or point-separated relation expressions. use null to
     *            ignore the token
     * @return filled collection of beans
     */
    public static <T> Collection<T> fromFlatFile(Reader r,
            String separation,
            Class<T> rootType,
            Map<String, Format> formats,
            String... attributeNames) {
        return fromFlatFile(r, separation, Bean.newBean(rootType), formats, attributeNames);
    }

    /**
     * reads a flat file (like csv) and tries to put the values to the given bean type. the given bean type should have
     * a default constructor. the given bean holds an example instance of your root-type. it is possible to provide an
     * overridden bean, to implement the method {@link Bean#newInstance(Object...)} to initialize your desired instance.
     * a new instance will be created on each new line.
     * </p>
     * there are two possibilities to use this method:</br>
     * - with a field separator (like comma or semicolon)</br>
     * - with line-column definitions (like '1-10:myBeanAttributeName)
     * </p>
     * 
     * with the first alternative, you give a separator (not null) and the pure attribute names of the given rootType
     * (included in your bean). it is possible to give attribute-relations like 'myAttr1.myAttr2.myAttr3'. to ignore
     * fields, use <code>null</code> as beanattribute-name.
     * </p>
     * 
     * the second alternative needs all beanattribute names with a column-prefix like 'begin-end:attributename'. for
     * example: 1-11:date. it is possible to use bean relations as in the first alternative, too.
     * <p/>
     * Please notice, that the column-indexes are one-based - the first column is 1 - and the end-index will not be
     * included like in String.substring(begin, end). e.g. to read '01.01.2001' you need 1-11. the indexes are
     * equivalent to standard texteditors like notepad++.
     * 
     * @param <T> root type
     * @param r normally a buffered reader.
     * @param separation separation character. if this is null, the attributeNames must contain at least one
     *            column-index.
     * @param bean root bean type to be instantiated and filled to the result collection
     * @param attributeFormats (optional) some format-instances to parse to the right object. used if found, otherwise
     *            standard formatters will be used.
     * @param attributeNames simple attribute names or point-separated relation expressions. use null to ignore the
     *            token
     * @return filled collection of beans
     */
    public static <T> Collection<T> fromFlatFile(Reader r,
            String separation,
            Bean<T> bean,
            Map<String, Format> attributeFormats,
            String... attributeNames) {
        /*
         * do some validation checks
         */
        if (attributeNames.length == 0) {
            throw ManagedException.implementationError("give at least one attribute-name to be filled!", null);
        }
        if (separation == null) {
            boolean hasColumnIndexes = false;
            for (String n : attributeNames) {
                if (n != null && n.contains(":")) {
                    hasColumnIndexes = true;
                    break;
                }
            }
            if (!hasColumnIndexes) {
                throw ManagedException
                    .implementationError(
                        "if you don't give a separation-character, you should give at least one column-index in your attribute-names",
                        null);
            }
        }

        final Collection<T> result = new LinkedList<T>();
        final StreamTokenizer st = new StreamTokenizer(r);
        /*
         * to remove parsing of numbers by the tokenizer we have to reset all!
         * ugly jdk implementation - perhaps we should use Scanner.
         */
        st.resetSyntax();
        st.wordChars(0x00, 0xFF);
//        st.quoteChar('\"');
        st.whitespaceChars('\r', '\r');
        st.whitespaceChars('\n', '\n');
        st.eolIsSignificant(true);
        st.commentChar('#');
//            st.slashSlashComments(true);
//            st.slashStarComments(true);
        int ttype = 0;
        final Class<T> rootType = bean.getClazz();
//        bean.newInstance();
        final Map<String, Exception> errors = new Hashtable<String, Exception>();
        final String rootInfo = rootType.getSimpleName() + ".";
        /*
         * prepare the format cache to parse strings with performance
         */
        final Map<String, Format> formatCache = new HashMap<String, Format>();
        if (attributeFormats != null) {
            formatCache.putAll(attributeFormats);
        }
        /*
         * prepared fixed columns
         */
        int begin, end;
        String attrName;
        final Map<String, Point> attributeColumns = new LinkedHashMap<String, Point>(attributeNames.length);
        for (int i = 0; i < attributeNames.length; i++) {
            if (separation != null || attributeNames[i] == null) {
                attributeColumns.put((attributeNames[i] != null ? attributeNames[i] : "null:" + String.valueOf(i)),
                    null);
            } else {
                begin = Integer.valueOf(StringUtil.substring(attributeNames[i], null, "-"));
                end = Integer.valueOf(StringUtil.substring(attributeNames[i], "-", ":"));
                if (end <= begin || begin < 0 || end < 1) {
                    throw new IllegalArgumentException("The given range " + attributeNames[i] + " is illegal!");
                }
                attrName = StringUtil.substring(attributeNames[i], ":", null);
                //store one-based indexes
                attributeColumns.put(attrName, new Point(begin - 1, end - 1));
            }
        }
        final Set<String> cols = attributeColumns.keySet();
        boolean filled = false;
        /*
         * do the reading, collecting all errors to throw only one exception at the end
         */
        try {
            String t;
            while ((ttype = st.nextToken()) != StreamTokenizer.TT_EOF) {
                if (ttype != StreamTokenizer.TT_EOL && st.sval.trim().length() > 0) {
                    bean.newInstance();
                    int lastSep = 0;
                    for (final String attr : cols) {
                        final Point c = attributeColumns.get(attr);
                        if (c != null) {
                            if (c.x >= st.sval.length() || c.y > st.sval.length()) {
                                throw new StringIndexOutOfBoundsException("The range " + c.x
                                    + "-"
                                    + c.y
                                    + " is not available on line "
                                    + st.lineno()
                                    + " with length "
                                    + st.sval.length()
                                    + ":"
                                    + st.sval);
                            }
                            t = st.sval.substring(c.x, c.y);
                        } else {
                            t = StringUtil.substring(st.sval, null, separation, lastSep);
                        }
                        lastSep += t.length() + (c != null ? 0 : separation.length());
                        //at line end, no separation char will occur
                        if (st.sval.length() < lastSep) {
                            lastSep = st.sval.length();
                        }
                        if (attr == null || attr.startsWith("null:")) {
                            LOG.info("ignoring line " + st.lineno()
                                + ", token '"
                                + t
                                + "' at column "
                                + (lastSep - t.length()));
                            continue;
                        }
                        t = StringUtil.trim(t, "\"");
                        final String info = "reading line " + st.lineno() + ":'" + t + "' into " + rootInfo + attr;
                        try {
                            Object newValue = null;
                            if (!Util.isEmpty(t)) {
                                final BeanAttribute beanAttribute = BeanAttribute.getBeanAttribute(rootType, attr);
                                Format parser = formatCache.get(attr);
                                if (parser == null) {
                                    parser = FormatUtil.getDefaultFormat(beanAttribute.getType(), true);
                                    formatCache.put(attr, parser);
                                }
                                newValue = parser.parseObject(t);
                                bean.setValue(beanAttribute.getName(), newValue);
                            }
                            LOG.info(info + "(" + newValue + ")");
                            filled = true;
                        } catch (final Exception e) {
                            LOG.info("problem on " + info);
                            LOG.error(e.toString());
                            errors.put(info, e);
                        }
                    }
                    if (filled) {
                        result.add(bean.getInstance());
                    }
                }
            }
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        if (errors.size() > 0) {
            throw new ManagedException(StringUtil.toFormattedString(errors, 80, true));
        }
        LOG.info("import finished - imported items: " + result.size() + " of type " + rootType.getSimpleName());
        return result;
    }

    /**
     * delegates to {@link #present(BeanCollector, String, String, String, String, String, String, String, String)}.
     */
    public static String presentAsCSV(BeanCollector collector) {
        return present(collector, "", "", "", "\n", "", ",", null, null);
    }

    /**
     * delegates to {@link #present(BeanCollector, String, String, String, String, String, String, String, String)}.
     */
    public static String presentAsTabSheet(BeanCollector collector) {
        return present(collector, "", "", "", "\n", "", "\t", null, null);
    }

    /**
     * creates a simple html-table as presentation for the given collector.
     * 
     * @param collector
     * @return see {@link #present(BeanCollector, String, String, String, String, String, String, String, String)}
     */
    public static String presentAsHtmlTable(BeanCollector collector) {
        return present(collector, "<table>\n", "</table>", "<tr>", "</tr>\n", "<td>", "\"</td>", "", ": <div/>\"");
    }

    /**
     * creates a string representing all items with all attributes of the given beancollector (holding a collection of
     * items).
     * <p/>
     * All parameters without nameBegin and nameEnd must not be null!
     * 
     * @param collector holding a list - defining the attribute presentation.
     * @param header text header
     * @param footer text footer
     * @param rowBegin text on a new line
     * @param rowEnd text on line end
     * @param colBegin text on new column
     * @param colEnd text on column end
     * @param nameBegin (optional) if not null, starting text of a fields name. if null, no field name will be presented
     * @param nameEnd (optional) if not null, ending text of a fields name. if null, no field name will be presented
     * @return string presentation of given collector
     */
    public static String present(BeanCollector collector,
            String header,
            String footer,
            String rowBegin,
            String rowEnd,
            String colBegin,
            String colEnd,
            String nameBegin,
            String nameEnd) {
        Collection c = collector.getCurrentData();
        List<IAttributeDefinition> attributes = collector.getBeanAttributes();
        StringBuilder buf = new StringBuilder(c.size() * attributes.size() * 30 + 100);
        buf.append(header);
        for (Object o : c) {
            buf.append(rowBegin);
            for (IAttributeDefinition a : attributes) {
                buf.append(colBegin + (nameBegin != null && nameEnd != null ? nameBegin + a.getName() + nameEnd : "")
                    + collector.getColumnText(o, a) + colEnd);
            }
            buf.append(rowEnd);
        }
        buf.append(footer);
        return buf.toString();
    }

    /**
     * simple delegation to {@link #valueOf(Object, Object)}.
     */
    public static final <T> T defaultValue(T value, T defaultIfNull) {
        return valueOf(value, defaultIfNull);
    }

    /**
     * returns the value itself - or if null the defaultIfNull
     * 
     * @param <T>
     * @param value value or null
     * @param defaultIfNull default value if value is null
     * @return value of defaultIfNull
     */
    public static final <T> T valueOf(T value, T defaultIfNull) {
        return value != null ? value : defaultIfNull;
    }

    /**
     * createUUID (see {@link UUID#randomUUID()}
     * 
     * @return random uuid string (128-bit value)
     */
    public static final String createUUID() {
        return UUID.randomUUID().toString();
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

    /**
     * creates a hashcode through all single-value attibutes of given bean instance
     * 
     * @param bean instance to evaluate the hashcode for
     * @param attributes (optional) attributes to be used for hashcode
     * @return new hashcode for given bean instance
     */
    public static int hashCodeReflect(Object bean, String... attributes) {
        final int prime = 31;
        int result = 1;
        BeanClass bc = BeanClass.getBeanClass(bean.getClass());
        if (attributes == null) {
            attributes = bc.getAttributeNames();
        }
        Object v;
        for (int i = 0; i < attributes.length; i++) {
            v = BeanClass.getValue(bean, attributes[i]);
            result = prime * result + (v == null ? 0 : v.hashCode());
        }
        return result;
    }

    /**
     * if not already a bean or beancollector, the given object will wrapped into a bean or beancollector. if it is not
     * serializable, a map of values will be packed into a beancollector.
     * 
     * @param obj
     * @return
     */
    public static BeanDefinition<?> getBean(Object obj) {
        return (BeanDefinition<?>) (obj instanceof BeanDefinition<?> ? obj
            : (Util.isContainer(obj)
                ? BeanCollector.getBeanCollector(Util.getContainer(obj), 0)
                : obj instanceof Serializable ? Bean.getBean((Serializable) obj)
                    : BeanCollector.getBeanCollector(Util.getContainer(BeanUtil.toValueMap(obj)), 0)));
    }

}

/**
 * To avoid using package awt, we can't use java.awt.Point - but we need a simple Point.
 */
class Point {
    int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
