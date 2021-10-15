package de.tsl2.nano.h5.timesheet;

import java.util.Collection;

import org.anonymous.project.Charge;

import de.tsl2.nano.bean.SimpleBeanReader;
import de.tsl2.nano.core.ENV;

public class SBRImport {
	public static Collection<Charge> doImportHumanReadable(String file) {
		SimpleBeanReader<Charge> reader = new SimpleBeanReader<>(Charge.class, ENV.get("timesheet.import.expression", "fromdate: fromtime-totime  (pause) value chargeitem: comment"));
		return reader.read(file);
	}
}
