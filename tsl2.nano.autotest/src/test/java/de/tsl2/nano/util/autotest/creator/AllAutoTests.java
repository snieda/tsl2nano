package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.core.Main;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
//		System.setProperty("tsl2nano.offline", "true");
		System.setProperty("tsl2.functiontest.filter.exclude", ".*(ENVTestPreparation|getFileOutput|SystemUtil.executeRegisteredLinuxBrowser|SystemUtil.softExitOnCurrentThreadGroup|ThreadState.top|LogFactory).*");
		System.setProperty("tsl2.functiontest.filter", matchPackage(Main.class));
	}
}
