/*
 * created by: Tom
 * created on: 06.04.2024
 * 
 * Copyright: (c) Thomas Schneider 2024, all rights reserved
 */
package de.tsl2.nano.core.util.parser;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.AdapterProxy;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FieldUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.SupplierExVoid;
import de.tsl2.nano.core.util.Util;

/**
 * structure definitions to provide a parser and a serializer.
 * contains abstract default implementations to be used for implementations of JSon/Yaml/Xml serializers. 
 * Is able to work on recursve references, interfaces and proxies.
 * 
 * It evaluates the Annotations {@link Serial} and {@link SerialClass} to special behaviour on classes and attributes.
 * all methods will be called providing the current treeinfo - but on the parsing 
 * through {@link #toString()} some string evaluation methods will provide null as treeinfo!
 * 
 * On de-serializing the objects will be instantiated through default constructors or, if not available through a constructor 
 * with all values/fields/attributes provided by the json construct.
 * 
 * NOTE I  : see Structure class of logicstructure
 * NOTE II : the base implemenation is not performance optimized. the charsequence will be splitted into copied substrings!
 */
public interface StructParser {
    /** @return whether this implementation is able to parse the sequence */
    boolean isParseable(CharSequence s);

    /** @return whether the sequence is a list of sequences */
    boolean isList(CharSequence s, TreeInfo tree);

    /** @return the implementors structure opening string */
    String tagOpen(TreeInfo tree);

    /** @return the implementors structure closing string */
    String tagClose(TreeInfo tree);

    /** @return the implementors list/array opening string */
    default String arrOpen(TreeInfo tree) {
        return "";
    }

    default String arrElementIdentifier(TreeInfo tree) {
        return null;
    }

    /** @return the implementors list/array closing string */
    default String arrClose(TreeInfo tree) {
        if (tree != null && !tree.refPath.isEmpty())
            tree.refPath.removeLast();
        return "";
    }

    /** @return separator between tags or properties */
    String div();

    /** @return charsequence without comments. depends on {@link #commentExpression()} */
    CharSequence removeCommentsAndEmptyLines(CharSequence s);

    /** @return the implementors comment expression, may return null, if no comments are defined */
    default String commentExpression() {
        return null;
    }

    default Map<String, String> escapingTable() {
        return MapUtil.asMap("\"", "\\\"");
    }

    /** @return child elements of splitted string s */
    default String[] getChildren(CharSequence s, TreeInfo tree) {
        return StringUtil.splitUnnested(s.subSequence(tagOpen(tree).length(), s.length() - tagClose(tree).length()),
                div());
    }

    /** divides the given string to key and value */
    String[] getKeyValue(CharSequence attr);

    /** @return a structure of of Maps or/and Lists */
    Object toStructure(CharSequence s);

    /** @return whether to handle an instance as simple property and not as object (e.g. on numbers, dates, strings and booleans) */
    default boolean isSimpleType(final Object obj) {
        return obj == null || ObjectUtil.isSimpleType(obj.getClass())
                || obj instanceof AnnotatedElement // this are reflection types calling native methods -> may result in fatal errors on reflecting values of their properties
                || obj instanceof Enum
                || obj.getClass().getPackageName().startsWith("java.net");
    }

    /** @return the implementors quotation characters. default: [\"]. used to enclose keys or strings */
    default String quot() {
        return "\"";
    }

    /** trims keys and values e.g. on spaces and quotations */
    default String trim(String s) {
        return StringUtil.trim(s, " \t" + quot());
    }

    /** base implementation of splitting the sequence to into structure elements */
    default String[] splitArray(CharSequence s) {
        return splitArray(s, new TreeInfo("root"));
    }

    /** splits the sequence into an array of sequences using {@link #div()} and {@link #trim(String)} */
    default String[] splitArray(CharSequence s, TreeInfo tree) {
        assert isList(s, tree);
        // NOTE: with regex splitting it is casi not possibible, see both regex
        // String regex = "[,]\\s*[\"]?(?![^\\{\\[]*[:,])"; // "(?<=[\\]\\}]?\\s?)[,](?=\\s*[\\[\\{])"
        String[] split = StringUtil
                .splitUnnested(s.subSequence(arrOpen(tree).length(), s.length() - arrClose(tree).length()), div());
        for (int i = 0; i < split.length; i++) {
            split[i] = trim(split[i]);
        }
        return split;
    }

