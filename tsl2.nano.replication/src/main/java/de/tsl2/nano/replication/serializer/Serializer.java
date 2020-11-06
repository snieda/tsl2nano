package de.tsl2.nano.replication.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public interface Serializer {
    String getKey();
    String getExtension();
    ByteArrayOutputStream serialize(Object obj);
    <T> T deserialize(InputStream stream, Class<T> type) throws IOException, ClassNotFoundException;
    
    //simple extension factory
    static final List<Serializer> serializers = new LinkedList<>();
    public static Serializer getByKey(String key) {
    	if (serializers.isEmpty()) { //it's not possible to do that in the sub classes on classloading - they wont be loaded!
    		serializers.addAll(Arrays.asList(new SerializeXML(), new SerializeBytes(), new SerializeJAXB()
    				, new SerializeJSON(), new SerializeYAML(), new SerializeSimpleXml()));
    	}
        return serializers.stream().filter(s -> s.getKey().equals(key.toUpperCase())).findFirst().orElse(null);
    }
}
