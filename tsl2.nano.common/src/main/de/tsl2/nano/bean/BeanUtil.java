/*
 * Copyright © 2002-2009 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.bean;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

import de.tsl2.nano.Messages;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.format.FormatUtil;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.StringUtil;

/**
 * A Utility-Class for beans
 * 
 * @author ds 05.03.2009
 * @author ts 05.03.2009
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BeanUtil {
    private static final Log LOG = LogFactory.getLog(BeanUtil.class);
    private static final List<String> STD_TYPE_PKGS;

    static {
        STD_TYPE_PKGS = new ArrayList<String>(3);
        STD_TYPE_PKGS.add("java.lang");
        STD_TYPE_PKGS.add("java.util");
        STD_TYPE_PKGS.add("java.math");
        STD_TYPE_PKGS.add("java.sql");
    };

    /**
     * Compares two Bean-Objects by serializing them comparing their equivalent byte-arrays.
     * <p/>
     * Condition: the comparable objects need to implement the Serializable-interface and have to be from the same
     * class.
     * 
     * @param bean1 first bean
     * @param bean2 second bean
     * @return true if both bean-objects are equal
     */
    public static boolean equals(Object bean1, Object bean2) {
        if (bean1 == bean2) {
            return true;
        }
        if ((bean1 == null) || (bean2 == null)) {
            return false;
        }

        if (!(bean1 instanceof Serializable)) {
            return false;
        }
        if (!(bean2 instanceof Serializable)) {
            return false;
        }

        if (!bean1.getClass().equals(bean2.getClass())) {
            return false;
        }
        if (bean1.equals(bean2))
            return true;

        return equals(convertToByteArray(bean1), convertToByteArray(bean2));

    }

    /**
     * Equals to serialized bean-object as byte-arrays.
     * 
     * @param bean1 first bean
     * @param bean2 second bean
     * @return true if both bean-objects are equal
     */
    public static boolean equals(byte[] bean1, byte[] bean2) {
        return Arrays.equals(bean1, bean2);
    }

    /**
     * copy serializing bean - doing a deep copy. may fail, if classloader (the current threads loader) is unable to
     * load nested classes.
     * <p/>
     * to copy only values, have a look at {@link BeanClass#copyValues(Object, Object, String...)} and
     * {@link #clone(Object)}.
     * 
     * @param <T> serializable bean
     * @return new instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T copy(T bean) {
        final byte[] ser = convertToByteArray(bean);
        return (T) convertToObject(ser, Thread.currentThread().getContextClassLoader());
    }

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
        return BeanClass.copyValues(src, dest, false, attributeNames);
    }

    /**
     * copies all not-null values to dest.
     * <p/>
     * delegates to {@link BeanClass#copyValues(Object, Object, String...)}.
     */
    public static <D> D addValues(Object src, D dest, String... attributeNames) {
        return BeanClass.copyValues(src, dest, true, attributeNames);
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
     * delegates to {@link BeanClass#createOwnCollectionInstances(Object)}.
     */
    public static <S> S createOwnCollectionInstances(S src) {
        return BeanClass.createOwnCollectionInstances(src);
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
            return (T) BeanClass.copyValues(src, src.getClass().newInstance());
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * Serialization of a bean-object to a byte-array.
     * 
     * @param bean to serialize to byte array
     * @return serialized bean
     */
    public static byte[] serializeBean(Object bean) {
        if (!(bean instanceof Serializable)) {
            if (bean != null) {
                LOG.warn("trying to serialize a non-serializeable object: " + bean.getClass().getName());
            }
            return null;//throw new FormattedException("bean must implement serializeable!");
        }
        return convertToByteArray(bean);
    }

    /**
     * Serialization of a bean object to a byte-array
     * 
     * @param bean to serialize to byte array
     * @return serialized bean
     */
    private static byte[] convertToByteArray(Object bean) {
        try {
            LOG.debug("creating byte array through serializing object of type " + bean.getClass());
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream o = new ObjectOutputStream(bos);
            o.writeObject(bean);
            o.close();
            LOG.debug("serialized byte array for type " + bean.getClass() + " size: " + bos.size() + " bytes");
            return bos.toByteArray();
        } catch (final IOException ex) {
            ForwardedException.forward(ex);
            return null;
        }

    }

    private static Object convertToObject(byte[] bytes) {
        return convertToObject(bytes, null);
    }

    /**
     * deserialization of a byte-array
     * 
     * @param bytes to deserialize
     * @return deserialized bean
     */
    private static Object convertToObject(byte[] bytes, final ClassLoader classLoader) {
        try {
            final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            final ObjectInputStream i;
            /*
             * if a classloader was given, we will use it - otherwise the native evaluated classloader will work.
             */
            if (classLoader != null) {
                i = new ObjectInputStream(bis) {
                    @Override
                    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                        String name = desc.getName();
                        try {
                            return Class.forName(name, false, classLoader);
                        } catch (Exception ex) {
                            return super.resolveClass(desc);
                        }
                    }
                };
            } else {
                i = new ObjectInputStream(bis);
            }
            /*
             * now, do the standard things
             */
            final Object object = i.readObject();
            i.close();
            return object;
        } catch (final Exception ex) {
            return ForwardedException.forward(ex);
        }

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
        if (type.getName().equals(Object.class.getName()))
            return false;
        //on array types, the package is null!
        final String p = type.getPackage() != null ? type.getPackage().getName() : type.getClass().getName();
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
        final String p = type.getPackage() != null ? type.getPackage().getName() : type.getClass().getName();
        return type.isInterface() || STD_TYPE_PKGS.contains(p);
    }

    /**
     * isSingleValueType
     * 
     * @param type class to analyze
     * @return true, if type is not a map, collection or array.
     */
    public static boolean isSingleValueType(Class<?> type) {
        return !(type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
    }

    /**
     * isByteStream
     * 
     * @param type class to analyse
     * @return true, if type is a byte (or Byte) array, or simple Serializable interface.
     */
    public static boolean isByteStream(Class<?> type) {
        return type.equals(Serializable.class) || (type.isArray() && (type.isAssignableFrom(Byte.class) || type.isAssignableFrom(Byte.class)));
    }

    /**
     * if all fields are null, the bean is empty
     * 
     * @param bean instance
     * @param filterAttributes attributes to ignore
     * @return true, if all attributes are null.
     */
    public static boolean isEmpty(Object bean, String... filterAttributes) {
        final BeanClass bc = new BeanClass(bean.getClass());
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
     * getGenericType
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
                if (clazz.getGenericInterfaces().length > 0)
                    genericType = clazz.getGenericInterfaces()[0];
                else
                    return null;
            }
            return (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
        } catch (Exception e) {
            ForwardedException.forward(e);
            return null;
        }
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
            return (Class<?>) ((ParameterizedType) clazz.getDeclaredField(fieldName).getGenericType()).getActualTypeArguments()[0];
        } catch (Exception e) {
            ForwardedException.forward(e);
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
        final Bean bean = new Bean(o);//Bean.getBean((Serializable)o);
        if (onlyFilterAttributes && filterAttributes.length > 0)
            bean.setAttributeFilter(filterAttributes);
        return bean.toValueMap(useClassPrefix, onlySingleValues, onlyFilterAttributes, filterAttributes);
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
        final Bean bean = new Bean(o);//Bean.getBean((Serializable)o);
        if (onlyFilteredAttributes && filterAttributes.length > 0)
            bean.setAttributeFilter(filterAttributes);
        return bean.toValueMap(keyPrefix, onlySingleValues, onlyFilteredAttributes, filterAttributes);
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
            ForwardedException.forward(e);
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
            ForwardedException.forward(e);
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
     * a new instance will be created on each new line.</p> there are two possibilities to use this method:</br> - with
     * a field separator (like comma or semicolon)</br> - with line-column definitions (like
     * '1-10:myBeanAttributeName)</p>
     * 
     * with the first alternative, you give a separator (not null) and the pure attribute names of the given rootType
     * (included in your bean). it is possible to give attribute-relations like 'myAttr1.myAttr2.myAttr3'. to ignore
     * fields, use <code>null</code> as beanattribute-name.</p>
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
        if (attributeNames.length == 0)
            throw FormattedException.implementationError("give at least one attribute-name to be filled!", null);
        if (separation == null) {
            boolean hasColumnIndexes = false;
            for (String n : attributeNames) {
                if (n != null && n.contains(":")) {
                    hasColumnIndexes = true;
                    break;
                }
            }
            if (!hasColumnIndexes)
                throw FormattedException.implementationError("if you don't give a separation-character, you should give at least one column-index in your attribute-names",
                    null);
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
                        if (st.sval.length() < lastSep)
                            lastSep = st.sval.length();
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
                            if (!StringUtil.isEmpty(t)) {
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
            ForwardedException.forward(e);
        }
        if (errors.size() > 0) {
            throw new FormattedException(StringUtil.toFormattedString(errors, 80, true));
        }
        LOG.info("import finished - imported items: " + result.size() + " of type " + rootType.getSimpleName());
        return result;
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
            ForwardedException.forward(e);
        }
    }

    /**
     * checks, whether the class of the given object implements 'toString()' itself.
     * 
     * @param obj instance of class to evaluate
     * @return true, if class of object overrides toString()
     */
    public static boolean hasToString(Object obj) {
        try {
            final Method method = obj.getClass().getMethod("toString", new Class[0]);
            //pure objects, representating there instance id
            return !method.toString().equals(OBJ_TOSTRING);
        } catch (final Exception e) {
            ForwardedException.forward(e);
            return false;
        }
    }

    /**
     * creates a hashcode through all single-value attibutes of given bean instance
     * @param bean instance to evaluate the hashcode for
     * @param attributes (optional) attributes to be used for hashcode
     * @return new hashcode for given bean instance
     */
    public static int hashCodeReflect(Object bean, String...attributes) {
        final int prime = 31;
        int result = 1;
        BeanClass bc = new BeanClass(bean.getClass());
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
