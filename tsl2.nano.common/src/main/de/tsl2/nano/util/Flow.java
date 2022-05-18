package de.tsl2.nano.util;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/** 
 * {@link Flow} as simplest workflow base using implementations of {@link ITask}.
 * It is the maker. going recursively through all tasks until end. experimental implementation
 * having only one class file with inner classes and a fat interface<p/> 
 * 
 * As {@link AFunctionalTask} is a base implementation of {@link ITask}, the {@link CTask}<br/>
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
@SuppressWarnings({"unchecked", "rawtypes"})
public class Flow {
	public static final String FILE_EXT = ".gra";
	protected static Class<? extends ITask> DEFAULT_TASK_TYPE = BeanClass.load(System.getProperty("tsl2nano.flow.default.task.type", CTask.class.getName()));
	protected static final String NAME_START = "START";
	protected static final String NAME_END = "END";
	protected static final String GRVT_LABEL_START = " [label=\"";
	protected static final String GRVT_LABEL_END = "\"]";

	String name;
	ITask start;
	List<Consumer<ITask>> listeners = new LinkedList<>();

	public void setTasks(ITask start) {
		this.start = start;
	}
	
	public void persist() {
		persist(FileUtil.userDirFile(name + FILE_EXT));
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
		return load(new Flow(), gravitoFile, taskType);
	}
	public static <F extends Flow> F load(F flow, File gravitoFile, Class<? extends ITask> taskType) {
		Scanner sc = Util.trY( () -> new Scanner(gravitoFile));
		flow.name = StringUtil.substring(FileUtil.replaceToJavaSeparator(gravitoFile.getPath()), "/", ".", true);
		ITask task = null;
		Map<String, ITask> tasks = new HashMap<>();
//		tasks.put(ITask.END.name(), ITask.END);
		Deque<String> strTasks = new LinkedList<>();
		String line;
		while (sc.hasNextLine()) {
			if (!(line = sc.nextLine()).isEmpty() && !line.trim().startsWith("#"))
				strTasks.add(line);
		}
		// create from end...
		while (!strTasks.isEmpty()) {
//			task = BeanClass.createInstance((Class<? extends ITask>)(taskType != null ? taskType : DEFAULT_TASK_TYPE));
			task = taskType != null ? BeanClass.createInstance(taskType) : new CTask();
			//TODO: thats nonsense - we throw the new instance away...
			task = task.fromGravString(strTasks.pollLast(), tasks);
			tasks.put(task.name(), task);
		}
		flow.setTasks(task);
		return flow;
	}
	
	public String getName() {
		return name;
	}
	
	public Deque<ITask> run(Map<String, Object> context) {
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
	public void addListener(Consumer<ITask> l) {
		listeners.add(l);
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
		return buildString(start, new StringBuilder()).toString().equals(f.buildString(f.start, new StringBuilder()).toString());
	}
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("usage: Flow <gravito-flow-file> [key1=value1 [key2=value2]...]\n\ttries to load a same named property file");
			return;
		}
		Flow flow = Flow.load(new File(args[0]), CTask.class);
		Properties p = FileUtil.loadProperties(args[0] + ".properties");
		MapUtil.fill(p, args, 1, "=");
		flow.run(new HashMap<>((Map)Util.untyped(p)));
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
		void setCondition(String condition);
		default ITask fromGravString(String line, Map<String, ITask> tasks) {
			String[] t = StringUtil.splitFix(line, false, " ", " -> ", GRVT_LABEL_START, GRVT_LABEL_END);
			if (!line.contains("[label")) {
				t[3] = t[4] = null;
			}
			ITask task2 = tasks.get(t[2]);
			if (task2 == null)
				task2 = t[2].equals(NAME_END) ? ITask.END : createTask(t[2], t[3], null);
			else
				task2.setCondition(t[3]);
			ITask task1 /*= tasks.remove(t[0]);
			if (task1 == null)
				task1 */= t[0].equals(NAME_START) ? createStart(task2) : createTask(t[0], null, StringUtil.substring(t[1], "(", ")"));
			if (!task1.name().equals(NAME_START))
				task1.addNeighbours(task2);
			return task1;
		}

		default ITask createTask(String name, String condition, String status) { throw new UnsupportedOperationException(); }
		public static ITask createStart(final ITask...tasks) {
			return new ITask() {
				@Override public String name() { return NAME_START; }
				@Override public boolean isStart() { return true; }
				@Override public void addNeighbours(ITask...tasks) {}
				@Override public void setCondition(String condition) {}
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
			String condition;
			@Override public String name() { return NAME_END; }
			@Override public List<ITask> next() { return Arrays.asList(); }
			@Override public boolean isEnd() { return true; }
			@Override public void addNeighbours(ITask...tasks) {}
			@Override public String gravCondition() { return condition != null ? GRVT_LABEL_START + condition + GRVT_LABEL_END : ""; }
			@Override public void setCondition(String condition) { this.condition = condition;}
			@Override public String toString() { return name();
			}
		};
		
	}

	public static abstract class ATask implements ITask {
		protected String condition;
		protected String expression;
		private Predicate<Map> fctCondition;
		private Function<Map, ?> fctFunction;
		private Status status = Status.NEW;
		private List<ITask> neighbours = new LinkedList<>();

		protected ATask() {}
		
		public ATask(String condition, String expression) {
			this.condition = condition;
			this.expression = expression;
		}
		@Override
		public String name() {
			return expression;
		}

		@Override
		public List<ITask> next() {
			return neighbours;
		}
		@Override
		public Status status() {
			return status;
		}
		public String gravCondition() {
			return GRVT_LABEL_START + condition + GRVT_LABEL_END;
		}
		@Override
		public void setCondition(String condition) {
			this.condition = condition;
		}
		@Override
		public boolean canActivate(Map<String, Object> context) {
			status = Status.ASK;
			if (fctCondition == null)
				fctCondition  = getFctCondition(condition);
			return fctCondition.test(context);
		}
		protected abstract Predicate<Map> getFctCondition(String condition);
		
		@Override
		public Object activate(Map<String, Object> context) {
			status = Status.RUN;
			Object result = null;
			try {
				if (fctFunction == null)
					fctFunction = getFctFunction(expression);
				result = fctFunction.apply(context);
				status = Status.OK;
			} catch (Exception ex) {
				status = Status.FAIL;
			}
			return result;
		}
		protected abstract Function<Map, ?> getFctFunction(String expression);

		@Override
		public void addNeighbours(ITask... tasks) {
			neighbours.addAll(Arrays.asList(tasks));
		}
		@Override
		public int hashCode() {
			return Objects.hash(condition, expression);
		}
		@Override
		public boolean equals(Object obj) {
			return hashCode() == obj.hashCode();
		}
		
		public ATask createTask(String name, String condition, String status) {
			ATask task = this.getClass().isMemberClass() && !Modifier.isStatic(this.getClass().getModifiers())
				? BeanClass.createInstance(this.getClass(), this, condition, name)
				: BeanClass.createInstance(this.getClass(), condition, name);
			if (status != null)
			task.status = Status.valueOf(status);
			return task;
		}
		@Override
		public String toString() {
			return asString();
		}
	}

	/** simple string-based task. context should match condition as string. expression has to point to a method. */
	public static class CTask extends ATask {
		protected CTask() {}
		
		/** Predicate as condition, FunctionalInterface as action */
		public CTask(String predicateClassName, String functionClassName) {
			super(predicateClassName, functionClassName);
		}

		@Override
		protected Predicate<Map> getFctCondition(String condition) {
			return BeanClass.createInstance(condition);
		}
		@Override
		protected Function<Map, ?> getFctFunction(String expression) {
			return BeanClass.createInstance(expression);
		}
	}

	/** simple http request task. condition should point to a rest-service with boolean response. expression to any rest service */
	public static class RTask extends ATask {
		protected RTask() {}
		
		public RTask(String urlCondition, String urlExpression) {
			super(urlCondition, urlExpression);
		}

		@Override
		protected Predicate<Map> getFctCondition(String condition) {
			return m -> new Boolean(NetUtil.getRest(condition, m, Boolean.class));
		}

		@Override
		protected Function<Map, ?> getFctFunction(String expression) {
			return m -> NetUtil.getRest(expression, m, Object.class);
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