    /**
     * deserializer definition
     */
    interface Serializer extends StructParser {
        /** @return string represention (serialization) of given object */
        String serialize(Object obj);

        /** @return deserialized object, parsing the given string s */
        <T> T toObject(Class<T> type, CharSequence s);

        /** @return deserialized list - not supported by all structures/implementations (like xml) */
        <T> List<T> toList(Class<T> type, CharSequence s);

        /** @return deserialized primitive array - not supported by all structures/implementations (like xml) */
        Object toArray(Class<?> type, CharSequence s);

        /** enclose the property key with quotations and add a separation to the value like '=' or ':' */
        public String encloseKey(Object k, TreeInfo tree);

        /** enclose value - @see {@link #encloseKey(Object, TreeInfo)} */
        Object encloseValue(Object obj, TreeInfo tree);
    }

    /**
     * base implementations and helpers
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public abstract class AStructParser implements StructParser {
        static final Log LOG = LogFactory.getLog(TreeInfo.class);

        private static final String REG_SIMPLE_VALUE = "[.\\w-+\\d,]+";

        private Map<String, String> swappedEscapings;

        protected SerialClass properties;

        protected AStructParser() {
        }

        protected AStructParser(SerialClass s) {
            this.properties = s;

        }

        protected CharSequence unescape(CharSequence s) {
            return StringUtil.replaceAll(s, swappedEscapings());
        }

        private Map<String, String> swappedEscapings() {
            if (swappedEscapings == null) {
                swappedEscapings = MapUtil.swapKeysAndValues(escapingTable());
            }
            return swappedEscapings;
        }

        @Override
        public String[] getKeyValue(CharSequence attr) {
            return getKeyValue(attr, ":", true);
        }

        String[] getKeyValue(CharSequence attr, String kvDiv, boolean trimValue) {
            String[] kv = new String[2];
            kv[0] = trim(StringUtil.substring(attr, null, kvDiv));
            kv[1] = StringUtil.substring(attr, kvDiv, null);
            if (trimValue)
                kv[1] = kv[1].trim();
            return kv;
        }

        /**
         * 
         * @return either a list or a map
         */
        public Object toStructure(CharSequence s) {
            s = removeCommentsAndEmptyLines(s);
            return toStructure(s, new TreeInfo(s, properties));
        }

        public Object toStructure(CharSequence s, TreeInfo tree) {
            if (isList(s, tree))
                return toStructList(String.class, s, tree);
            Map map = (Map) tree.addRef(new SelfReferencingMap());
            String[] children = getChildren(s, tree);
            for (int i = 0; i < children.length; i++) {
                String[] kv = getKeyValue(children[i]);
                String v = unescape(trim(kv[1])).toString();
                if (v.equals("null")) {
                    map.put(kv[0], null);
                } else if (v.matches(REG_SIMPLE_VALUE)) {
                    Object value;
                    if (v.equals(Boolean.toString(true)) || v.equals(Boolean.toString(false)))
                        value = Boolean.valueOf(v);
                    else if (NumberUtil.isNumber(v)) {
                        value = Util.trY(() -> NumberFormat.getInstance(Locale.US).parse(v));
                    } else {
                        value = v;
                    }
                    map.put(kv[0], value);
                } else {
                    tree.increaseRecursion(kv[0], kv[1]);
                    map.put(trim(kv[0]),
                            isParseable(v)
                                    ? tree.addRef(toStructure(v, tree))
                                    : tree.getReference(v));
                    tree.decreaseRecursion();
                }
            }
            return map;
        }

        <T> List<T> toStructList(Class<T> type, CharSequence s, TreeInfo tree) {
            tree.path.getLast().setIsArray(true);
            String[] attrs = splitArray(s, tree);
            List<T> list = new ArrayList<>(attrs.length);
            for (int i = 0; i < attrs.length; i++) {
                list.add((T) (isParseable(attrs[i]) && !attrs[i].equals(s)
                        ? tree.addRef(toStructure(attrs[i], tree))
                        : tree.getReference(attrs[i])));
            }
            return list;
        }

