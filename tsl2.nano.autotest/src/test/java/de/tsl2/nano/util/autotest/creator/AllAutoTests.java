package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_EXCLUDE;
import static de.tsl2.nano.autotest.creator.AutoTest.MODIFIER;
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
		// setSequentialForDebugging(null);
		set(MODIFIER, -1); // public: 1
		set(FILTER_EXCLUDE, StringUtil.matchingOneOf("ENVTestPreparation","ENV.delete","getFileOutput",
				"SystemUtil.executeRegisteredLinuxBrowser","SystemUtil.softExitOnCurrentThreadGroup",
				"ThreadState.top","LogFactory","ConcurrentUtil.getCaller","ConcurrentUtil.sleep",
				"Profiler","NumberUtil.numbers","StringUtil.fixString","CollectionUtil.copyOfRange",
				"Profiler.workLoop", "NumberUtil.fixLengthNumber", "DateUtil.setNoTimeOffset",
				"DateUtil.setUTCTimeZone", "ByteUtil.getPipe", "IPreferences", "CPUState",
						"AnnotationProxy.setAnnotationValues", "Message.send", "PrivateAccessor.findMembers", "ValueSet", "Mail", "FieldUtil.foreach",
				"ConcurrentUtil.waitFor", "SystemUtil.isNestedApplicationStart", "FileUtil.readBytes",
				"FieldUtil.getFieldNamesInHierarchy", "ObjectUtil.isEmpty", "ValueRandomizer.createFromRegEx*"));
		set(FILTER, matchPackage(Main.class));
		// set(FILTER, matchMethod(Crypt.class, "decrypt", CharSequence.class, String.class, String.class));
	}
}
