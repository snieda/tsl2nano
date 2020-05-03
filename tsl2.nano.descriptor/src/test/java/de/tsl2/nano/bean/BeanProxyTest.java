package de.tsl2.nano.bean;

import static org.junit.Assert.*;

import org.junit.Test;

import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.util.test.TypeBean;

public class BeanProxyTest {

	@Test
	public void testPersistingBeanProxy() {
		TypeBean instance = new TypeBean();
		BeanDefinition beandef = BeanDefinition.getBeanDefinition(instance.getClass());
		IAttributeDefinition attr = beandef.getAttribute("string");
		new PrivateAccessor(attr).set("constraint", BeanProxy.createBeanImplementation(IConstraint.class));
		attr.getConstraint().setLength(99);
		beandef.saveDefinition();
		
		beandef.clearCache();
		
		beandef = BeanDefinition.getBeanDefinition(instance.getClass());
		assertEquals(99, beandef.getAttribute("string").getConstraint().getLength());
	}

}
