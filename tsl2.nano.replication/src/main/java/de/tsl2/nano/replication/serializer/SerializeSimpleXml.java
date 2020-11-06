package de.tsl2.nano.replication.serializer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.simpleframework.xml.core.Persister;


public class SerializeSimpleXml implements Serializer {
    public static final String KEY = "SIMPLE_XML";
    @Override
    public String getKey() {
        return KEY;
    }
    @Override
    public String getExtension() {
        return "simple.xml";
    }
    @Override
    public ByteArrayOutputStream serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new Persister().write(obj, out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        return out;
    }
    @Override
    public <T> T deserialize(InputStream stream, Class<T> type) {
        try {
			return new Persister().read(type, stream);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * convenient to be used as Consumer (e.g.: SerializeSimpleXml::serializer)
     * @param obj
     */
    public static void serializer(Object obj) {
    	new SerializeSimpleXml().serialize(obj);
    }
}