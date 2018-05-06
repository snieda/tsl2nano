
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.h5.NanoH5Unit;

public class MyNanoApplicationTest extends NanoH5Unit {

	@Test
	public void testBeanDefinition() throws Exception {
		HtmlPage page = runWebClient();
		page = submit(page, BTN_LOGIN_OK);
//		assertTrue(page.getElementById("Location") != null);
		page = submit(page, BEANCOLLECTORLIST + BTN_OPEN);
	}

	@AfterClass
	public static void tearDown() {
//		shutdown();
	}
}
