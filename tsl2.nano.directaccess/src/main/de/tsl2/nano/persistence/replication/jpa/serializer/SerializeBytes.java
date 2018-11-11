package de.tsl2.nano.persistence.replication.jpa.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializeBytes implements Serializer {
    public static final String KEY = "BYTES";

    @Override
    public String getKey() {
        return KEY;
    }
    @Override
    public String getExtension() {
        return ".bytes";
    }
    @Override
    public ByteArrayOutputStream serialize(Object obj) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        new ObjectOutputStream(bs).writeObject(obj);
        return bs;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(InputStream stream, Class<T> type) throws ClassNotFoundException, IOException {
        return (T) new ObjectInputStream(stream).readObject();
    }
}