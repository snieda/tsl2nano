package de.tsl2.nano.incubation.specification;

import java.util.List;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Flow;
import de.tsl2.nano.core.util.Flow.ITask;
import de.tsl2.nano.core.util.StringUtil;

public class Task extends Flow.ATask {

	@SuppressWarnings("unchecked")
	public Task(Flow flow, String conditionRule, String activationRule, List<ITask> neighbours) {
		flow.super(activationRule, 
				m -> (Boolean)ENV.get(Pool.class).get(conditionRule).run(m), 
				m -> ENV.get(Pool.class).get(activationRule).run(m), 
				neighbours);
	}
	public Task fromGravString(Flow flow, String line) {
		String[] t = StringUtil.splitFix(line, " ", " -> ", " ", "[label=\"", "\"]");
		return new Task(flow, t[3], t[0], null);
	}
}
