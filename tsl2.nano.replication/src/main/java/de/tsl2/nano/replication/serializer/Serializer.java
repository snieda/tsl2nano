package de.tsl2.nano.replication.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public interface Serializer {
    String getKey();
    String getExtension();
    ByteArrayOutputStream serialize(Object obj) throws IOException;
    <T> T deserialize(InputStream stream, Class<T> type) throws IOException, ClassNotFoundException;
}
