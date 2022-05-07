package de.tsl2.nano.incubation.specification;

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
import de.tsl2.nano.core.util.Flow;
import de.tsl2.nano.core.util.Flow.ITask;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.IPRunnable;
import de.tsl2.nano.util.FilePath;
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
	String queryName;
	Map<String, Object> context;
	
	transient Collection<Flow> flows = new LinkedList<>();
	
	protected Workflow() {}

	public Workflow(String flowType, String flowFileName, String schedule, String queryName) {
		super();
		this.flowFileName = flowFileName;
		this.flowType = flowType;
		this.schedule = schedule;
		this.queryName = queryName;
	}

	public ScheduledFuture<?> activate() {
		String[] time = schedule.split("[/]");
		return SchedulerUtil.runAt(Integer.valueOf(time[0]), Integer.valueOf(time[1]), Integer.valueOf(time[2]), TimeUnit.valueOf(time[3]), this);
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
			flow.process(flowContext);
		});
	}

	protected Collection<Object> getData() {
		IPRunnable query = ENV.get(Pool.class).get(queryName);
		Collection<Object> items = (Collection<Object>) query.run(context);
		return items;
	}
	void log(Object obj) {
		System.out.println(obj);
	}
}

class TaskListener implements Consumer<ITask> {

	private BufferedWriter fileWriter;

	public TaskListener(Object i, long start) {
		Bean<Object> bean = Bean.getBean(i);
		fileWriter = FilePath.getFileWriter(ENV.getConfigPath() + "/" + bean.getName() + "-" + bean.getId() + ".flow");
		Util.trY( () -> fileWriter.write("starting flow at " + DateUtil.getFormattedTime(start)));
	}

	@Override
	public void accept(ITask t) {
		Util.trY( () -> fileWriter.write(t.asString()));
		if (t.isEnd()) {
			Util.trY( () -> fileWriter.close());
		}
	}
	
}