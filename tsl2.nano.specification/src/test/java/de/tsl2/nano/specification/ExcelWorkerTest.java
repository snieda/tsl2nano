package de.tsl2.nano.specification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.specification.ExcelWorker;

public class ExcelWorkerTest {
	static int policiert = 0;

	@Test
	public void testExcelWorker() {
		policiert = 0;
		Util.trY( () -> new ExcelWorker("src/test/resources/test-sheet.txt").run(), false);
		assertEquals(2, policiert);
	}

	@Test
	public void testExcelWorkerDryRun() {
		System.setProperty(Worker.DRYRUN, "true");
		policiert = 0;
		new ExcelWorker("src/test/resources/test-sheet.txt").run();
		assertEquals(0, policiert);
	}

	@Test
	public void testExcelWorkerSwallowTabs() {
		System.setProperty("tsl2nano.excelworker.swallowtabs", "true");
		policiert = 0;
		new ExcelWorker("src/test/resources/test-sheet.txt").run();
		assertEquals(2, policiert);
	}
}
