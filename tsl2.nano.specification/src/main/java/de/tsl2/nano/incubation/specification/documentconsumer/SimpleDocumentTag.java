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
		Scanner sc = Util.trY( () -> new Scanner(f));
		String l;
		StringBuilder toBeConsumed = new StringBuilder();
		while (sc.hasNextLine()) {
			l = sc.nextLine();
			if (l.trim().isEmpty() || l.startsWith("#"))
				continue;
			else if (l.contains("=") && StringUtil.substring(l.substring(1), null, "=").matches("\\w+"))
				define(l);
			else if (l.trim().startsWith(">>")) 
				toBeConsumed.append(run(l) + "\n");
			else
				toBeConsumed.append(consumeSpecific(l) + "\n");
		}
		after(f, toBeConsumed.toString());
	}

	protected void before(File f) {
	}

	protected void after(File f, String toBeConsumed) {
	}

	protected String consumeSpecific(String l) {
		LOG.info("emtpy consumeSpecific called");
		return l;
	}

	private String run(String l) {
		String action = StringUtil.substring(l, ">>", null);
		return String.valueOf(ENV.get(Pool.class).get(action).run(ENV.getProperties()));
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
