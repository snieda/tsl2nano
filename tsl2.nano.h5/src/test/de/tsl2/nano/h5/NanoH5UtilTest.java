package de.tsl2.nano.h5;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.Pool;

public class NanoH5UtilTest implements ENVTestPreparation {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ENVTestPreparation.setUp("h5", NanoH5UtilTest.class, false);
		NanoH5.registereExpressionsAndPools();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
//		ENVTestPreparation.tearDown();
	}
	
	@Test
	public void testEmptySpecificationProperties() {
		BeanDefinition bean = BeanDefinition.getBeanDefinition(TypeBean.class);
		Properties p = new Properties();
		BeanUtil.saveSpecificationEntries(bean, p);
		
		String file = ENV.getConfigPath() + BeanUtil.FILENAME_SPEC_PROPERTIES;
		assertTrue(FileUtil.userDirFile(file).exists());
		assertEquals(95, p.size());
		
		Pool pool = ENV.get(Pool.class);
		pool.add("%testrule", "TEST");
		Properties pp = new Properties();
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			String key = entry.getKey().toString();
			String val = entry.getValue().toString();
			
			String allnames = StringUtil.toString(bean.getAttributeNames(), -1).replace(" ", "");
			allnames = allnames.substring(1, allnames.length() - 1);
			pp.put(key.substring(1), 
					val.replace("<rule>", "%testrule")
					.replace("<attribute names comma or space separated>", allnames)
					.replace("<comma-separated-list-of-observable-attribute-names>", allnames)
					.replace("<path like 'presentable.layoutconstaints'>", "presentable.layoutconstaints")
					);
		}
		FileUtil.saveProperties(file, pp);
		
		int errors = NanoH5Util.enrichFromSpecificationProperties();
		
//		assertTrue(FileUtil.userDirFile(ENV.getTempPath() + BeanUtil.FILENAME_SPEC_PROPERTIES).exists());
		assertFalse(FileUtil.userDirFile(ENV.getTempPath() + BeanUtil.FILENAME_SPEC_PROPERTIES).exists());
		assertTrue(FileUtil.userDirFile(file + ".done").exists());
		assertEquals(0, errors);
		
		assertTrue(bean.getAttribute("testrule") != null);
		assertTrue(bean.getAction("%testrule") != null);
		assertTrue(Proxy.isProxyClass(bean.getAttribute("string").getPresentation().getClass()));
	}

}
