package de.tsl2.nano.incubation.specification;

import java.io.BufferedWriter;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
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
	transient Class<? extends ITask> flowClass;
	
	transient Collection<Flow> flows = new LinkedList<>();
	
	protected Workflow() {}

	public Workflow(String flowType, String flowFileName, String schedule, String queryName) {
		super();
		this.flowFileName = flowFileName;
		this.flowType = flowType;
		this.schedule = schedule;
		this.queryName = queryName;
		init();
	}

	private void init() {
		flowClass = BeanClass.load(flowType);
		String[] time = schedule.split("[/]");
		SchedulerUtil.runAt(Integer.valueOf(time[0]), Integer.valueOf(time[1]), Integer.valueOf(time[2]), TimeUnit.valueOf(time[3]), this);
	}

	@Override
	public void run() {
		IPRunnable query = ENV.get(Pool.class).get(queryName);
		Collection<Object> items = (Collection<Object>) query.run(context);
		items.forEach(i -> {
			Map<String, Object> flowContext = BeanUtil.toValueMap(i);
			Flow flow = Flow.load(new File(flowFileName), flowClass);
			flow.addListener(new TaskListener(i, System.currentTimeMillis()));
			flows.add(flow);
			flow.process(flowContext);
		});
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
	}
	
}