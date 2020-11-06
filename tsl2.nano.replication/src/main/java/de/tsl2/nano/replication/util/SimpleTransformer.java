package de.tsl2.nano.replication.util;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * tries to anonymize personal data.
 * <p/>
 * transforms all fields of an object, that match the regex found in system
 * properties 'replication.transformer.regex' to the fixed string 'XXXXX'.
 * 
 * @author ts
 */
public class SimpleTransformer<T> implements Consumer<T> {
	static final String XXX = "XXXXX";

	@Override
	public void accept(T t) {
		Field[] fields = t.getClass().getDeclaredFields();
		String regex = System.getProperty("replication.transformer.regex", ".*[.]((pre|sur)?name|street|code|address)");
		System.out.print("transforming entity: " + t + "... ");
		int c = 0;
		for (Field f : fields) {
			if (String.class.isAssignableFrom(f.getType()) && f.toString().matches(regex)) {
				try {
					if (f.get(t) != null) {
						f.set(t, XXX);
						c++;
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		System.out.print(c + " fields transformed\n");
	}

}