        @Override
        public CharSequence removeCommentsAndEmptyLines(CharSequence s) {
            final String emptyLineExpr = "^\\s*$";
            return commentExpression() != null ? StringUtil.replaceAll(s, emptyLineExpr + "|" + commentExpression(), "")
                    : s;
        }

    }

    /**
     * base implementations and helpers
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public abstract class ASerializer extends AStructParser implements Serializer {

        ASerializer() {
        }

        ASerializer(SerialClass s) {
            super(s);
        }

        public String serialize(Object obj) {
            return serialize(obj, createInitialStringBuilder(), new TreeInfo(null, properties)).toString();
        }

        StringBuilder createInitialStringBuilder() {
            return new StringBuilder();
        }

        StringBuilder serialize(final Object object, StringBuilder result, TreeInfo tree) {
            final Object obj = tree.contains(object) ? tree.getReferenceKey(object)
                    : isSimpleType(object) ? object : tree.addRef(object);
            // TODO: should we remove the object from refs (added above)?
            if (tree.serializablesOnly != null && tree.serializablesOnly && !(object instanceof Serializable))
                return result;
            // TODO: can we remove the special Class/Method implementations in cause of being now simpleTypes?
            else if (obj instanceof Class) {
                createTag(result, tree, "name", ((Class) obj).getName());
            } else if (obj instanceof Method) {
                createTag(result, tree, "name", ((Method) obj).toGenericString());
            } else if (Proxy.isProxyClass(obj.getClass()) && Proxy.getInvocationHandler(obj) instanceof AdapterProxy) {
                tree.addRef(obj, () -> serializeMapObject(obj, result, tree,
                        ((AdapterProxy) Proxy.getInvocationHandler(obj)).values()));
            } else if (ObjectUtil.isSingleValueType(obj.getClass())) {
                if (isSimpleType(obj)) {
                    tree.callOnPath(nameOf(object), obj, tree.isReference(obj),
                            () -> result.append(encloseValue(obj, tree)));
                } else
                    tree.addRef(obj, () -> serializeMapObject(obj, result, tree, getValueMap(obj, tree)));
            } else if (obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive()) {
                tree.addRef(obj, () -> createArray(result, tree, PrimitiveUtil.toArrayString(obj)));
            } else if (ByteUtil.isByteStream(obj.getClass())) {
                tree.addRef(obj, () -> createArray(result, tree, ByteUtil.toString(obj)));
            } else if (!(obj instanceof Map)) {
                tree.addRef(obj, () -> serializeArray(result, tree,
                        obj instanceof Collection ? ((Collection) obj).toArray() : (Object[]) obj));
            } else
                tree.addRef(obj, () -> serializeMapObject(obj, result, tree, (Map) obj));
            return result;
        }

        @Override
        public Object encloseValue(Object obj, TreeInfo tree) {
            boolean isNumberOrBoolean = NumberUtil.isNumber(obj) || PrimitiveUtil.isBoolean(obj);
            String value = PrimitiveUtil.isPrimitiveOrWrapper(obj.getClass()) ? String.valueOf(obj)
                    : FormatUtil.format(obj);
            String quot = !isNumberOrBoolean || value.contains(div()) ? quot() : "";
            return quot + escape(value) + quot;
        }

        private Object createArray(StringBuilder result, TreeInfo tree, String content) {
            return result.append(arrOpen(tree) + content + arrClose(tree));
        }

        private StringBuilder createTag(StringBuilder result, TreeInfo tree, String key, final String content) {
            return result.append(tagOpen(tree) + encloseKey(key, tree) + encloseValue(content, tree) + tagClose(tree));
        }

        Map<String, Object> getValueMap(final Object obj, TreeInfo tree) {
            return remapByAnnotations(obj, tree);
        }

        private Map<String, Object> remapByAnnotations(Object obj, TreeInfo tree) {
            SerialClass ann = obj.getClass().getAnnotation(SerialClass.class);
            if (ann != null)
                tree.initOnce(ann);
            Map<String, Object> values = ann != null && ann.useFields()
                    ? FieldUtil.toMap(obj,
                            m -> FieldUtil.MODIFIABLE_MEMBER.test(m)
                                    && FieldUtil.WITH_MODIFIER.test(m, ann.havingModifiers()))
                    : BeanClass.BeanMap.toValueMap(obj, "", m -> ann != null
                            ? FieldUtil.WITH_MODIFIER.test(m.getAccessMethod(), ann.havingModifiers())
                            : true);
            String[] attributeOrder = ann != null && ann.attributeOrder() != null ? ann.attributeOrder()
                    : values.keySet().toArray(new String[0]);
            Map<String, Object> map = new LinkedHashMap<>();
            Format format;
            for (String name : attributeOrder) {
                Serial serial = Proprietizer.serial(obj.getClass(), name, false,
                        (tree.useFieldsOnly != null && tree.useFieldsOnly) || (ann != null && ann.useFields()));
                name = serial.name() != null ? serial.name() : name;
                format = serial.formatter() != null && ObjectUtil.isInstanceable(serial.formatter())
                        ? Util.trY(() -> BeanClass.getBeanClass(serial.formatter()).createInstance(), false,
                                InstantiationException.class)
                        : null;
                Object v = values.get(name);
                if (!serial.ignore() && v != null) {
                    if (serial.embedItems()) {
                        embedItems(map, v, format);
                    } else
                        map.put(name, format != null ? format.format(v) : v);
                }
            }
            return map;
        }

        public CharSequence escape(CharSequence s) {
            return StringUtil.replaceAll(s, escapingTable());
        }

        private void embedItems(Map<String, Object> map, Object object, Format format) {
            Map<String, Object> items = object instanceof List ? toMap((List<?>) object)
                    : FieldUtil.toSerializingMap(object);
            for (String name : items.keySet()) {
                Object v = items.get(name);
                map.put(name, format != null ? format.format(v) : v);
            }
        }

        private Map<String, Object> toMap(List<?> list) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>(list.size());
            list.forEach(i -> map.put(i.getClass().getSimpleName(), i));
            return map;
        }

        void serializeArray(StringBuilder result, TreeInfo tree, Object[] orr) {
            result.append(arrOpen(tree));
            for (int i = 0; i < orr.length; i++) {
                if (orr[i] != null) {
                    if (Util.isSimpleType(orr[i].getClass()))
                        result.append(encloseValue(orr[i], tree));
                    else {
                        serialize(orr[i], result, tree);
                    }
                    result.append(div());
                }
            }
            if (orr.length > 0 && result.length() > 1
                    && (div().isEmpty() || result.charAt(result.length() - 1) == div().charAt(0)))
                result.deleteCharAt(result.length() - 1);
            result.append(arrClose(tree));
        }

        private String nameOf(Object obj) {
            return obj.getClass().getSimpleName();
        }

        public String serialize(Map map) {
            TreeInfo tree = new TreeInfo();
            return serializeMapObject(tree.addRef(map), createInitialStringBuilder(), tree, map).toString();
        }

        StringBuilder serializeMapObject(Object obj, StringBuilder s, TreeInfo tree, Map<String, Object> m) {
            if (!tree.hasMaxRecursionReached(obj)) {
                tree.callOnPath(nameOf(obj), obj, true, () -> serializeMap(m, s, tree));
            }
            return s;
        }

        StringBuilder serializeMap(Map map, StringBuilder s, TreeInfo tree) {
            Set keys = map.keySet();
            s.append(tagOpen(tree));
            for (Object k : keys) {
                Object v = map.get(k);
                if (Util.isEmpty(v)) {
                    continue;
                } else if (tree.contains(v)) {
                    v = tree.getReferenceKey(v);
                }
                s.append(encloseKey(k, tree.increaseRecursion(k, v)));
                serialize(v, s, tree);
                tree.decreaseRecursion();
                s.append(div());
            }
            if (s.length() > 1 && String.valueOf(s.charAt(s.length() - 1)).equals(div()))
                s.deleteCharAt(s.length() - 1);
            s.append(tagClose(tree)).toString();
            return s;
        }

        public <T> List<T> toList(Class<T> type, CharSequence s) {
            List list = (List) toStructure(s);
            return BeanClass.BeanMap.fillList(type, list);
        }

        public Object toArray(Class type, CharSequence s) {
            List list = toList(type, s);
            return type.isPrimitive() ? ObjectUtil.fromListOfWrappers(type, list)
                    : list.toArray((Object[]) Array.newInstance(type, 0));
        }

        public <T> T toObject(Class<T> type, CharSequence s) {
            return (T) BeanClass.getBeanClass(type).map().fromValueMap((Map<String, Object>) toStructure(s));
        }

        public static class Proprietizer {
            private static Serial EMPTY_SERIAL_PROXY = Util.proxy(Serial.class,
                    (m, args) -> m.getReturnType().isPrimitive() ? PrimitiveUtil.getDefaultValue(m.getReturnType())
                            : null);

            public static final Serial serial(Class<?> cls, String name, boolean setter) {
                return serial(cls, name, setter, null);
            }

            public static final Serial serial(IAttribute<?> attr, boolean setter) {
                return serial(attr.getType(), attr.getName(), setter, null);
            }

            public static final Serial serial(Class<?> cls, String name, boolean setter, Boolean fieldsOnly) {
                return Util.value(getAnnotation(cls, name, setter, Serial.class, fieldsOnly), EMPTY_SERIAL_PROXY);
            }

            public static final <A extends Annotation> A getAnnotation(Class<?> cls, String name, boolean setter,
                    Class<A> annotationType, Boolean fieldsOnly) {
                if (fieldsOnly == null) {
                    SerialClass annClass = cls.getAnnotation(SerialClass.class);
                    fieldsOnly = annClass != null ? annClass.useFields() : false;
                }
                A ann = null;
                if (!fieldsOnly) {
                    BeanAttribute<?> attr = BeanAttribute.getBeanAttribute(cls, name, false);
                    if (attr != null) {
                        Method m = setter
                                ? BeanAttribute.getBeanAttribute(attr.getAccessMethod()).getWriteAccessMethod()
                                : attr.getAccessMethod();
                        if (m != null)
                            ann = m.getAnnotation(annotationType);
                    }
                }
                if (ann == null) {
                    Field field = Util.trY(() -> FieldUtil.getField(cls, name), false);
                    if (field != null)
                        ann = field.getAnnotation(annotationType);
                }
                return ann;
            }
        }
    }
}

@SuppressWarnings({ "rawtypes", "unchecked" })
/**
 * try to reuse references and avoid endless recursive self-referencing loops
 * TODO: clean usage of addRef <-> callOnPath and refsPath <-> path
 */
