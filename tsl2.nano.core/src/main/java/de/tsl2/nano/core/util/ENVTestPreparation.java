/**
 * 
 */
package de.tsl2.nano.core.util;

import java.io.File;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Prepares a tsl2.nano unit test with an ENV in directory 'target/test'
 * 
 * @author Tom
 */
public interface ENVTestPreparation {
	static final String BASE_DIR_PREF = "tsl2.nano.";
	String BASE_DIR = (System.getProperty("user.dir").contains(BASE_DIR_PREF) ? "../" : "") + BASE_DIR_PREF;
	static final String TARGET_DIR = "target/";
	static final String TEST_DIR = ENV.PREFIX_ENVNAME + "test/";
	static final String TARGET_TEST = TARGET_DIR + TEST_DIR;

	static String setUp() {
		//baseDir path is absolute!
		return setUp(true);
	}
	
	static String setUp(boolean deleteExistingEnvironment) {
		//baseDir path is absolute!
		return setUp(System.getProperty("user.dir"), "", TEST_DIR, false, deleteExistingEnvironment);
	}
	
	default String setUp(String moduleShort) {
		return setUp(BASE_DIR, moduleShort, getTestEnv(), true, false);
	}
	static String setUp(String moduleShort, boolean strict) {
		return setUp(moduleShort, strict, true);
	}
	static String setUp(String moduleShort, boolean strict, boolean deleteExistingEnvironment) {
		//ATTENTION: here the BASE_DIR path is relative!
		return setUp(BASE_DIR, moduleShort, TEST_DIR, strict, deleteExistingEnvironment);
	}
	
	static String setUp(String baseDir, String moduleShort, String envDir, boolean strict, boolean deleteExistingEnvironment) {
		String baseDirModule = moduleShort == null ? "./" : baseDir + moduleShort + "/";
		if (!System.getProperty("user.dir").endsWith(TARGET_DIR.substring(0, TARGET_DIR.length() - 1)))
			setUserDirToTarget(baseDirModule);
		LogFactory.setLogFactoryXml(envDir + "test-logging.xml");
		LogFactory.setLogFile(envDir + "test.log");
		ENV.create(envDir);
//		ENV.setProperty(ENV.KEY_CONFIG_PATH, new File(envDir).getAbsolutePath() + "/");
		ENV.setProperty("app.strict.mode", strict);
		if (deleteExistingEnvironment)
			ENV.deleteEnvironment();
		return baseDirModule;
	}

	default String getTestEnv() {
		String name = StringUtil.substring(this.getClass().getSimpleName(), null, "Test") + "/";
		return TEST_DIR + StringUtil.toFirstLower(name);
	}
	static void tearDown() {
		try {
			ENV.deleteEnvironment();
		} catch (Exception e) {
			// ...this may result in problems on other tests...
			System.out.println(e.toString());
		}
	}

	static void removeCaches() {
		ENV.reset();
		BeanClass.clearCache();
	}
	static void setUserDirToTarget(String baseDir) {
		String userDir = System.getProperty("user.dir");
		if (!userDir.endsWith(baseDir))
	        userDir = new File(baseDir + TARGET_DIR).getAbsolutePath();
		else
			userDir = new File(TARGET_DIR).getAbsolutePath();
			
		File dir = new File(userDir);
		if (!dir.exists() || !dir.isDirectory() || !dir.canWrite())
			throw new IllegalAccessError("cannot access project directory " + userDir);
		System.setProperty("user.dir", userDir);
		System.out.println("------------------------------------------------------------");
		System.out.println("user.dir: " + System.getProperty("user.dir"));
		System.out.println("------------------------------------------------------------");
	}
	
	static String testpath(String file) {
		return FileUtil.userDirFile(file).getPath();
	}
}
