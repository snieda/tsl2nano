package de.tsl2.nano.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;

/**
 * structure definitions to provide a parser and a serializer.
 * contains abstract default implementations to be used for implementations of JSon/Yaml/Xml serializers.
 * NOTE: see Structure class of logicstructure
 */
public interface StructParser {
    /** whether this implementation is able to parse the sequence */
    boolean isParseable(CharSequence s);

    /** whether the sequence is a list of sequences */
    boolean isList(CharSequence s);

    /** the implementors structure opening string */
    String tagOpen(TreeInfo tree);

    /** the implementors structure closing string */
    String tagClose(TreeInfo tree);

    /** the implementors list/array opening string */
    default String arrOpen(TreeInfo tree) {
        return "";
    }

    /** the implementors list/array closing string */
    default String arrClose(TreeInfo tree) {
        tree.path.removeLast();
        return "";
    }

    /** separator between tags or properties */
    String div();

    /** enclose the property key with quotations and add a separation to the value like '=' or ':' 
     * @param tree */
    public String encloseKey(Object k, TreeInfo tree);

    default Object encloseValue(Object obj, TreeInfo tree) {
        boolean isNumberOrBoolean = NumberUtil.isNumber(obj) || PrimitiveUtil.isBoolean(obj);
        String value = PrimitiveUtil.isPrimitiveOrWrapper(obj.getClass()) ? String.valueOf(obj)
                : FormatUtil.format(obj);
        String quot = !isNumberOrBoolean || value.contains(div()) ? quot() : "";
        return quot + value + quot;
    }

    default String quot() {
        return "\"";
    }

    default String[] getChildren(CharSequence s) {
        return StringUtil.splitUnnested(s.subSequence(1, s.length() - 1), div());
    }

    /** divides the given string to key and value */
    String[] getKeyValue(String attr);

    /** @return a structure of of Maps or/and Lists */
    Object toStructure(CharSequence s);

    /** whether to handle an object as simple property and not as object (e.g. on numbers, dates, strings and booleans) */
    default boolean isSimpleType(final Object obj) {
        return ObjectUtil.isSimpleType(obj.getClass());
    }

    /** trims keys and values e.g. on spaces and quotations */
    default String trim(String s) {
        return StringUtil.trim(s, " \"");
    }

    /** splits the sequence into an array of sequences using {@link #div()} and {@link #trim(String)} */
    default String[] splitArray(CharSequence s) {
        assert isList(s);
        // NOTE: with regex splitting it is casi not possibible, see both regex
        // String regex = "[,]\\s*[\"]?(?![^\\{\\[]*[:,])"; // "(?<=[\\]\\}]?\\s?)[,](?=\\s*[\\[\\{])"
        String[] split = StringUtil.splitUnnested(s.subSequence(1, s.length() - 1), div());
        for (int i = 0; i < split.length; i++) {
            split[i] = trim(split[i]);
        }
        return split;
    }

    /**
     * deserializer definition
     */
    interface Serializer extends StructParser {
        String serialize(Object obj);

        <T> T toObject(Class<T> type, CharSequence s);

        <T> List<T> toList(Class<T> type, CharSequence s);

        Object toArray(Class<?> type, CharSequence s);

    }

    /**
     * base implementations and helpers
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public abstract class AStructParser implements StructParser {
        /**
         * 
         * @return either a list or a map
         */
        public Object toStructure(CharSequence s) {
            return toStructure_(s, new TreeInfo());
        }

        public Object toStructure_(CharSequence s, TreeInfo tree) {
            if (isList(s))
                return toStructList(String.class, s, tree);
            Map map = (Map) tree.addRef(new SelfReferencingMap());
            String[] childs = getChildren(s);
            for (int i = 0; i < childs.length; i++) {
                String[] kv = getKeyValue(childs[i]);
                if (kv[1].equals("null")) {
                    map.put(kv[0], null);
                } else if (kv[1].matches("[.\\w-+\\d,]+")) {
                    Object value;
                    if (kv[1].equals(Boolean.toString(true)) || kv[1].equals(Boolean.toString(false)))
                        value = Boolean.valueOf(kv[1]);
                    else /*if (kv[1].contains(",") ||  kv[1].contains())*/ {
                        value = Util.trY(() -> NumberFormat.getInstance(Locale.US).parse(kv[1]));
                    }
                    map.put(kv[0], value);
                } else {
                    kv[1] = trim(kv[1]);
                    map.put(trim(kv[0]),
                            isParseable(kv[1])
                                    ? tree.addRef(toStructure_(kv[1], tree))
                                    : tree.getReference(kv[1]));
                }
            }
            return map;
        }

