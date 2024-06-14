package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.annotation.extension.With;
import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.execution.VolatileResult;
import de.tsl2.nano.fi.FIDate;
import de.tsl2.nano.plugin.Plugin;
import de.tsl2.nano.scanner.FieldReader;
import de.tsl2.nano.util.FuzzyFinder;
import de.tsl2.nano.util.MarkdownLog;

@RunWith(Suite.class)
// @Execution(SAME_THREAD)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		// set(PARALLEL, false);
		System.setProperty("tsl2.functiontest.filter", matchPackage(MarkdownLog.class
		, CommonAction.class
		, With.class
		, VolatileResult.class
		, FIDate.class
		, Plugin.class
		, FieldReader.class
		, FuzzyFinder.class ));
		System.setProperty("tsl2.functiontest.filter.exclude", "(.*[.]Mail.*|.*VolatileResult.get.*)");
	}
}
