/**
 * 
 */
package de.tsl2.nano.core.util;

import java.io.File;

import org.junit.BeforeClass;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Prepares a tsl2.nano unit test with an ENV in directory 'target/test'
 * 
 * @author Tom
 */
public interface ENVTestPreparation {
	static final String BASE_DIR = "../tsl2.nano.";
	static final String TARGET_DIR = "target/";
	static final String TEST_DIR = ENV.PREFIX_ENVNAME + "test/";
	static final String TARGET_TEST = TARGET_DIR + TEST_DIR;

	static String setUp() {
		return setUp(System.getProperty("user.dir"), "", false);
	}
	
	static String setUp(String moduleShort, boolean strict) {
		return setUp(BASE_DIR, moduleShort, strict);
	}
	
	static String setUp(String baseDir, String moduleShort, boolean strict) {
		if (!System.getProperty("user.dir").endsWith(TARGET_DIR.substring(0, TARGET_DIR.length() - 1)))
			setUserDirToTarget();
		String baseDirModule = baseDir + moduleShort + "/";
		LogFactory.setLogFactoryXml(baseDirModule + TARGET_TEST + "test-logging.xml");
		LogFactory.setLogFile(baseDirModule + TARGET_TEST + "test.log");
		ENV.create(baseDirModule + TARGET_TEST);
		ENV.setProperty(ENV.KEY_CONFIG_PATH, new File(TEST_DIR).getAbsolutePath() + "/");
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

	static void setUserDirToTarget() {
        System.setProperty("user.dir", new File(TARGET_DIR).getAbsolutePath());
//		System.setProperty("user.dir", new File(baseDirModule + TARGET_DIR).getAbsolutePath());
		System.out.println("------------------------------------------------------------");
		System.out.println("user.dir: " + System.getProperty("user.dir"));
		System.out.println("------------------------------------------------------------");
	}
}
