package de.tsl2.nano.core.util;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Test;

import de.tsl2.nano.core.cls.BeanClass;

/** the maker. goes recursively through all tasks until end. */
public class Flow {
	String name;
	List<Consumer<ITask>> listeners = new LinkedList<>();
	
	public Deque<ITask> process(ITask start, Map<String, Object> context) {
		assert start.isStart();
		LinkedList<ITask> solved = new LinkedList<>();
		flow(start, context, solved);
		return solved;
	}

	private void flow(ITask current, Map<String, Object> context, List<ITask> solved) {
		if (current.canActivate(context)) {
			Object result = current.activate(context);
			context.put(current.name(), result);
			listeners.forEach(l -> l.accept(current));
			solved.add(current);
			if (current.isStart() || current.status().equals(ITask.Status.OK)) {
				for (ITask t : current.next()) {
					flow(t, context, solved);
				}
			}
		}
	}
	public boolean isSuccessfull(Deque<ITask> solved) {
		return solved.getLast().isEnd();
	}
	
	public boolean isUnConditioned(Deque<ITask> solved) {
		return !solved.getLast().isEnd() && solved.getLast().status().equals(ITask.Status.OK);
	}
	public boolean isFailed(Deque<ITask> solved) {
		return solved.getLast().status().equals(ITask.Status.FAIL);
	}
	@Test
	public static void main(String[] args) {
		Flow flow = new Flow();
		STask t1 = flow.new STask("task1", ".*init=1.*", c -> c.put("init", 2));
		STask t2 = flow.new STask("task2", ".*init=2.*", null);
		ITask start = ITask.createStart(t1);
		t1.addNeighbours(t2);
		t2.addNeighbours(ITask.END);
		flow.listeners.add(t -> System.out.println(t));
		Deque solved = flow.process(start, MapUtil.asMap("init", 1));
		flow.isSuccessfull(solved);
	}
	/** base definition to do a simple workflow */
	public interface ITask {
		enum Status {NEW, ASK, RUN, FAIL, OK}
		
		String name();
		default boolean canActivate(Map<String, Object> context) { return !context.containsValue(this); }
		default Object activate(Map<String, Object> context) { return context.put(name(), this); }
		default Status status() { return Status.NEW; }
		default List<ITask> next() { return Arrays.asList(END); }
		void addNeighbours(ITask...tasks);
		
		default boolean isStart() { return false; }
		default boolean isEnd() { return false; }

		default String asString() { // we can't override toString() in an interface or not-public class
			StringBuilder buf = new StringBuilder();
			next().forEach(n -> buf.append(name() + " (" + status() + ")" + " -> " + n.name() + n.gravCondition()));
			return buf.toString();
		}

		default String gravCondition() {
			return "";
		}
		public static ITask createStart(final ITask...tasks) {
			return new ITask() {
				@Override public String name() { return "START"; }
				@Override public boolean isStart() { return true; }
				@Override public void addNeighbours(ITask...tasks) {}
				@Override
				public List<ITask> next() {
					return Arrays.asList(tasks);
				}
				@Override
				public Status status() {
					return ITask.Status.OK;
				}
				@Override
				public String toString() {
					return asString();
				}
			};
		}
		static final ITask END = new ITask() {
			@Override public String name() { return "END"; }
			@Override public boolean isEnd() { return true; }
			@Override public void addNeighbours(ITask...tasks) {}
			@Override
			public String toString() {
				return name();
			}
		};
		
	}

	@SuppressWarnings("rawtypes")
	public class ATask implements ITask {
		private String name;
		private Predicate<Map> condition;
		private Function<Map, ?> function;
		private Status status = Status.NEW;
		private List<ITask> neighbours;

		public ATask(String name, Predicate<Map> condition, Function<Map, ?> function, List<ITask> neighbours) {
			this.name = name;
			this.condition = condition;
			this.function = function;
			this.neighbours = neighbours != null ? neighbours : new LinkedList<>();
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean canActivate(Map<String, Object> context) {
			status = Status.ASK;
			return condition.test(context);
		}
		
		@Override
		public Object activate(Map<String, Object> context) {
			status = Status.RUN;
			Object result = null;
			try {
				result = function.apply(context);
				status = Status.OK;
			} catch (Exception ex) {
				status = Status.FAIL;
			}
			return result;
		}
		
		@Override
		public List<ITask> next() {
			return neighbours;
		}
		@Override
		public Status status() {
			return status;
		}
		@Override
		public void addNeighbours(ITask... tasks) {
			neighbours.addAll(Arrays.asList(tasks));
		}
		
		@Override
		public String toString() {
			return asString();
		}
		
	}
	/** simple string-based task. condition should match context as string. expression has to point to a method. */
	public class STask extends ATask {
		String condition; //for logging output
		String expression;
		/** Predicate as condition, FunctionalInterface as action */
		public STask(String predicateClassName, String functionClassName) {
			super(BeanClass.load(functionClassName).getSimpleName(), 
					m -> ((Predicate<Map>)BeanClass.createInstance(predicateClassName)).test(m),
					m -> ((Function<Map, ?>)BeanClass.createInstance(functionClassName)).apply(m), 
					null);
			this.condition = predicateClassName;
			this.expression = "@" + functionClassName;
		}
		/** direct function implementation -> not persistable! */
		public STask(String name, String condition, Function<Map, ?> function) {
			super(name, m -> m.toString().matches(condition), function, null);
			this.condition = condition;
		}
		public String gravCondition() {
			return " [label=\"" + condition + "\"]";
		}
		public STask fromGravString(String line) {
			// TODO: how to persist/restore action expression
			String[] t = StringUtil.splitFix(line, " ", " -> ", " ", "[label=\"", "\"]");
			return new STask(t[0], t[3]);
		}
	}

	/** simple http request task. condition should point to a rest-service with boolean response. expression to any rest service */
	public class RTask extends ATask {
		public RTask(String name, String urlCondition, String urlExpression) {
			super(name, m -> new Boolean(NetUtil.getRest(urlCondition, m, Boolean.class)), m -> NetUtil.getRest(urlExpression, m, Object.class), null);
		}
	}
	public class RegExMatchContext implements Predicate<Map> {
		private String condition;

		RegExMatchContext(String condition) {
			this.condition = condition;
		}
		@Override
		public boolean test(Map m) {
			return m.toString().matches(condition);
		}
		
	}
}
