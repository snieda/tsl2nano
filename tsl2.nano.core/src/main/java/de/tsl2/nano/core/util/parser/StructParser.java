package de.tsl2.nano.core.util.parser;

import java.lang.reflect.Array;
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
 * all methods will be called providing the current treeinfo - but on the parsing 
 * through {@link #toString()} some string evaluation methods will provide null as treeinfo!
 * 
 * NOTE I  : see Structure class of logicstructure
 * NOTE II : the base implemenation is not performance optimized. the charsequence will be splitted into copied substrings!
 * NOTE III: Warning: serialization inspects field values, deserialization inspects bean attributes (getter/setter)
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

    /** enclose the property key with quotations and add a separation to the value like '=' or ':' */
    public String encloseKey(Object k, TreeInfo tree);

    /** enclose value - @see {@link #encloseKey(Object, TreeInfo)} */
    default Object encloseValue(Object obj, TreeInfo tree) {
        boolean isNumberOrBoolean = NumberUtil.isNumber(obj) || PrimitiveUtil.isBoolean(obj);
        String value = PrimitiveUtil.isPrimitiveOrWrapper(obj.getClass()) ? String.valueOf(obj)
                : FormatUtil.format(obj);
        String quot = !isNumberOrBoolean || value.contains(div()) ? quot() : "";
        return quot + value + quot;
    }

    /** @return the implementors quotation characters. default: [\"]. used to enclose keys or strings */
    default String quot() {
        return "\"";
    }

    /** @return charsequence without comments. depends on {@link #commentExpression()} */
    CharSequence removeCommentsAndEmptyLines(CharSequence s);

    /** @return the implementors comment expression, may return null, if no comments are defined */
    default String commentExpression() {
        return null;
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

    /** @return whether to handle an object as simple property and not as object (e.g. on numbers, dates, strings and booleans) */
    default boolean isSimpleType(final Object obj) {
        return ObjectUtil.isSimpleType(obj.getClass());
    }

    /** trims keys and values e.g. on spaces and quotations */
    default String trim(String s) {
        return StringUtil.trim(s, " \t" + quot());
    }

    /** base implementation of splitting the sequence to into structure elements */
    default String[] splitArray(CharSequence s) {
        return splitArray(s, null);
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

    }

    /**
     * base implementations and helpers
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public abstract class AStructParser implements StructParser {
        static final Log LOG = LogFactory.getLog(TreeInfo.class);

        private static final String REG_SIMPLE_VALUE = "[.\\w-+\\d,]+";

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
            return toStructure(s, new TreeInfo().increaseRecursion(TreeInfo.KEY_ROOT, s));
        }

        public Object toStructure(CharSequence s, TreeInfo tree) {
            if (isList(s, tree))
                return toStructList(String.class, s, tree);
            Map map = (Map) tree.addRef(new SelfReferencingMap());
            String[] children = getChildren(s, tree);
            for (int i = 0; i < children.length; i++) {
                String[] kv = getKeyValue(children[i]);
                String v = trim(kv[1]);
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
            tree.path.getLast().isArray = true;
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
    public abstract class ASerializer extends AStructParser {
        public String serialize(Object obj) {
            return serialize(obj, createInitialStringBuilder(), new TreeInfo()).toString();
        }

        StringBuilder createInitialStringBuilder() {
            return new StringBuilder();
        }

        StringBuilder serialize(final Object object, StringBuilder result, TreeInfo tree) {
            final Object obj = tree.contains(object) ? tree.getReferenceKey(object)
                    : isSimpleType(object) ? object : tree.addRef(object);
            if (obj instanceof Class) {
                result.append(tagOpen(tree) + ((Class) obj).getName() + tagClose(tree));
            } else if (obj instanceof Method) {
                result.append(tagOpen(tree) + ((Method) obj).toGenericString() + tagClose(tree));
            } else if (Proxy.isProxyClass(obj.getClass()) && Proxy.getInvocationHandler(obj) instanceof AdapterProxy) {
                tree.addRef(obj, () -> serializeMapObject(obj, result, tree,
                        ((AdapterProxy) Proxy.getInvocationHandler(obj)).values()));
            } else if (ObjectUtil.isSingleValueType(obj.getClass())) {
                if (isSimpleType(obj)) {
                    tree.callOnPath(nameOf(object), obj, tree.isReference(obj),
                            () -> result.append(encloseValue(obj, tree)));
                } else
                    tree.addRef(obj, () -> serializeMapObject(obj, result, tree, getValueMap(obj)));
            } else if (obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive()) {
                tree.addRef(obj, () -> result.append(PrimitiveUtil.toArrayString(obj)));
            } else if (ByteUtil.isByteStream(obj.getClass())) {
                tree.addRef(obj, () -> result.append(ByteUtil.toString(obj)));
            } else if (!(obj instanceof Map)) {
                tree.addRef(obj, () -> serializeArray(result, tree,
                        obj instanceof Collection ? ((Collection) obj).toArray() : (Object[]) obj));
            } else
                tree.addRef(obj, () -> serializeMapObject(obj, result, tree, (Map) obj));
            return result;
        }

        Map<String, Object> getValueMap(final Object obj) {
            //TODO: here get the map out of fields, but on deserialization we get them from bean getters/setters
            return remapByAnnotations(obj, FieldUtil.toSerializingMap(obj));
        }

        private Map<String, Object> remapByAnnotations(Object obj, Map<String, Object> values) {
            SerialClass ann = obj.getClass().getAnnotation(SerialClass.class);
            String[] attributeOrder = ann != null && ann.attributeOrder() != null ? ann.attributeOrder()
                    : values.keySet().toArray(new String[0]);
            BeanClass<Object> bc = BeanClass.getBeanClass(obj);
            Map<String, Object> map = new LinkedHashMap<>();
            Format format;
            for (String name : attributeOrder) {
                IAttribute attr = bc.getAttribute(name);
                Serial serial = BeanAttribute.serial(attr, false);
                name = serial.name() != null ? serial.name() : name;
                format = serial.formatter() != null
                        ? BeanClass.getBeanClass(serial.formatter()).createInstance()
                        : null;
                if (!serial.ignore())
                    map.put(name, format != null ? format.format(values.get(name)) : values.get(name));
            }
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
            if (orr.length > 0)
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
            return BeanClass.fillList(type, list);
        }

        public Object toArray(Class type, CharSequence s) {
            List list = toList(type, s);
            return type.isPrimitive() ? ObjectUtil.fromListOfWrappers(type, list)
                    : list.toArray((Object[]) Array.newInstance(type, 0));
        }

        public <T> T toObject(Class<T> type, CharSequence s) {
            return (T) BeanClass.getBeanClass(type).fromValueMap((Map<String, Object>) toStructure(s));
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
        return path.size() > 1 ? path.get(path.size() - 2) : null;
    }

    String currentName() {
        return path.size() > 0 ? path.getLast().key : refPath.getLast().getClass().getSimpleName();
    }

    Object getReference(String value) {
        if (isReference(value)) {
            String sindex = StringUtil.extract(value, "\\d+");
            return get(Integer.valueOf(sindex));
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

    class Item {
        String key;
        Object value;
        boolean isArray; // save the state, given by the implementation - only for performance aspects

        public Item(Object k, Object v) {
            this.key = String.valueOf(k);
            this.value = v;
        }

        boolean isArray() {
            return isArray || value.getClass().isArray() || value instanceof Iterable;
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
 * avoid endless toString() or hashCod() calls inside the standard Map implemenations (going through the content)
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
}