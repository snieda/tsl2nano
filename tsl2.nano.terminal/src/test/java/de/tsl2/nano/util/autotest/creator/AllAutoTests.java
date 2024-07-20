package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.platform.PlatformManagement;
import de.tsl2.nano.terminal.SIShell;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		System.setProperty("tsl2.functiontest.filter.void.return", "true");
		System.setProperty("tsl2.functiontest.filter", matchPackage(PlatformManagement.class, SIShell.class));
		System.setProperty("tsl2.functiontest.filter.exclude",
				".*(SIShell.main|PrintWriter|Platform|PlatformTest.testPlatformBeans).*");
	}
}
