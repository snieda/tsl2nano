package de.tsl2.nano.util.autotest.creator;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.aspect.AspectCover;
import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		System.setProperty("tsl2.functiontest.filter", ".*" + AspectCover.class.getPackage().getName() + ".*");
	}
}
