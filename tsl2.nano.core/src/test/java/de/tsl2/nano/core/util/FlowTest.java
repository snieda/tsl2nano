package de.tsl2.nano.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Deque;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.Flow.ITask;
import de.tsl2.nano.core.util.Flow.STask;

public class FlowTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testProcess() {
		Flow flow = new Flow();
		ITask start = createTasks(flow);
		flow.listeners.add(t -> System.out.println(t));
		Deque solved = flow.process(MapUtil.asMap("init", 1));
		flow.isSuccessfull(solved);
	}

	private ITask createTasks(Flow flow) {
		STask t1 = flow.new STask("task1", ".*init=1.*", c -> c.put("init", 2));
		STask t2 = flow.new STask("task2", ".*init=2.*", null);
		ITask start = ITask.createStart(t1);
		t1.addNeighbours(t2);
		t2.addNeighbours(ITask.END);
		flow.setTasks(start);
		return start;
	}

	@Test
	public void testSaveAndLoad() {
		Flow flow = new Flow();
		createTasks(flow);
		
		File file = FileUtil.userDirFile("target/test.gra");
		flow.persist(file);
		
		Flow flow1 = Flow.load(file);
		assertEquals(flow, flow1);
	}
}
