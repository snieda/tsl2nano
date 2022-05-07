package de.tsl2.nano.incubation.specification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RuleDecisionTable;
import de.tsl2.nano.incubation.specification.rules.RuleScript;

public class WorkflowTest implements ENVTestPreparation{
	@Before
	public void setUp() throws Exception {
		ENVTestPreparation.super.setUp("specification");
    	Pool.registerTypes(Rule.class, RuleScript.class, RuleDecisionTable.class, Action.class);
		new TaskTest().testFlow();
	}
	@After
	public void tearDown() {
		//ENVTestPreparation.tearDown();
	}
	
	@Test
	public void testWorkflow() throws InterruptedException, ExecutionException {
		String flowFileName = ENV.getConfigPath() + "/test.gra";
		String queryName = null;
		Workflow workflow = new Workflow(Task.class.getName(), flowFileName, "0/2/6/SECONDS", queryName) {
			@Override
			protected Collection<Object> getData() {
				return Arrays.asList(new TypeBean(true));
			}
		};
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
		ScheduledFuture<?> future = workflow.activate();
		ConcurrentUtil.waitFor(() -> future.isCancelled() || future.isDone());
//		ConcurrentUtil.sleep(70000);
		assertTrue(future.isDone());
//		assertEquals("machnix", future.get());
	}

}