class TreeInfo {
    static final String KEY_ROOT = "root";
    private static final Log LOG = LogFactory.getLog(TreeInfo.class);
    /** current path with key and value currently used by Xml and Yaml */
    LinkedList<Item> path = new LinkedList<>();
    /** current path (used currently by referencing objects) */
    // TODO: replace by path_
    LinkedList refPath = new LinkedList<>();
    /** stored references */
    List refs = new LinkedList<>();
    int recursion;
    public Boolean serializablesOnly;
    /** whether to evaluate class fields instead of bean class attributes throuth their getters/setters */
    public Boolean useFieldsOnly;
    Integer havingModifiers;
    /** whether tag was opened to embed simple attributes - that has to be finished to add child tags into it (see Xml) */
    private boolean tagOpenUnfinished;

    TreeInfo() {
    }

    TreeInfo(Object root) {
        this(root, null);
    }

    public TreeInfo(Object root, SerialClass s) {
        if (root != null)
            increaseRecursion(KEY_ROOT, root);
        if (s != null) {
            initOnce(s);
        }
    }

    void initOnce(SerialClass s) {
        if (useFieldsOnly == null)
            useFieldsOnly = s.useFields();
        if (serializablesOnly == null)
            serializablesOnly = s.implementingSerializable();
        if (havingModifiers == null)
            havingModifiers = s.havingModifiers();
    }

