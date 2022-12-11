package de.tsl2.nano.h5;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;

/**
 * exports any objects to ics format. example: new ICSCalendarExport("myCal").addAll(myObjects, MapUtil.asMap(FIELD.SUMMARY, "myTitle", ...)).write();
 * @author ts
 */
public class ICSCalendarExport {
	StringBuilder buf;
	String name;
	enum REPEAT {DAILY, WEEKLY, MONTHLY, YEARLY}
	enum FIELD {BEGIN, VERSION, PROID, METHOD, UID, LOCATION, SUMMARY, DESCRIPTION, CATEGORIES, CLASS, TZID, RRULE, DTSTART, DTEND, DTSTAMP, END}
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
	private REPEAT repeat;
	
	public ICSCalendarExport(String name, REPEAT repeat) {
		this.name = name;
		this.repeat = repeat;
		buf = new StringBuilder(getStartBlock());
	}
	protected String getStartBlock() {
		return FIELD.BEGIN + ":VCALENDAR\n" + FIELD.VERSION + ":2.0\n" + FIELD.PROID + ":https://sourceforge.net/projects/tsl2nano/\n" + FIELD.METHOD + ":PUBLISH\n";
	}
	protected String getEndBlock() {
		return FIELD.END + ":VCALENDAR\n";
	}
	public ICSCalendarExport addAll(Collection<?> objects, Map<FIELD, String> attributeMapping) {
		assert objects != null && attributeMapping != null && attributeMapping.size() > 2 : "at least SUMMARY, DTSTART, DTEND must be mapped!"; 
		for (Object obj: objects) {
			buf.append(toICS(obj, attributeMapping));
		}
		return this;
	}
	protected String toICS(Object obj, Map<FIELD, String> attributeMapping) {
		StringBuilder ics = new StringBuilder();
		Bean<Object> bean = Bean.getBean(obj);
		ics.append(FIELD.BEGIN + ":VEVENT\n");
		if (repeat != null)
			ics.append(FIELD.RRULE + rule(repeat));
		for ( Entry<FIELD, String> e : attributeMapping.entrySet()) {
			Object v = bean.getValue(e.getValue());
			if (v instanceof Date) {
				v = sdf.format(v);
			}
			ics.append(e.getKey() + ":" + v + "\n");
		}
		
		ics.append(FIELD.DTSTAMP + ":" + sdf.format(new Date()) + "\n" + FIELD.END + ":VEVENT\n");
		return ics.toString();
	}
	protected String rule(REPEAT r) {
		return ":FREQ=" + r + ";BYDAY=-1SU;BYMONTH=3\n";
	}
	public void write() {
		try {
			Files.write(Paths.get(getFileName()), buf.toString().getBytes());
		} catch (IOException e) {
			ManagedException.forward(e);
		}
	}
	public String getFileName() {
		return ENV.getConfigPath() + name + ".ics";
	}
	@Override
	public String toString() {
		return buf.toString() + getEndBlock();
	}
	
	public static void doExportICS(String name, String rule, Collection<?> items, String...attributeMapping) {
		HashMap<FIELD, String> map = new HashMap<>();
		for (int i=0; i<attributeMapping.length-1; i+=2) {
			map.put(FIELD.valueOf(attributeMapping[i]), attributeMapping[i+1]);
		}
		new ICSCalendarExport(name, REPEAT.valueOf(rule)).addAll(items, map).write();
	}
}
