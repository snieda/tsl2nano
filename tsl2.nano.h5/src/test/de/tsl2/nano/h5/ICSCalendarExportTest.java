package de.tsl2.nano.h5;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.ICSCalendarExport.REPEAT;
import de.tsl2.nano.util.test.TypeBean;
import static de.tsl2.nano.h5.ICSCalendarExport.FIELD.*;
public class ICSCalendarExportTest implements ENVTestPreparation {

    @BeforeClass
    public static void setUp() {
        ENVTestPreparation.setUp();
    }

//    @AfterClass
//    public static void tearDown() {
//        ENVTestPreparation.tearDown();
//    }
    
	@Test
	public void testCalendarExport() {
		TypeBean tb = new TypeBean();
		tb.string = "mysubject";
		tb.object = "mydescription";
		tb.immutableInteger = 4711;
		tb.date = DateUtil.getDate(2020, 1, 1);
		tb.date = DateUtil.setTime(tb.date, 8, 30, 0, 0);
		String ics = new ICSCalendarExport("mycalendar", REPEAT.WEEKLY).addAll(Arrays.asList(tb)
				, MapUtil.asMap(SUMMARY, "string", DTSTART, "date", DTEND, "date"
						, DESCRIPTION, "object", UID, "immutableInteger"))
				.toString();
		System.out.println(ics);
		String expected = "BEGIN:VCALENDAR\n"
				+ "VERSION:2.0\n"
				+ "PROID:https://sourceforge.net/projects/tsl2nano/\n"
				+ "METHOD:PUBLISH\n"
				+ "BEGIN:VEVENT\n"
				+ "RRULE:FREQ=WEEKLY;BYDAY=-1SU;BYMONTH=3\n"
				+ "SUMMARY:mysubject\n"
				+ "DTSTART:20200101T083000Z\n"
				+ "DTEND:20200101T083000Z\n"
				+ "DESCRIPTION:mydescription\n"
				+ "UID:4711\n"
				+ "DTSTAMP:XXX\n"
				+ "END:VEVENT\n"
				+ "END:VCALENDAR\n";
		assertEquals(expected, ics.replaceAll("DTSTAMP[:].*", "DTSTAMP:XXX"));
		
		//smoke test only
		ICSCalendarExport.doExportICS("test", "YEARLY", Arrays.asList(tb), "SUMMARY", "string", "DTSTART", "date", "DTEND", "date");
	}

}
