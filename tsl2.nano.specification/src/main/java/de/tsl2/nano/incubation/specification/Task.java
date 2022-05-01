package de.tsl2.nano.incubation.specification;

import java.util.List;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Flow;
import de.tsl2.nano.core.util.Flow.ITask;

/**
 * Provides use of Flow and Tasks through specification items like rules and actions.
 * @author ts
 *
 */
public class Task extends Flow.AFunctionalTask {

	/** WORKAROUND to use inner classes in Flow - may be refactored external classes */
	private static Flow flow;
	
	Task() {
		flow.super();
	}
	@SuppressWarnings("unchecked")
	public Task(Flow flow, String conditionRule, String activationRule, List<ITask> neighbours) {
		flow.super(conditionRule, activationRule, 
				m -> (Boolean)ENV.get(Pool.class).get(conditionRule).run(m), 
				m -> ENV.get(Pool.class).get(activationRule).run(m));
		Task.flow = flow;
	}
	@Override
	public ITask createTask(String[] t) {
		return new Task(flow, t[3], t[0], null);
	}
}
