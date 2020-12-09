package de.tsl2.nano.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class PrintUtilTest {

	@Test
	public void testPrint() {
		PrintUtil.main("print source=printer-info".split("\\s"));
		try {
			PrintUtil.main("print source=printer-info printer=PDFCreator".split("\\s"));
		} catch (Exception e) {
			checkMessageWIthPDFCreator(e);
		}
		try {
			PrintUtil.main("print source=**/*.pdf printer=PDFCreator papersize=ISO_A4".split("\\s"));
		} catch (Exception e) {
			checkMessageWIthPDFCreator(e);
		}
	}

	private void checkMessageWIthPDFCreator(Exception e) {
		boolean messageWithDPFCreator = e.getMessage() != null && e.getMessage().contains("PDFCreator");
		if (!messageWithDPFCreator)
			e.printStackTrace();
		assertTrue(e.toString(), messageWithDPFCreator);
	}

}
