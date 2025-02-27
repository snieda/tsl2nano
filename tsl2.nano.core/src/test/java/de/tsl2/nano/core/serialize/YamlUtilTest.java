package de.tsl2.nano.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.ENVTestPreparation;

public class YamlUtilTest implements ENVTestPreparation {
	static boolean initSerializationPassed;
	static boolean initDeserializationPassed;
	
	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("core");
	}
	
	@Test
	public void testDumpAndLoad() {
		Reference ref = new Reference<Reference>(Reference.class, new Reference(Object.class, null));
		String dump = YamlUtil.dump(ref);
		System.out.println("\n"+dump);
		
		String expected = "!<Reference>\n"
				+ "type: !<Class> {name: de.tsl2.nano.core.serialize.Reference}\n"
				+ "value: !<Reference>\n"
				+ "  type: !<Class> {name: java.lang.Object}\n";
		assertEquals(expected, dump);
		
		Reference load = YamlUtil.load(expected, Reference.class);
		//yaml handles back-references
		expected = "!<Reference>\n"
				+ "type: !<Class> {name: de.tsl2.nano.core.serialize.Reference}\n"
				+ "value: !<Reference>\n"
				+ "  type: !<Class> {name: java.lang.Object}\n";
		assertEquals(expected, YamlUtil.dump(load));
		assertTrue(initSerializationPassed && initDeserializationPassed);
	}

	@Test
	public void testProxyDumpAndLoad() {
//		Object proxy = DelegationHandler.createProxy(new DelegationHandler(new Reference(10)));
		Object proxy = DelegationHandler.createProxy(new DelegationHandler(10));
		String dump = YamlUtil.dump(proxy);
		System.out.println("\n" + dump);
		
		YamlUtil.load(dump, DelegationHandler.class);
	}
}

class Reference<T> {
	Class<T> type;
	T value;
	Reference() {}
	Reference(T value) {
		this((Class<T>)(value != null ? value.getClass() : Class.class), value);
	}
	Reference(Class<T> type, T value) {
		this.type = type;
		this.value = value;
	}
	void initSerialization() {
		YamlUtilTest.initSerializationPassed = true;
	}
	void initDeserialization() {
		YamlUtilTest.initDeserializationPassed = true;
	}
}
