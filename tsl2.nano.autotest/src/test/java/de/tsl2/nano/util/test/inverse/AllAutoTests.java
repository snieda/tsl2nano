package de.tsl2.nano.util.test.inverse;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.core.util.FileUtil;

@RunWith(Suite.class)
@SuiteClasses({AutoFunctionTest.class})
public class AllAutoTests {
	@BeforeClass
	public static void setUp() {
		System.setProperty("user.dir", System.getProperty("user.dir") + "/target/");
	}
}
