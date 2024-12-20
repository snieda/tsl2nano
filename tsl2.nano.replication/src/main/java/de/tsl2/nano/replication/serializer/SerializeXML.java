package de.tsl2.nano.replication.serializer;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SerializeXML implements Serializer {
    public static final String KEY = "XML";
    @Override
    public String getKey() {
        return KEY;
    }
    @Override
    public String getExtension() {
        return "xml";
    }
    @Override
    public ByteArrayOutputStream serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(out))) {
	        encoder.writeObject(obj);
        }
        return out;
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(InputStream stream, Class<T> type) {
        try (XMLDecoder dec = new XMLDecoder(stream)) { return (T) dec.readObject();}
    }
}