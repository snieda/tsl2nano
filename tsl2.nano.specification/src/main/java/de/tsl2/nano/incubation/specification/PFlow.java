package de.tsl2.nano.incubation.specification;

import java.io.File;
import java.io.Serializable;
import java.util.Deque;
import java.util.Map;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.util.Flow;
import de.tsl2.nano.util.Flow.ITask;

/**
 * Extends the basis {@link Flow} to be usable in the {@link Pool}.
 * 
 * @author ts
 */
@Default(value = DefaultType.FIELD, required = false)
public class PFlow extends Flow implements IPRunnable<Deque<ITask>, Map<String, Object>> , IPrefixed {

	public static PFlow load(File gravitoFile, Class<? extends ITask> taskType) {
		return Flow.load(new PFlow(), gravitoFile, taskType);
	}

	@Override
	public Deque<ITask> run(Map<String, Object> context, Object... extArgs) {
		return super.run(context);
	}

	@Override
	public Map<String, ? extends Serializable> getParameter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> checkedArguments(Map<String, Object> args, boolean strict) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String prefix() {
		return ">";
	}
	
}