    Object get(int index) {
        return refs.get(index);
    }

    public boolean isReference(Object obj) {
        return obj instanceof String && ((String) obj).matches("@\\d+");
    }

    boolean contains(Object obj) {
        return refs.contains(obj) || parentPathContains(obj);
    }

    private boolean parentPathContains(Object obj) {
        return path.size() > 1
                && path.subList(0, path.size() - 1).stream().anyMatch(i -> i.value == obj);
    }

    public Item getParent() {
        return path.size() > 1 ? path.get(path.size() - 2) : Item.EMPTY_ITEM;
    }

    Item current() {
        return path.size() > 0 ? path.getLast() : Item.EMPTY_ITEM;
    }

    String currentName() {
        return path.size() > 0 ? path.getLast().key : refPath.getLast().getClass().getSimpleName();
    }

    Object getReference(String value) {
        if (isReference(value)) {
            Integer index = Integer.valueOf(StringUtil.extract(value, "\\d+"));
            if (index >= refs.size()) {
                String msg = IndexOutOfBoundsException.class.getSimpleName() + ": " + value
                        + " --> ref index >  size of references:" + refs.size();
                LOG.error(msg);
                return msg;
            } else
                return get(index);
        } else {
            return value;
        }
    }

    Object addRef(Object value) {
        return addRef(value, null);
    }

