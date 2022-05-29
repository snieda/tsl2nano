package de.tsl2.nano.incubation.specification.documentconsumer;

import java.io.File;
import java.util.function.Consumer;

import de.tsl2.nano.bean.TransformableBeanReader;

public class FileImportConsumer implements Consumer<File> {

	@Override
	public void accept(File t) {
		TransformableBeanReader.importFrom(t.getAbsolutePath());
	}

}
