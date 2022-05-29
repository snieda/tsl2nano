package de.tsl2.nano.incubation.specification.documentconsumer;

import java.io.File;
import java.util.Scanner;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.PFlow;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RuleDecisionTable;
import de.tsl2.nano.incubation.specification.rules.RuleScript;
import de.tsl2.nano.util.FilePath;

public class SimpleDocumentTag implements Consumer<File> {
	private static final Log LOG = LogFactory.getLog(SimpleDocumentTag.class);

	public SimpleDocumentTag() {
		if (!Pool.hasRegisteredTypes())
	    	Pool.registerTypes(Rule.class, RuleScript.class, RuleDecisionTable.class, Action.class, PFlow.class);
	}
	
	@Override
	public void accept(File f) {
		LOG.info("consuming " + f);
		before(f);
		String content = new String(FilePath.read(f.getAbsolutePath()));
		Scanner sc = Util.trY( () -> new Scanner(f));
		while (sc.hasNextLine()) {
			String l = sc.nextLine();
			if (l.trim().isEmpty() || l.startsWith("#"))
				continue;
			else if (l.contains("="))
				define(l);
			else if (l.trim().startsWith(">>")) 
				run(l);
			else
				consumeSpecific(l);
		}
		after(f);
	}

	protected void before(File f) {
	}

	protected void after(File f) {
	}

	protected void consumeSpecific(String l) {
		LOG.info("emtpy consumeSpecific called");
	}

	private void run(String l) {
		String action = StringUtil.substring(l, ">>", null);
		ENV.get(Pool.class).get(action).run(ENV.getProperties());
	}

	private void define(String l) {
		String key = StringUtil.substring(l, null, "=");
		String val = StringUtil.substring(l, "=", null);
		
		Pool pool = ENV.get(Pool.class);
		if (key.matches(pool.getFullExpressionPattern())) {
			pool.add(key, val);
		} else {
			ENV.setProperty(key, val);
		}
	}
	
}
