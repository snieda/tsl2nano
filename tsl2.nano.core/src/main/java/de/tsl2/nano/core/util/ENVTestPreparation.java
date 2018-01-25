/**
 * 
 */
package de.tsl2.nano.core.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.tsl2.nano.core.ENV;

/**
 * Prepares a tsl2.nano unit test with an ENV in directory 'target/test'
 * 
 * @author Tom
 */
public interface ENVTestPreparation {
	static final String BASE_DIR = "../tsl2.nano.";
	static final String TARGET_TEST = "target/" + ENV.PREFIX_ENVNAME + "test/";

	static String setUp(String moduleShort, boolean strict) {
		String baseDirModule = BASE_DIR + moduleShort + "/";
		ENV.create(baseDirModule + TARGET_TEST);
		ENV.setProperty(ENV.KEY_CONFIG_PATH, TARGET_TEST);
		ENV.setProperty("app.strict.mode", strict);
		ENV.deleteEnvironment();
		return baseDirModule;
	}

	static void tearDown() {
		try {
			ENV.deleteEnvironment();
		} catch (Exception e) {
			// ...this may result in problems on other tests...
			System.out.println(e.toString());
		}
	}
}
