package de.tsl2.nano.h5.timesheet;

import static de.tsl2.nano.service.util.finder.Finder.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.anonymous.project.Charge;
import org.anonymous.project.Chargeitem;
import org.anonymous.project.Item;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.TransformableBeanReader;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.service.util.finder.Finder;

public class FBRImport implements BiConsumer<String[], Map<String, Object>>, BiFunction<Bean, Map<String, Object>, Bean> {
	public static final String MTD_DOIMPORTHUMANREADABLE = "doImportHumanReadable";
	
	public static Collection<Charge> doImportHumanReadable(String file) {
		TransformableBeanReader<Charge> reader = new TransformableBeanReader<>(Charge.class, 
				ENV.get("timesheet.import.expression", "fromdate: fromtime-totime (pause^\\)\\:? ^value chargeitem comment"));
		Collection<Charge> beans = reader.read(file);
		Message.send("persisting " + beans.size() + " items");
		if (BeanContainer.isInitialized())
			beans.forEach(b -> BeanContainer.instance().save(b));
		Message.send("finished successfull (" + beans.size() + " items imported");
		return beans;
	}

	@Override
	public void accept(String[] values, Map<String, Object> properties) {
		//nothing to transform...only to show the use of a transformer
	}

	@Override
	public Bean apply(Bean b, Map<String, Object> properties) {
		Message.send("importing " + b);
		Map messages = new HashMap<>();
		if (!b.isValid(messages )) {
			Message.send(messages.toString());
			return null;
		}
		String itemName = String.valueOf(properties.get("item"));
		Item itemEx = new Item();
		itemEx.setName(itemName);
		Item item = Finder.findOne(example(itemEx));
		Chargeitem chargeitem = (Chargeitem) b.getValue("chargeitem");
		BeanContainer.createId(chargeitem);
		chargeitem.setCharge(BigDecimal.ZERO);
		//NOTE: Charge.chargeitem must be cascdatetype.all
		item.getChargeitems().add(chargeitem);
		Bean.getBean(chargeitem).setValue("item", item);
		return b;
	}
}
