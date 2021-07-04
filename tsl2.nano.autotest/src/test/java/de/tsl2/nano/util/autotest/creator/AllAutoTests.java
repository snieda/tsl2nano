package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.*;

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
		set(false, "parallel");
//		set(true, "testneverfail");
//		set("duplication", 5);
		set("modifier", -1); // public: 1
		set("filter.exclude", StringUtil.matchingOneOf("ENVTestPreparation","ENV.delete","getFileOutput",
				"SystemUtil.executeRegisteredLinuxBrowser","SystemUtil.softExitOnCurrentThreadGroup",
				"ThreadState.top","LogFactory","ConcurrentUtil.getCaller","ConcurrentUtil.sleep",
				"Profiler","NumberUtil.numbers","StringUtil.fixString","CollectionUtil.copyOfRange",
				"Profiler.workLoop","NumberUtil.fixLengthNumber","DateUtil.getWorkdayCount", "ByteUtil.getPipe"));
//		set("filter.error.types", ".*OutOfMemory.*");
		String matchPackage = matchPackage(Main.class);
//		matchPackage = ".*ManagedException.assertion.*"; 
		set("filter", matchPackage);
	}
}
