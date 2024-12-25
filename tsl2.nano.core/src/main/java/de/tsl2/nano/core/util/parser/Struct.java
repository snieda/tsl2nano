package de.tsl2.nano.core.util.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.FileUtil;

public class Struct {
    static final List<Class<? extends StructParser.ASerializer>> serializers = Arrays.asList(JSon.class, Yaml.class,
            Xml.class);

    public static void serialize(Object obj, Path file, Class<? extends StructParser.ASerializer> serializer) {
        String s = serialize(obj, serializer);
        try {
            if (file.getParent() != null)
                Files.createDirectories(file.getParent());
            Files.write(file, s.getBytes());
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }
    
    public static String serialize(Object obj, Class<? extends StructParser.ASerializer> serializer) {
        return newInstance(serializer).serialize(obj);
    }

    private static <T extends StructParser.ASerializer> T newInstance(Class<T> serializer) {
        return BeanClass.getBeanClass(serializer).createInstance();
    }

    public static <T> T deserialize(Path file, Class<T> type) {
        return deserialize(FileUtil.getFileOrResourceAsString(file.toString()), type);
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
