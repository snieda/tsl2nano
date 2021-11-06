package de.tsl2.nano.h5.timesheet;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import org.anonymous.project.Charge;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.FlatBeanReader;
import de.tsl2.nano.core.ENV;

public class FBRImport implements BiConsumer<String[], Map<String, Object>> {
	public static final String MTD_DOIMPORTHUMANREADABLE = "doImportHumanReadable";
	
	public static Collection<Charge> doImportHumanReadable(String file) {
		FlatBeanReader<Charge> reader = new FlatBeanReader<>(Charge.class, 
				ENV.get("timesheet.import.expression", "fromdate: fromtime-totime (pause^\\)\\:? ^value chargeitem comment"));
		Collection<Charge> beans = reader.read(file);
		if (BeanContainer.isInitialized())
			beans.forEach(b -> BeanContainer.instance().save(b));
		return beans;
	}

	@Override
	public void accept(String[] values, Map<String, Object> properties) {
		// TODO Auto-generated method stub
		
	}
	
	
}
