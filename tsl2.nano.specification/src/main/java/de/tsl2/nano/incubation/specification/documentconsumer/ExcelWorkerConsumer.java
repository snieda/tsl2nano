package de.tsl2.nano.incubation.specification.documentconsumer;

import java.io.File;
import java.util.function.Consumer;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.ExcelWorker;

public class ExcelWorkerConsumer implements Consumer<File> {

	@Override
	public void accept(File t) {
		Util.trY( () -> ExcelWorker.generateFromCSV(t.getAbsolutePath()));
	}

}
