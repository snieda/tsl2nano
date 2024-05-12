package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.cursus.Consilium;
import de.tsl2.nano.cursus.persistence.EConsilium;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		set(FILTER, matchPackage(Consilium.class, EConsilium.class));
		// set(AutoTest.FILTER_EXCLUDE, StringUtil.matchingOneOf(Timer.class.getSimpleName() + ".(runThrough|next).*"));
	}
}
