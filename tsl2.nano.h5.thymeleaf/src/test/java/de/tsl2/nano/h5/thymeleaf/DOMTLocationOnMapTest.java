package de.tsl2.nano.h5.thymeleaf;

import org.junit.Test;
import org.w3c.dom.Document;

import de.tsl2.nano.h5.HtmlUtil;
import de.tsl2.nano.h5.NanoH5Session;
import de.tsl2.nano.h5.NanoH5Unit;
import de.tsl2.nano.util.test.TypeBean;

public class DOMTLocationOnMapTest {

	@Test
	public void testSmoke() throws Exception {
		NanoH5Session session = NanoH5Unit.createApplicationAndSession("locationonmap", new TypeBean());
		Document doc = HtmlUtil.createDocument("<html><body><p id=\"field.panel\">previoustag</p><b>test</b></body></html>");
		new DOMTLocationOnMap().decorate(doc, session);
	}

}
