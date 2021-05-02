package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Date;

import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.serialize.YamlUtil;
import de.tsl2.nano.core.util.DefaultFormat;

public class YamlBeanTest {

	@Test
	public void testBeanDefinitionDumpAndLoadSimple() {
		checkDumpAndLoad(Reference.class, true);
	}

	@Test
	public void testBeanDefinitionDumpAndDefaultFormat() {
		checkDumpAndLoad(DefaultFormat.class, true);
	}

	@Test
	public void testBeanDefinitionDumpAndLoadTypeBean() {
//		System.setProperty("casc.yaml.max.aliases", "100");
		BeanDefinition beandef = checkDumpAndLoad(TypeBean.class, false); // reference id differ!
		
		IAttributeDefinition attrDate = beandef.getAttribute(TypeBean.ATTR_DATE);
		assertEquals(Date.class, attrDate.getConstraint().getType());
	}

	private BeanDefinition checkDumpAndLoad(Class<?> cls, boolean compare) {
		BeanDefinition beandef = BeanDefinition.getBeanDefinition(cls);
		String dump = YamlUtil.dump(beandef);
		System.out.println(dump);
		
		BeanDefinition beancopy = YamlUtil.load(dump, BeanDefinition.class);
		if (compare)
			assertEquals(maskId(dump), maskId(YamlUtil.dump(beancopy)));
		return beancopy;
	}

	private String maskId(String dump) {
		return dump.replaceAll("[&*]id\\d+.*", "XXXX");
	}

}

class Reference {
	Class type;
	String name;
	Date date;
	BigInteger number;
}
