package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.DUPLICATION;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_EXCLUDE;
import static de.tsl2.nano.autotest.creator.AutoTest.MODIFIER;
import static de.tsl2.nano.autotest.creator.AutoTest.TIMEOUT;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.util.StringUtil;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		System.setProperty("tsl2nano.offline", "true");
		set(DUPLICATION, 10);
//		set(TIMEOUT, -1);
		set(MODIFIER, -1); // public: 1
		set(FILTER_EXCLUDE, StringUtil.matchingOneOf("ENVTestPreparation","ENV.delete","getFileOutput",
				"SystemUtil.executeRegisteredLinuxBrowser","SystemUtil.softExitOnCurrentThreadGroup",
				"ThreadState.top","LogFactory","ConcurrentUtil.getCaller","ConcurrentUtil.sleep",
				"Profiler","NumberUtil.numbers","StringUtil.fixString","CollectionUtil.copyOfRange",
				"Profiler.workLoop","NumberUtil.fixLengthNumber","DateUtil.getWorkdayCount", "ByteUtil.getPipe", "IPreferences", "CPUState"));
		set(FILTER, matchPackage(Main.class));
	}
}
