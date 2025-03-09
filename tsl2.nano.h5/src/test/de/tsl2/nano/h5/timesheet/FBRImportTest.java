package de.tsl2.nano.h5.timesheet;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import org.anonymous.project.Charge;
import org.anonymous.project.Chargeitem;
import org.anonymous.project.Item;
import org.anonymous.project.Party;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.configuration.ConfigBeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.NanoH5Util;

public class FBRImportTest implements ENVTestPreparation {

	@Before
	public void setUp() {
		Locale.setDefault(Locale.GERMANY);
		Bean.clearCache();
		ENVTestPreparation.super.setUp("h5");
        ENV.addService(BeanPresentationHelper.class, new Html5Presentation<>());
        ConfigBeanContainer.initAuthAndLocalBeanContainer();
	}
	@After
	public void tearDown() {
		// ENVTestPreparation.tearDown();
	}
	@Test
	public void testImport() {
		Item item = new Item();
		item.setName("XXX");
		ConfigBeanContainer.simpleReturnExampleItself();
		BeanProxy.doReturnWhen(ConfigBeanContainer.getGenServiceProxy(), (m, a) -> Arrays.asList(item), "findByQuery");

		NanoH5Util.define(Party.class, null, "{name}");
		NanoH5Util.define(Chargeitem.class, null, "{item}");
		NanoH5Util.define(Item.class, null, "{name}");
		
		File file = FileUtil.userDirFile("test-classes/import-timesheet.log");
		
		Collection<Charge> c = FBRImport.doImportHumanReadable(file.getPath());
		assertEquals(527, c.size());
	}
	@Test
	public void testSimpleImport() {
		File file = FileUtil.userDirFile("test.txt");
		String s = "21.06.: 07:30-17:00(0,5h)  9,0h TICKET-123 Analyse\n\n"
				 + "22.06.: 08:15-17:15 (0,25h)8,0h TICKET-234 Implementierung\n"
				 + "23.06.: 08:30-17:00(0,5h)  8,25h TICKET-234   Implementierung\n"
				 + "24.06.: 08:30-17:30(0,5h)  8,0h TICKET-345 Implementierung\n";
		FileUtil.writeBytes(s.getBytes(), file.getPath(), false);
		//TODO: create the whole tree to persist the relation to TICKET-xxx
//		Item item = BeanContainer.instance().createBean(Item.class);
//		item.setId(1);
//		item.setn

		Collection<Charge> c = FBRImport.doImportHumanReadable(file.getPath());
		assertEquals(4, c.size());
	
		Iterator<Charge> it = c.iterator();
		expect(it.next(), 21, 6, 7, 30, 17, 0, 0.5d, 9d, "TICKET-123", "Analyse");
		//TODO: do that for the other 3 lines
	}
	private void expect(Charge charge, int day, int month, int fromHour, int fromMinute, int toHour, int toMinute, Double pause, Double duration, String object, String description) {
		int year = DateUtil.getCurrentYear();
		assertEquals(DateUtil.getDate(year, 6, 21), charge.getFromdate());
		//TODO: the formatters read no jpa informations - this is done on nanoh5 context only
//		assertEquals(DateUtil.getTime(fromHour, fromMinute), charge.getFromtime());
//		assertEquals(DateUtil.getTime(toHour, toMinute), charge.getTotime());
//		assertEquals(pause, charge.getPause());
//		assertEquals(duration, charge.getValue());
//		assertEquals(object, charge.getChargeitem().getItem().getName());
		assertEquals(description, charge.getComment());
	}

}