    Object addRef(Object value, SupplierExVoid callback) {
        boolean parentReferenceAdded = avoidEndlessReferenceLoop(value);
        if (callback != null)
            callback.get();
        if (!parentReferenceAdded && !refs.contains(value))
            refs.add(value);
        return value;
    }

    public Object getReferenceKey(Object object) {
        return "@" + refs.indexOf(object);
    }

    private boolean avoidEndlessReferenceLoop(Object value) {
        if (refs.contains(value))
            return true;
        // in cause of endless self-references we store the object here too early (before creating the string for it)!
        if (refPath.contains(value)) {
            refs.add(value);
            return true;
        } else {
            refPath.add(value);
            return false;
        }
    }

    public TreeInfo callOnPath(Object key, Object value, boolean increase, SupplierExVoid callback) {
        if (increase)
            increaseRecursion(key, value);
        callback.get();
        if (increase)
            decreaseRecursion();
        return this;
    }

    public TreeInfo increaseRecursion(Object k, Object v) {
        ++recursion;
        path.add(new Item(k, v));
        return this;
    }

    public TreeInfo decreaseRecursion() {
        --recursion;
        path.removeLast();
        return this;
    }

    public boolean hasMaxRecursionReached(Object obj) {
        int maxRecursion = Util.get("tsl2.serializer.recursion.max", 20);
        if (recursion > maxRecursion || Util.isJavaInternal(obj.getClass())) {
            LOG.warn("ignoring creation of item on '" + obj.getClass() + "' (recursion: "
                    + recursion + ")");
            return true;
        }
        return false;
    }

    public boolean consumeTagOpenUnfinished() {
        boolean last = tagOpenUnfinished;
        tagOpenUnfinished = false;
        return last;
    }

    public void setTagOpenUnfinished(boolean tagOpenUnfishished) {
        this.tagOpenUnfinished = tagOpenUnfishished;
    }

    class Item {
        String key;
        Object value;
        private Boolean isArray; // save the state, given by the implementation - only for performance aspects
        private Boolean isStream; // bytestreams and any primitive arrays

        static final Item EMPTY_ITEM;
        static {
            EMPTY_ITEM = new TreeInfo().new Item("EMPTY", "EMPTY");
            EMPTY_ITEM.isArray = false;
            EMPTY_ITEM.isStream = false;
        }

        public Item(Object k, Object v) {
            this.key = String.valueOf(k);
            this.value = v;
        }

        boolean isArray() {
            if (isArray == null)
                isArray = value.getClass().isArray() || value instanceof Iterable;
            return isArray;
        }

        public void setIsArray(Boolean isArray) {
            this.isArray = isArray;
        }

        boolean isStream() {
            if (isStream == null)
                isStream = ByteUtil.isByteStream(value.getClass())
                        || value.getClass().isArray() && value.getClass().getComponentType().isPrimitive();
            return isStream;
        }

    }

    public boolean isRoot() {
        return path.isEmpty() || path.getLast().key.equals(KEY_ROOT);
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        path.forEach(i -> s.append(i.key + "=>"));
        s.replace(s.length(), s.length(), " [");
        s.append("refs: " + refs.size() + ", recursion: " + recursion + "]");
        return s.toString();
    }
}

/**
 * avoid endless toString() or hashCode() calls inside the standard Map implementations (going through the content)
 */
class SelfReferencingMap extends LinkedHashMap<String, Object> {
    final int hashCode = new Object().hashCode();

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + hashCode;
    }
}
