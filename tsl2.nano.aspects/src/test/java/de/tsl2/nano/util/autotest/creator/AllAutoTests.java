package de.tsl2.nano.util.autotest.creator;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.BaseTest;
import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;

@RunWith(Suite.class)
@SuiteClasses({AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	@BeforeClass
	public static void setUp() {
		BaseTest.useTargetDir();
	}
}
