package de.tsl2.nano.core.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import de.tsl2.nano.core.cls.BeanClass;

/** 
 * {@link Flow} as simplest workflow base using implementations of {@link ITask}.
 * It is the maker. going recursively through all tasks until end. experimental implementation
 * having only one class file with inner classes and a fat interface<p/> 
 * 
 * As {@link ATask} is a base implementation of {@link ITask}, the {@link STask}<br/>
 * provides an extension to be used on functional implementation.<p/>
 * 
 * Each task has a name (may be equal to the action definition representation), a condition<br/>
 * for activation and the action itself.<p/>
 * 
 * To persist a flow with its task tree, a gravito state diagram file will be created.<br/>
 * This can be rendered by gravito inside a markdown file. So, a graphical representation is given.<p/>
 * 
 * This base implementation of a workflow is used and extended inside the module 'tsl2.nano.specification'
 * which provides conditions and actions as rules (through scripting, decision tables etc.) or specified actions. 
 * */
public class Flow {
	String name;
	ITask start;
	List<Consumer<ITask>> listeners = new LinkedList<>();

	public void setTasks(ITask start) {
		this.start = start;
	}
	
	public void persist() {
		persist(FileUtil.userDirFile(name + "gra"));
	}
	public void persist(File gravitoFile) {
		StringBuilder buf = new StringBuilder();
		buildString(start, buf);
		Util.trY( () -> Files.write(Paths.get(gravitoFile.getPath()), buf.toString().getBytes()));
	}
	
	private StringBuilder buildString(ITask root, StringBuilder buf) {
		buf.append(root.asString() + "\n");
		root.next().forEach(t -> buildString(t, buf));
		return buf;
	}

	public static Flow load(File gravitoFile) {
		return load(gravitoFile, null);
	}
	public static Flow load(File gravitoFile, Class<? extends ITask> taskType) {
		Scanner sc = Util.trY( () -> new Scanner(gravitoFile));
		Flow flow = new Flow();
		ITask task;
		Map<String, ITask> tasks = new HashMap<>();
		tasks.put(ITask.END.name(), ITask.END);
		Deque<String> strTasks = new LinkedList<>();
		String line;
		while (sc.hasNextLine()) {
			if (!(line = sc.nextLine()).isEmpty())
			strTasks.add(line);
		}
		// create from end...
		while (!strTasks.isEmpty()) {
			task = taskType != null ? BeanClass.createInstance(taskType) : flow.new STask();
			task.fromGravString(strTasks.pollLast(), tasks);
		}
		return flow;
	}
	public Deque<ITask> process(Map<String, Object> context) {
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
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Flow))
			return false;
		Flow f = (Flow) obj;
		return buildString(start, new StringBuilder()).toString().equals(f.buildString(start, new StringBuilder()).toString());
	}
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("usage: Flow <gravito-flow-file>");
			return;
		}
		Flow flow = Flow.load(new File(args[0]), STask.class);
		flow.process(new HashMap<>());
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
		default ITask fromGravString(String line, Map<String, ITask> tasks) {
			// TODO: how to persist/restore action expression
			String[] t = StringUtil.splitFix(line, " ", " -> ", " [label=\"", "\"]");
			ITask task = createTask(t);
			if (tasks.containsKey(t[2]))
				task.addNeighbours(tasks.get(t[2]));
			return task;
		}

		default ITask createTask(String[] t) { throw new UnsupportedOperationException(); }
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
			@Override public List<ITask> next() { return Arrays.asList(); }
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

		protected ATask() {
		}
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
		public boolean equals(Object obj) {
			if (!(obj instanceof ATask))
				return false;
			ATask o = (ATask) obj;
			return name.equals(o.name) && condition.equals(o.condition);
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
		protected STask() {
		}
		
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
		public STask createTask(String[] t) {
			STask task = BeanClass.isPublicClass(t[0]) && BeanClass.isPublicClass(t[3]) 
					? new STask(t[0], t[3])
					: new STask(t[0], t[3], null);
			return task;
		}
	}

	/** simple http request task. condition should point to a rest-service with boolean response. expression to any rest service */
	public class RTask extends ATask {
		protected RTask() {}
		
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
