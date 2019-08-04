#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.h5.NanoH5Unit;

public class MyNanoApplicationIT extends NanoH5Unit {

    @BeforeClass
    public static void setUp() {
        NanoH5Unit.setUp();
    }
    
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
