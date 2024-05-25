package de.tsl2.nano.specification;

import java.io.BufferedWriter;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.simpleframework.xml.Attribute;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FilePath;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.util.Flow;
import de.tsl2.nano.util.Flow.ITask;
import de.tsl2.nano.util.SchedulerUtil;

/**
 * workflow on a {@link Flow} starting through schedule expression (delay/period/end/TimeUnit) and loading data through given query rule. 
 * Each data row will go through flow of given type.
 * 
 * @author ts
 */
public class Workflow implements Runnable {
	@Attribute
	String flowType;
	@Attribute
	String flowFileName;
	@Attribute
	String schedule;
	@Attribute
	String queryOrFileName;
	Map<String, Object> context;
	
	transient Collection<Flow> flows = new LinkedList<>();
	
	protected Workflow() {}

	public Workflow(String flowFileName, String schedule, String queryOrFileName) {
		this(Task.class.getName(), flowFileName, schedule, queryOrFileName);
	}
	public Workflow(String flowType, String flowFileName, String schedule, String queryOrFileName) {
		super();
		this.flowFileName = flowFileName;
		this.flowType = flowType;
		this.schedule = schedule;
		this.queryOrFileName = queryOrFileName;
	}

	public ScheduledFuture<?> activate() {
		if (schedule == null || schedule.equals("now")) {
			run();
			return null;
		} else {
			String[] time = schedule.split("[/]");
			return SchedulerUtil.runAt(Integer.valueOf(time[0]), Integer.valueOf(time[1]), Integer.valueOf(time[2]), TimeUnit.valueOf(time[3]), this);
		}
	}

	@Override
	public void run() {
		Class<? extends ITask> flowClass = BeanClass.load(flowType);
		log("starting workflow [" + flowClass.getName() + "]");
		Collection<Object> items = getData();
		items.parallelStream().forEach(i -> {
			Thread.currentThread().setUncaughtExceptionHandler((t, e)  -> e.printStackTrace());
			Map<String, Object> flowContext = BeanUtil.toValueMap(i);
			Flow flow = Flow.load(new File(flowFileName), flowClass);
			flow.addListener(new TaskListener(i, System.currentTimeMillis()));
			flows.add(flow);
			flow.run(flowContext);
			FilePath.write(flowFileName + ".finished", MapUtil.toJSon(flowContext).getBytes());
		});
	}

	public static void main(String[] args) {
		if (args.length == 0 || (args.length == 1 && args[1].equals("--help"))) {
			log("usage: Workflow <flowfilename> <schedule|'now'> <queryname || -file[:beantypeclassname]=filename>" +
					"\n\twith: flowfilename: file with gravito mind map" +
					"\n\t      schedule    : delay/period/end/TimeUnit (TimeUnit=SECONDS, MINUTES, etc) or simply 'now'" +
					"\n\t      queryname   : query rule name or '-file'[:beantypeclassname]=<data-file-name>");
			return;
		}
		Workflow workflow = new Workflow(args[0], args[1], args[2]);
		String file = Pool.getSpecificationRootDir() + "workflow/" + new File(args[0]).getName() + ".workflow";
		FileUtil.userDirFile(file).getParentFile().mkdirs();
		ENV.save(file, workflow);
		workflow.activate();
	}
	protected Collection<Object> getData() {
		if (queryOrFileName.startsWith("-file="))
			return getFileItems(queryOrFileName, context);
		else
			return getQueryItems(queryOrFileName, context);
	}

	protected Collection<Object> getFileItems(String queryOrFileName, Map<String, Object> context) {
		return FileReader.getFileItems(queryOrFileName);
	}

	protected Collection<Object> getQueryItems(String queryOrFileName, Map<String, Object> context) {
		IPRunnable query = ENV.get(Pool.class).get(queryOrFileName);
		Collection<Object> items = (Collection<Object>) query.run(context);
		return items;
	}
	static final void log(Object obj) {
		System.out.println(obj);
	}
}

class TaskListener implements Consumer<ITask> {

	private BufferedWriter fileWriter;

	public TaskListener(Object i, long start) {
		Bean<Object> bean = Bean.getBean(i);
		String file = bean.getName().replace("[]", "Array");
		fileWriter = FilePath.getFileWriter(ENV.getConfigPath() + "/" + file + ".flow.log");
		Util.trY( () -> fileWriter.write("\nstarting flow at " + DateUtil.getFormattedTime(start) + "\n"));
	}

	@Override
	public void accept(ITask t) {
		Util.trY( () -> fileWriter.write(t.asString()));
		if (t.isEnd()) {
			Util.trY( () -> fileWriter.close());
		}
	}
	
}