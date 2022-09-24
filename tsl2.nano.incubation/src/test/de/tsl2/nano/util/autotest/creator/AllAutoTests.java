package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.tsl2.nano.agent.AttachAgent;
import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.graph.SVGChart;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		System.setProperty("tsl2.functiontest.filter", matchPackage(AttachAgent.class, SVGChart.class));
	}
}
