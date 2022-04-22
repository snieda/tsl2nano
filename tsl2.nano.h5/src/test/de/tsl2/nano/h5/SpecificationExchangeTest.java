package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.Pool;
import static de.tsl2.nano.bean.def.SpecificationExchange.*;

public class SpecificationExchangeTest implements ENVTestPreparation {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ENVTestPreparation.setUp("h5", SpecificationExchangeTest.class, false);
		NanoH5.registereExpressionsAndPools();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
//		ENVTestPreparation.tearDown();
	}
	
	int getExpectedEntryCount(Class type, int ruleCount) {
		return BeanDefinition.getBeanDefinition(type).getAttributeNames().length * 3 
				+ ruleCount + 4;
	}

	@Test
	public void testSimpleSpecificationProperties() {
		Class<?> type = TypeBean.class;
		Properties p = initializeBeanSpecProperties(type, getExpectedEntryCount(type, 1));
		String file = fillSpecificationProperties(type, p);
		int errors = NanoH5Util.enrichFromSpecificationProperties();
		
		checkSpecificationDone(type, file, errors);
	}

	@Test
	public void testSimpleSpecificationCSV() {
		Class<?> type = TypeBean.class;
		Properties p = initializeBeanSpecProperties(type, getExpectedEntryCount(type, 1));
		String file = fillSpecificationProperties(type, p);
		FileUtil.delete(file); // -> the csv file will be used!
		
		file = new SpecificationH5Exchange().saveAsCSV(file + EXT_CSV, p);
		
		int errors = NanoH5Util.enrichFromSpecificationProperties();
		
		checkSpecificationDone(type, file, errors);
	}
	
	private void checkSpecificationDone(Class<?> type, String file, int errors) {
//		assertTrue(FileUtil.userDirFile(ENV.getTempPath() + BeanUtil.FILENAME_SPEC_PROPERTIES).exists());
		assertFalse(FileUtil.userDirFile(file).exists());
		assertTrue(file + ".done should exist", FileUtil.userDirFile(file + ".done").exists());
		assertEquals(0, errors);
		
		BeanDefinition<?> bean = BeanDefinition.getBeanDefinition(type);
		assertTrue(bean.getAttribute("testrule", false) != null);
		assertTrue(bean.getAction("%testrule") != null);
		assertTrue(Proxy.isProxyClass(bean.getAttribute("string").getPresentation().getClass()));
	}

	private String fillSpecificationProperties(Class<?> type, Properties p) {
		String file = ENV.getConfigPath() + SpecificationH5Exchange.FILENAME_SPEC_PROPERTIES;
		Pool pool = ENV.get(Pool.class);
		pool.add("%testrule", "TEST");
		Properties pp = new Properties();
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			String key = entry.getKey().toString();
			String val = entry.getValue().toString();
			
			String allnames = StringUtil.toString(BeanDefinition.getBeanDefinition(type).getAttributeNames(), -1).replace(" ", "");
			allnames = allnames.substring(1, allnames.length() - 1);
			pp.put(key.substring(1), 
					val.replace("<rule>", "%testrule")
					.replace("<attribute names comma or space separated>", allnames)
					.replace("<comma-separated-list-of-observable-attribute-names>", allnames)
					.replace("<path like 'presentable.layoutconstaints'>", "presentable.layoutconstaints")
					);
		}
		FileUtil.saveProperties(file, pp);
		p.clear();
		p.putAll(pp);
		return file;
	}

	private Properties initializeBeanSpecProperties(Class<?> type, int expectedElementCount) {
		BeanDefinition bean = BeanDefinition.getBeanDefinition(TypeBean.class);
		Properties p = new Properties();
		SpecificationH5Exchange specExchange = new SpecificationH5Exchange();
		specExchange.saveSpecificationEntries(bean, p);

		String file = ENV.getConfigPath() + SpecificationH5Exchange.FILENAME_SPEC_PROPERTIES;
		assertTrue(FileUtil.userDirFile(file).exists());
		assertEquals(expectedElementCount, p.size());
		return p;
	}

}
