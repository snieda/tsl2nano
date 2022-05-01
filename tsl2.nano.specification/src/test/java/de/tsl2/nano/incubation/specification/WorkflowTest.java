package de.tsl2.nano.incubation.specification;

import static org.junit.Assert.*;

import org.junit.Test;

public class WorkflowTest {

	@Test
	public void testWorkflow() {
		String flowFileName = null;
		String queryName = null;
		new Workflow(Task.class.getName(), flowFileName, "0/10/100/SECONDS", queryName);
	}

}
