package de.tsl2.nano.h5;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.actions.Action;
import de.tsl2.nano.specification.rules.Rule;
import de.tsl2.nano.specification.rules.RuleDecisionTable;
import de.tsl2.nano.specification.rules.RuleScript;

public class SpecifiedActionTest {
	
	@Before
	public void setUp() {
		NanoH5Test.createENV("specifiedaction");
    	Pool.registerTypes(Rule.class, RuleScript.class, RuleDecisionTable.class, Action.class);
	}

	@After
	public void tearDown() {
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testActionWithoutParameter() {
		TypeBean instance = new TypeBean();
		String name = "systemproperties";
		ENV.get(Pool.class).add(new Action(name, System.class, "getProperties", null));
		BeanDefinition<TypeBean> bean = Bean.getBean(instance).addAction(new SpecifiedAction(name, instance));
		
		bean.getAction(name).run();
	}

	@Test
	public void testActionWithParameters() {
		TypeBean instance = new TypeBean();
		instance.setString("user.home");
		String name = "systemproperty";
		ENV.get(Pool.class).add(new Action<>(name, System.class, "getProperty", null));
		BeanDefinition<TypeBean> bean = Bean.getBean(instance).addAction(new SpecifiedAction<>(name, instance));
		
		bean.getAction(name).setParameter("user.home").run();
	}

}
