package de.tsl2.nano.persistence.replication.jpa.serializer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.bind.JAXB;


public class SerializeJAXB implements Serializer {
    public static final String KEY = "JAXB";
    @Override
    public String getKey() {
        return KEY;
    }
    @Override
    public String getExtension() {
        return "jaxb.xml";
    }
    @Override
    public ByteArrayOutputStream serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JAXB.marshal(obj, out);
        return out;
    }
    @Override
    public <T> T deserialize(InputStream stream, Class<T> type) {
        return JAXB.unmarshal(stream, type);
    }
}