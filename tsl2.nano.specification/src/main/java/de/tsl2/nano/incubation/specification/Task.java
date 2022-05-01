package de.tsl2.nano.incubation.specification;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Flow;
import de.tsl2.nano.core.util.Flow.ITask;

/**
 * Provides use of Flow and Tasks through specification items like rules and actions.
 * @author ts
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Task extends Flow.ATask {
	Predicate<Map> fctCondition;
	Function<Map, ?> fctFunction;

	/** WORKAROUND to use inner classes in Flow - may be refactored external classes */
	private static Flow flow;
	
	Task() {
		flow.super();
	}
	public Task(Flow flow, String conditionRule, String activationRule, List<ITask> neighbours) {
		flow.super(conditionRule, activationRule);
		Task.flow = flow;
	}
	@Override
	protected Predicate<Map> getFctCondition(String condition) {
		return m -> (Boolean)ENV.get(Pool.class).get(condition).run(m);
	}
	@Override
	protected Function<Map, ?> getFctFunction(String expression) {
		return m -> ENV.get(Pool.class).get(expression).run(m);
	}
}
