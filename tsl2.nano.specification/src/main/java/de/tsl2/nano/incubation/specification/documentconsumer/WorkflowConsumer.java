package de.tsl2.nano.incubation.specification.documentconsumer;

import java.io.File;
import java.util.Scanner;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.incubation.specification.PFlow;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.Workflow;

public class WorkflowConsumer extends SimpleDocumentTag {

	@Override
	public void after(File t, String toBeConsumed) {
		String flowFileName = (String) ENV.get("flowfilename");
		flowFileName = createFlow(flowFileName, toBeConsumed);
		Workflow.main(new String[] {flowFileName, (String) ENV.get("schedule"), (String) ENV.get("queryname")});
	}

	private String createFlow(String flowFileName, String toBeConsumed) {
		Scanner sc = new Scanner(toBeConsumed);
		StringBuilder flow = new StringBuilder();
		boolean start = false;
		while(sc.hasNext()) {
			String l = sc.nextLine();
			if (start || l.trim().startsWith("START")) {
				start = true;
				flow.append(l + "\n");
				if (l.trim().endsWith("END"))
					break;
			}
		}
		String path = ENV.get(Pool.class).getDirectory(PFlow.class) + flowFileName;
		FileUtil.writeBytes(flow.toString().getBytes(), path, false);
		ENV.get(Pool.class).add(PFlow.load(FileUtil.userDirFile(path)));
		return path;
	}
}
