package de.tsl2.nano.bean.def;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.SortedMap;

import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.util.MapUtil;

public class BeanMapEntryTest {

	@Test
	public void testBeanMapEntries() {
		//object is: not serializable, no toString() method implemented
		CompatibilityLayer myMapValue = new CompatibilityLayer();
		MyBean b = new MyBean(MapUtil.asSortedMap(myMapValue.getClass(), myMapValue)); 
		Bean<MyBean> bean = Bean.getBean(b);
		
		Object bServices = bean.getValue("services");
		Bean<Object> mapBean = Bean.getBean(bServices);
		Iterator<BeanValue<?>> it = mapBean.getBeanValues().iterator();
		it.next();
		BeanValue<?> mapValue = it.next();
		
		//check all representations
		assertEquals(myMapValue.getClass().toString(), mapValue.getName());
		assertEquals(myMapValue, mapValue.getValue());
		assertEquals(mapValue.getName() + "=" + mapValue.getValue(), mapValue.toString());
		assertEquals(mapValue.getType().getSimpleName(), mapValue.getValueText());

		//if no toString(), and the following registered, it will be called in the deep of dust
        myMapValue.registerMethod("reflectionToString",
                "de.tsl2.nano.format.ToStringBuilder",
                "reflectionToString",
                true,
                Object.class);
		ENV.addService(CompatibilityLayer.class, myMapValue);
		
//        BeanCollector bc = BeanCollector.getBeanCollector((Collection) mapBean.getInstance(), 0);
		IBeanCollector bc = ((BeanValue)bean.getAttribute("services")).connectToSelector(bean);
        assertTrue(bc.getColumnText(bc.getCurrentData().iterator().next(), 1).contains(myMapValue.getClass().getSimpleName()));
	}

}
class MyBean {
	SortedMap<Class<?>, Object> services;
	
	public MyBean(SortedMap<Class<?>, Object> services) {
		this.services = services;
	}
	public SortedMap<Class<?>, Object> getServices() {
		return services;
	}
	public void setServices(SortedMap<Class<?>, Object> services) {
		this.services = services;
	}
}