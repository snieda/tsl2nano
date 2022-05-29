package de.tsl2.nano.incubation.specification.documentconsumer;

import java.io.File;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.incubation.specification.Workflow;

public class WorkflowConsumer extends SimpleDocumentTag {

	@Override
	public void after(File t) {
		Workflow.main(new String[] {(String) ENV.get("flowfilename"), (String) ENV.get("schedule"), (String) ENV.get("queryname")});
	}
}
