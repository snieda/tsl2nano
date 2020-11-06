package de.tsl2.nano.replication.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;


public class SerializeJSON implements Serializer {
    public static final String KEY = "JSON";
    @Override
    public String getKey() {
        return KEY;
    }
    @Override
    public String getExtension() {
        return "json";
    }
    @Override
    public ByteArrayOutputStream serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new ObjectMapper().writer().writeValue(out, obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        return out;
    }
    @Override
    public <T> T deserialize(InputStream stream, Class<T> type) {
        try {
			return new ObjectMapper().readerFor(type).readValue(stream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
}