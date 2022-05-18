package de.tsl2.nano.incubation.specification;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.util.Flow;

/**
 * Provides use of Flow and Tasks through specification items like rules and actions.
 * @author ts
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Task extends Flow.ATask {

	Task() {
		super();
	}
	public Task(String conditionRule, String activationRule) {
		super(conditionRule, activationRule);
	}
	@Override
	protected Predicate<Map> getFctCondition(String condition) {
		return m -> ObjectUtil.wrap(ENV.get(Pool.class).get(condition).run(m), Boolean.class);
	}
	@Override
	protected Function<Map, ?> getFctFunction(String expression) {
		return m -> ENV.get(Pool.class).get(expression).run(m);
	}
}
