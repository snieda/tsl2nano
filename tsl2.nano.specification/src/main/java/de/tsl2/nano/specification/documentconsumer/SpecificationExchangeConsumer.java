package de.tsl2.nano.specification.documentconsumer;

import java.io.File;
import java.util.function.Consumer;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.specification.SpecificationExchange;
import de.tsl2.nano.util.FilePath;

public class SpecificationExchangeConsumer implements Consumer<File> {

	@Override
	public void accept(File t) {
		FilePath.copy(t.getAbsolutePath(), ENV.getConfigPath() + SpecificationExchange.FILENAME_SPEC_PROPERTIES);
		ENV.get(SpecificationExchange.class).enrichFromSpecificationProperties();
	}

}
