package de.tsl2.nano.util.autotest.creator;

import static de.tsl2.nano.autotest.creator.AutoTest.FILTER;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.matchPackage;
import static de.tsl2.nano.autotest.creator.InitAllAutoTests.set;

import java.util.Arrays;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.w3c.dom.Document;

import de.tsl2.nano.autotest.ValueSets;
import de.tsl2.nano.autotest.creator.AutoFunctionTest;
import de.tsl2.nano.autotest.creator.CurrentStatePreservationTest;
import de.tsl2.nano.autotest.creator.InitAllAutoTests;
import de.tsl2.nano.h5.HtmlUtil;
import de.tsl2.nano.h5.thymeleaf.DOMTLocationOnMap;

@RunWith(Suite.class)
@SuiteClasses({InitAllAutoTests.class, AutoFunctionTest.class, CurrentStatePreservationTest.class})
public class AllAutoTests {
	public static void init() {
		Document doc = HtmlUtil.createDocument("<html><body><p id=\"field.panel\">previoustag</p><b>test</b></body></html>");
		ValueSets.storeSet(Document.class, Arrays.asList(doc));

		set(FILTER, matchPackage(DOMTLocationOnMap.class));
	}
}
