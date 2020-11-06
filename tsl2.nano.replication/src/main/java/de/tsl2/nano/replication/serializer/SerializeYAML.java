package de.tsl2.nano.replication.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;


public class SerializeYAML implements Serializer {
    public static final String KEY = "YAML";
    @Override
    public String getKey() {
        return KEY;
    }
    @Override
    public String getExtension() {
        return "yml";
    }
    @Override
    public ByteArrayOutputStream serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String str = new Yaml().dump(obj);
        try {
			out.write(str.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        return out;
    }
    @Override
    public <T> T deserialize(InputStream stream, Class<T> type) {
        return new Yaml().loadAs(stream, type);
    }
}