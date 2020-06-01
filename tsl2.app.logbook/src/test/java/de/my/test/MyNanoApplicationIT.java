package de.my.test;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.h5.NanoH5Unit;
import de.tsl2.nano.core.util.ConcurrentUtil;

public class MyNanoApplicationIT extends NanoH5Unit {

	@Override
	public String getTestEnv() {
		return "pre-integration-test/.nanoh5.environment";
	}

    @Before
    public void setUp() {
		port = 8067;
        System.setProperty("app.server.running", "true");
        super.setUpUnit(null);
    }
    
	@Test
	public void testBeanDefinition() throws Exception {
		HtmlPage page = runWebClient();
		page = submit(page, BTN_LOGIN_OK);
//		assertTrue(page.getElementById("Location") != null);
		page = submit(page, BEANCOLLECTORLIST + BTN_OPEN);
	}

	@After
	public void tearDown() {
		super.tearDown();
	}
}
