package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.*;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.historize.HistorizedInput;
import de.tsl2.nano.structure.StructureTest;
@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		set("filter", matchPackage(CollectionUtil.class, HistorizedInput.class, StructureTest.class));
		set("filter.exclude", ".*Map.put.*"); // ExpiringMap.put may vary in result dependent on time
	}
}
