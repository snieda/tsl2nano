package de.tsl2.nano.h5;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.incubation.specification.Pool;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.util.test.TypeBean;

public class SpecifiedActionTest {
	
	@Before
	public void setUp() {
		ENVTestPreparation.setUp("h5", false);
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
		instance.setString("user.dir");
		String name = "systemproperty";
		ENV.get(Pool.class).add(new Action(name, System.class, "getProperty", null));
		BeanDefinition<TypeBean> bean = Bean.getBean(instance).addAction(new SpecifiedAction(name, instance));
		
		bean.getAction(name).setParameter(instance, "user.dir").run();
	}

}
