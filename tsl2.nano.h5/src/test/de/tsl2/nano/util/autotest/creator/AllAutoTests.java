package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.CREATE_RANDDOM_MAX_DEPTH;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.methods;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.AutoTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoH5Unit;
import de.tsl2.nano.h5.NanoH5Util;
import de.tsl2.nano.h5.RuleCover;
import de.tsl2.nano.h5.SpecificationH5Exchange;
import de.tsl2.nano.h5.timesheet.Timesheet;
import de.tsl2.nano.h5.websocket.NanoWebSocketServer;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		set(CREATE_RANDDOM_MAX_DEPTH, 2); // org.w3.Document -> Element -> Document
		set(FILTER, matchPackage(NanoH5.class));
		set(AutoTest.FILTER_EXCLUDE,
			StringUtil.matchingOneOf("stop", "reset", "clear", "teardown",
			NanoH5Unit.class.getSimpleName(),
			Timesheet.class.getSimpleName(),
			methods(RuleCover.class, "cover"),
			methods(NanoH5Util.class, "createCompositor"),
			// methods(NanoH5IT.class, "testNano"),
			methods(SpecificationH5Exchange.class, "enrichFromSpecificationProperties"),
			methods(Html5Presentation.class, "createNavigationbar|reset"),
			methods(NanoWebSocketServer.class, "stop")));
	}
}