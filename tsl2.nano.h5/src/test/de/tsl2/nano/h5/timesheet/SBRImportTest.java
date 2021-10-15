package de.tsl2.nano.h5.timesheet;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collection;

import org.anonymous.project.Charge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;

public class SBRImportTest implements ENVTestPreparation {

	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("h5");
	}
	@After
	public void tearDown() {
		ENVTestPreparation.tearDown();
	}
	@Test
	public void testImport() {
		File file = FileUtil.userDirFile("test.txt");
		String s = "21.06.: 07:30-17:00(0,5h)  9,0h TICKET-123 Analyse\n\n"
				+ "22.06.: 08:30-17:00(0,5h)  8,0h TICKET-234 Implementierung\n";
		FileUtil.writeBytes(s.getBytes(), file.getPath(), false);
		Collection<Charge> c = SBRImport.doImportHumanReadable(file.getPath());
		assertEquals(2, c.size());
		
		Charge c1 = c.iterator().next();
		assertEquals(DateUtil.getDate(DateUtil.getCurrentYear(), 6, 21), c1.getFromdate());
	}

}
