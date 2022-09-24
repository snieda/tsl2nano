package de.tsl2.nano.specification;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.management.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.specification.PFlow;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.Task;
import de.tsl2.nano.specification.actions.Action;
import de.tsl2.nano.specification.rules.Rule;
import de.tsl2.nano.specification.rules.RuleDecisionTable;
import de.tsl2.nano.specification.rules.RuleScript;
import de.tsl2.nano.util.Flow;
import de.tsl2.nano.util.Flow.ITask;

public class TaskTest implements ENVTestPreparation {

	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("specification");
    	Pool.registerTypes(Rule.class, RuleScript.class, RuleDecisionTable.class, Action.class, PFlow.class);
	}
	@After
	public void tearDown() {
		ENVTestPreparation.tearDown();
	}
	
	@Test
	public void testFlow() throws Exception {
		Pool pool = new Pool();
		pool.add(new Rule<>("test", "1", null));
		pool.add(new Action<>(TaskTest.class.getMethod("myAction", new Class[0])));
		
		Flow flow = new Flow();
		Task task = new Task("§test", "!myAction");
		ITask start = ITask.createStart(task);
		task.addNeighbours(ITask.END);
		flow.setTasks(start);
		
		File file = FileUtil.userDirFile(ENV.getConfigPath() + "/test.gra");
		flow.persist(file);
		
		Flow flow1 = Flow.load(file, Task.class);
		assertEquals(flow, flow1);
		
	}

	public static String myAction() {
		return "machnix";
	}
}
