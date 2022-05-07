package de.tsl2.nano.incubation.specification;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.Flow;

/**
 * Provides use of Flow and Tasks through specification items like rules and actions.
 * @author ts
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Task extends Flow.ATask {

	Task() {
		new Flow().super();
	}
	public Task(String conditionRule, String activationRule) {
		new Flow().super(conditionRule, activationRule);
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
