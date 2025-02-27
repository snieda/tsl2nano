package de.tsl2.nano.bean.def;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.autotest.BaseTest;
import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;

//@Ignore
public class AttributeCoverTest implements ENVTestPreparation {
	static final String MYRULECOVERVALUE = "MYRULECOVERVALUE";

    @Before
    public void setUp() {
		if (BaseTest.isExternalCIPlatform())
			return;
    	ConcurrentUtil.sleep(20000); // to not compromise other bean tests
		ENVTestPreparation.super.setUp("descriptor");
    }

    @After
    public void tearDown() {
		if (BaseTest.isExternalCIPlatform())
			return;
    	Bean.clearCache();
    	ENVTestPreparation.tearDown();
    }
    
//    @Ignore("Must be done soon...")
	@Test
	public void testCache() {
		if (BaseTest.isExternalCIPlatform())
			return;
		TypeBean instance = new TypeBean();
		String attrName = "string";
		String path = "presentable.layoutConstraints";

		//TODO: implement a check on AttributeCover to have a delegator with interfaces!
		
		createCover(instance, attrName, path);

		Bean<TypeBean> bean = Bean.getBean(instance);
		assertTrue(bean.getAttribute(attrName).hasRuleCover());

		AttributeCover.removeCover(instance.getClass(), attrName, path);
		assertFalse(bean.getAttribute(attrName).hasRuleCover());
	}

	private void createCover(TypeBean instance, String attrName, String path) {
		//prepare the delegation instance of type Presentable to let the proxy know its interfaces
		BeanDefinition<? extends TypeBean> beanDef = BeanDefinition.getBeanDefinition(instance.getClass());
		IPresentable presentable = beanDef.getAttribute(attrName).getPresentation();
		assertTrue(presentable != null);
		
		AttributeCover cover = AttributeCover.cover(MyRuleCover.class, instance.getClass(), attrName, path, "myrule");
		assertTrue(cover.getInterfaces() != null);
	}

}

class MyRuleCover<T> extends AttributeCover<T> {

	public MyRuleCover() {
		super();
	}

	public MyRuleCover(String name, Map<String, String> rules) {
		super(name, rules);
	}

	@Override
	public Object eval(String propertyPath) {
		return AttributeCoverTest.MYRULECOVERVALUE;
	}

	@Override
	protected boolean checkRule(String ruleName) {
		return true;
	}
	
}