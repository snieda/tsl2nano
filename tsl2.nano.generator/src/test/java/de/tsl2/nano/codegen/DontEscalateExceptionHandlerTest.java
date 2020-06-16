package de.tsl2.nano.codegen;

import static org.junit.Assert.*;

import org.junit.Test;

public class DontEscalateExceptionHandlerTest {

	@Test
	public void testEscalateClass() {
		System.setProperty("bean.generation.catchedclass", ".*ZZZ.*");
		System.setProperty("bean.generation.catchedmethod", ".*DontEscalate.*");
		try {
			new DontEscalateExceptionHandler().methodException(this.getClass(), "testDontEscalate", new RuntimeException("test"));
			fail("...exception must be rethrown");
		} catch (Exception e) {
			//Ok
		}
	}

	@Test
	public void testEscalateMethod() {
		System.setProperty("bean.generation.catchedclass", ".*DontEscalateExceptionHandlerT.*");
		System.setProperty("bean.generation.catchedmethod", ".*ZZZ.*");
		try {
			new DontEscalateExceptionHandler().methodException(this.getClass(), "testDontEscalate", new RuntimeException("test"));
			fail("...exception must be rethrown");
		} catch (Exception e) {
			//Ok
		}
	}

	@Test
	public void testDontEscalate() throws Exception {
		System.setProperty("bean.generation.catchedclass", ".*DontEscalateExceptionHandlerT.*");
		System.setProperty("bean.generation.catchedmethod", ".*DontEscalate.*");
		new DontEscalateExceptionHandler().methodException(this.getClass(), "testDontEscalate", new RuntimeException("test"));
	}

}
