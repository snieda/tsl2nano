package de.tsl2.nano.h5;

import static de.tsl2.nano.specification.SpecificationExchange.EXT_CSV;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Properties;

import org.anonymous.project.Account;
import org.anonymous.project.Address;
import org.anonymous.project.Category;
import org.anonymous.project.Charge;
import org.anonymous.project.Chargeitem;
import org.anonymous.project.Item;
import org.anonymous.project.Property;
import org.anonymous.project.Type;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IBeanDefinitionSaver;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.SpecificationExchange;

public class SpecificationExchangeTest implements ENVTestPreparation {
	
	@Before
	public void setUpBefore() throws Exception {
		Bean.clearCache();
		ENVTestPreparation.setUp("h5", SpecificationExchangeTest.class, false);
		NanoH5.registereExpressionsAndPools();
		FileUtil.delete(ENV.getConfigPath() + SpecificationExchange.FILENAME_SPEC_PROPERTIES);
		FileUtil.delete(ENV.getConfigPath() + SpecificationExchange.FILENAME_SPEC_PROPERTIES + SpecificationExchange.EXT_CSV);
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
		BeanDefinition.getBeanDefinition(type).saveDefinition();
		Properties p = initializeBeanSpecProperties(type, getExpectedEntryCount(type, 1));
		String file = fillSpecificationProperties(type, p);
		int errors = NanoH5Util.enrichFromSpecificationProperties();
		
		checkSpecificationDone(type, file, errors, "");
	}

	@Test
	public void testSimpleSpecificationCSV() {
		Class<?> type = TypeBean.class;
		BeanDefinition.getBeanDefinition(type).saveDefinition();
		Properties p = initializeBeanSpecProperties(type, getExpectedEntryCount(type, 1));
		String file = fillSpecificationProperties(type, p);
		FileUtil.delete(file); // -> the csv file will be used!
		
		file = new SpecificationH5Exchange().saveAsTSV(file + EXT_CSV, p);
		
		int errors = NanoH5Util.enrichFromSpecificationProperties();
		
		checkSpecificationDone(type, file, errors, EXT_CSV);
	}
	
	@Test
	public void testTimesheetSpec() {
		BeanContainer.initEmtpyServiceActions();
		
		ENV.extractResource(SpecificationExchange.FILENAME_SPEC_PROPERTIES + SpecificationExchange.EXT_CSV);
		SpecificationH5Exchange specExchange = new SpecificationH5Exchange();
		ENV.addService(IBeanDefinitionSaver.class, specExchange);
		ENV.addService(SpecificationExchange.class, specExchange);
		specExchange.setExists(true);
		
		BeanDefinition.defineBeanDefinitions(Account.class, Address.class, Category.class, Type.class, Property.class, 
				Item.class, Charge.class, Chargeitem.class);
		assertEquals("specification enrichment finished with errors", 0, NanoH5Util.enrichFromSpecificationProperties());
		assertTrue(BeanDefinition.getBeanDefinition("charge").getAttribute("weekday") != null);
	}
	
	private void checkSpecificationDone(Class<?> type, String file, int errors, String fileExt) {
//		assertTrue(FileUtil.userDirFile(ENV.getTempPath() + BeanUtil.FILENAME_SPEC_PROPERTIES).exists());
		assertEquals(0, errors);
//		assertFalse(FileUtil.userDirFile(file).exists()); //moving not yet working?
		String doneFile = Pool.getSpecificationRootDir() + SpecificationExchange.FILENAME_SPEC_PROPERTIES + fileExt + ".done";
		assertTrue(file + ".done should exist", FileUtil.userDirFile(doneFile).exists());
		
		BeanDefinition<?> bean = BeanDefinition.getBeanDefinition(type);
		assertTrue("attribute 'testrule' was not created", bean.getAttribute("testrule", false) != null);
		assertTrue(bean.getAction("%testrule") != null);
		assertTrue(Proxy.isProxyClass(bean.getAttribute("string").getPresentation().getClass()));
		assertEquals(2, (int)AttributeDefinition.getAttributePropertyFromPath("typeBean.bigDecimal.constraint.scale").getValue());
	}

	private String fillSpecificationProperties(Class<?> type, Properties p) {
		String file = ENV.getConfigPath() + SpecificationH5Exchange.FILENAME_SPEC_PROPERTIES;
//		Pool pool = ENV.get(Pool.class);
//		pool.add("%testrule", "TEST");
		Properties pp = new Properties();
		pp.put("%testrule", "TEST");
		pp.put("typeBean.bigDecimal.constraint.scale*", "2");
		String allnames = StringUtil.toString(BeanDefinition.getBeanDefinition(type).getAttributeNames(), -1).replace(" ", "");
		allnames = allnames.substring(1, allnames.length() - 1);
		allnames += ",testrule";
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			String key = entry.getKey().toString();
			String val = entry.getValue().toString();
			
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
		ENV.get(Pool.class).add("%testrule", "1");
		BeanDefinition bean = BeanDefinition.getBeanDefinition(TypeBean.class);
		Properties p = new Properties();
		SpecificationH5Exchange specExchange = new SpecificationH5Exchange();
		specExchange.saveSpecificationEntries(bean, p);
		ENV.addService(SpecificationExchange.class, specExchange);
		
		String file = ENV.getConfigPath() + SpecificationH5Exchange.FILENAME_SPEC_PROPERTIES;
		assertTrue(FileUtil.userDirFile(file).exists());
		assertEquals(expectedElementCount, p.size());
		return p;
	}

}
