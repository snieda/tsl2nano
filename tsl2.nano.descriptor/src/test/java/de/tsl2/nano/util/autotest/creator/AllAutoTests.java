package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.AutoTest.FILTER_EXCLUDE;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.currency.CurrencyUnit;
import de.tsl2.nano.format.GenericParser;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		set(FILTER, matchPackage(BeanDefinition.class, GenericParser.class, CurrencyUnit.class));
		set(FILTER_EXCLUDE, StringUtil.matchingOneOf("CurrencyUtil.initializeCurrencyUnits"));
	}
}
