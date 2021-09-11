package de.tsl2.nano.incubation.specification;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.Flow;
import de.tsl2.nano.core.util.Flow.ITask;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.rules.Rule;

public class TaskTest {

	@Test
	public void testFlow() throws Exception {
		Pool pool = new Pool();
		pool.add(new Rule<>("test", "isnix", null));
		pool.add(new Action<>(TaskTest.class.getMethod("myAction", new Class[0])));
		
		Flow flow = new Flow();
		Task task = new Task(flow, "test", "myAction", null);
		ITask.createStart(task);
		task.addNeighbours(ITask.END);
		flow.setTasks(task);
		
		File file = FileUtil.userDirFile("target/test.gra");
		flow.persist(file);
		
		Flow flow1 = Flow.load(file, Task.class);
		assertEquals(flow, flow1);
		
	}

	public String myAction() {
		return "machnix";
	}
}