        <T> List<T> toStructList(Class<T> type, CharSequence s, TreeInfo tree) {
            String[] attrs = splitArray(s);
            List<T> list = new ArrayList<>(attrs.length);
            for (int i = 0; i < attrs.length; i++) {
                list.add((T) (isParseable(attrs[i])
                        ? tree.addRef(toStructure_(attrs[i], tree))
                        : tree.getReference(attrs[i])));
            }
            return list;
        }

    }

    /**
     * base implementations and helpers
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public abstract class ASerializer extends AStructParser {
        public String serialize(Object obj) {
            return serialize(obj, new StringBuilder(), new TreeInfo()).toString();
        }

        void serializeArray(StringBuilder result, TreeInfo tree, Object[] orr) {
            result.append(arrOpen(tree));
            for (int i = 0; i < orr.length; i++) {
                if (orr[i] != null) {
                    /*arr[i] = */if (Util.isSimpleType(orr[i].getClass()))
                        result.append("\"" + FormatUtil.format(orr[i]) + "\"");
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

        void serializeMapObject(Object obj, StringBuilder s, TreeInfo tree, Map<String, Object> m) {
            if (tree.hasMaxRecursionReached(obj)) {
                return;
            }
            serializeMap(m, s, tree);
        }

        public String serialize(Map map) {
            return serializeMap(map, new StringBuilder(), new TreeInfo()).toString();
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
                s.append(encloseKey(k, tree));
                serialize(v, s, tree.increaseRecursion(k, v));
                tree.decreaseRecursion();
                s.append(div());
            }
            if (s.length() > 1 && String.valueOf(s.charAt(s.length() - 1)).equals(div()))
                s.deleteCharAt(s.length() - 1);
            s.append(tagClose(tree)).toString();
            return s;
        }

        StringBuilder serialize(final Object object, StringBuilder result, TreeInfo tree) {
            final Object obj = tree.contains(object) ? tree.getReferenceKey(object) : object;
            if (obj instanceof Class) {
                result.append(tagOpen(tree) + ((Class) obj).getName() + tagClose(tree));
            } else if (obj instanceof Method) {
                result.append(tagOpen(tree) + ((Method) obj).toGenericString() + tagClose(tree));
            } else if (Proxy.isProxyClass(obj.getClass()) && Proxy.getInvocationHandler(obj) instanceof AdapterProxy) {
                tree.addRef(obj, () -> serializeMapObject(obj, result, tree,
                        ((AdapterProxy) Proxy.getInvocationHandler(obj)).values()));
            } else if (ObjectUtil.isSingleValueType(obj.getClass())) {
                if (isSimpleType(obj)) {
                    result.append(encloseValue(obj, tree));
                } else
                    tree.addRef(obj, () -> serializeMapObject(obj, result, tree, FieldUtil.toSerializingMap(obj)));
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
 */
class TreeInfo {
    private static final Log LOG = LogFactory.getLog(TreeInfo.class);
    /** current path */
    LinkedList path = new LinkedList<>();
    LinkedList<Object[]> path_ = new LinkedList<>();
    /** stored references */
    List refs = new LinkedList<>();
    int recursion;
    public boolean array;

    Object get(int index) {
        return refs.get(index);
    }

    boolean contains(Object obj) {
        return refs.contains(obj);
    }

    String currentName() {
        return path_.size() > 0 ? path_.getLast()[0].toString() : path.getLast().getClass().getSimpleName();
    }

    Object getReference(String value) {
        if (value.matches("@\\d+")) {
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
        if (!parentReferenceAdded)
            refs.add(value);
        return value;
    }

    public Object getReferenceKey(Object object) {
        return "@" + refs.indexOf(object);
    }

    private boolean avoidEndlessReferenceLoop(Object value) {
        // in cause of endless self-references we store the object here too early (before creating the string for it)!
        if (path.contains(value)) {
            refs.add(value);
            return true;
        } else {
            path.add(value);
            return false;
        }
    }

    public TreeInfo increaseRecursion(Object k, Object v) {
        ++recursion;
        path_.add(new Object[] { k, v });
        return this;
    }

    public TreeInfo decreaseRecursion() {
        --recursion;
        path_.removeLast();
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