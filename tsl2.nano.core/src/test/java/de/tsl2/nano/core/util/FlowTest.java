package de.tsl2.nano.core.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Deque;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.Flow.ITask;
import de.tsl2.nano.core.util.Flow.CTask;

public class FlowTest  implements ENVTestPreparation {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ENVTestPreparation.setUp("core", false);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ENVTestPreparation.tearDown();
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
		CTask t1 = flow.new CTask("task1", ".*init=1.*") {
			protected java.util.function.Predicate<java.util.Map> getFctCondition(String condition) {
				return m -> m.toString().matches(condition);
			}
			protected java.util.function.Function<java.util.Map,?> getFctFunction(String expression) {
				return c -> c.put("init", 2);
			}
		};
		CTask t2 = flow.new CTask("task2", ".*init=2.*") {
			protected java.util.function.Predicate<java.util.Map> getFctCondition(String condition) {
				return m -> m.toString().matches(condition);
			}
			protected java.util.function.Function<java.util.Map,?> getFctFunction(String expression) {
				return null;
			}
		};
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
		
		File file = FileUtil.userDirFile("test.gra");
		flow.persist(file);
		
		Flow flow1 = Flow.load(file);
		assertEquals(flow, flow1);
	}
}
