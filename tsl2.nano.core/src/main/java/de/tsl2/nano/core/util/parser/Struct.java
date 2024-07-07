package de.tsl2.nano.core.util.parser;

import java.util.Arrays;
import java.util.List;

import de.tsl2.nano.core.cls.BeanClass;

public class Struct {
    static final List<Class<? extends StructParser.ASerializer>> serializers = Arrays.asList(JSon.class, Yaml.class,
            Xml.class);

    public static String serialize(Object obj, Class<? extends StructParser.ASerializer> serializer) {
        return newInstance(serializer).serialize(obj);
    }

    private static <T extends StructParser.ASerializer> T newInstance(Class<T> serializer) {
        return BeanClass.getBeanClass(serializer).createInstance();
    }

    public static <T> T deserialize(CharSequence serialized, Class<T> type) {
        return serializers.stream()
                .map(s -> newInstance(s))
                .filter(i -> i.isParseable(serialized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no implementation found for format of given serialized string"))
                .toObject(type, serialized);
    }
}
