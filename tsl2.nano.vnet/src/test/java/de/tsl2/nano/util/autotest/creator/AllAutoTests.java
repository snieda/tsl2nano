package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_EXCLUDE;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.network.JobServer;
import de.tsl2.nano.vnet.Net;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		System.setProperty("tsl2.functiontest.filter.voidreturn", "true");
		System.setProperty("tsl2.functiontest.filter", matchPackage(JobServer.class, Net.class));
		set(FILTER_EXCLUDE, StringUtil.matchingOneOf("NetCommunicator.setImplementationByUserInput"));
	}
}
