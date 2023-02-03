package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.AutoTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoH5Unit;
import de.tsl2.nano.h5.timesheet.Timesheet;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		set(FILTER, matchPackage(NanoH5.class));
		set(AutoTest.FILTER_EXCLUDE, StringUtil.matchingOneOf(NanoH5Unit.class.getSimpleName(), Timesheet.class.getSimpleName()));
	}
}
