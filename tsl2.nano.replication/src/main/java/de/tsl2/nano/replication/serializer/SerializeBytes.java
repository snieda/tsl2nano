package de.tsl2.nano.replication.serializer;

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
        return "bytes";
    }
    @Override
    public ByteArrayOutputStream serialize(Object obj) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        try(ObjectOutputStream stream = new ObjectOutputStream(bs)) {
			stream.writeObject(obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        return bs;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(InputStream stream, Class<T> type) throws ClassNotFoundException, IOException {
        try(ObjectInputStream in = new ObjectInputStream(stream)) {return (T) in.readObject();}
    }
}