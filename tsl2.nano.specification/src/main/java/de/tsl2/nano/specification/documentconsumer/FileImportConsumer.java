package de.tsl2.nano.specification.documentconsumer;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.TransformableBeanReader;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;

public class FileImportConsumer implements Consumer<File> {

	@Override
	public void accept(File t) {
		Collection<Object> beans = TransformableBeanReader.importFrom(t.getAbsolutePath());
		Message.send("persisting " + beans.size() + " items");		if (BeanContainer.isInitialized())
		if (BeanContainer.isInitialized()) 
			beans.forEach(b -> {
				try {
					BeanContainer.instance().save(b);
				} catch (Exception ex) {
					if (!Message.ask("ERROR on " + b + "!\n\nAbort?", true))
						ManagedException.forward(ex);
				}
			});
		Message.send("finished successfull (" + beans.size() + " items imported");
	}

}
