package de.tsl2.nano.core;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Test;

import de.tsl2.nano.core.util.MapUtil;

public class MainTest {

	@After
	public void tearDown() {
		ENV.reset();
	}
	@Test
	public void teststartParameterWithArgMapping() {
		String[] args = new String[] {"https://damp-cove-99806.herokuapp.com:5000/", "arg1=var1"};
		Map<Integer, String> argMapping = MapUtil.asMap(0, "service.url");
		Main.setEnvironmentArguments(argMapping, args);
		assertEquals(args[0], ENV.get("service.url", null));
		assertEquals(argMapping.get("arg1"), ENV.get("arg1", null));
	}

	@Test
	public void teststartParameterWithoutArgMapping() {
		String[] args = new String[] {"https://damp-cove-99806.herokuapp.com:99999", "arg1=var1"};
		Main.setEnvironmentArguments(null, args);
		assertEquals(args[0], ENV.get("service.url", null));
	}

	@Test
	public void teststartPortWithoutArgMapping() {
		String[] args = new String[] {"99999"};
		Main.setEnvironmentArguments(null, args);
		assertEquals("http://localhost:99999", ENV.get("service.url", null));
	}

}
