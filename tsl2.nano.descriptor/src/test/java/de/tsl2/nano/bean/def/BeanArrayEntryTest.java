package de.tsl2.nano.bean.def;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.SortedMap;

import org.junit.Test;

import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.util.MapUtil;

public class BeanArrayEntryTest {

	@Test
	public void testBeanArrayEntries() {
		CompatibilityLayer myMapValue = new CompatibilityLayer();
		MyArrayBean b = new MyArrayBean(new CompatibilityLayer[] {myMapValue}); 
		Bean<MyArrayBean> bean = Bean.getBean(b);
		
		Object bServices = bean.getValue("services");
		Bean<Object> mapBean = Bean.getBean(bServices);
		Iterator<BeanValue<?>> it = mapBean.getBeanValues().iterator();
		it.next();
		BeanValue<?> mapValue = it.next();
		assertEquals("a0", mapValue.getName());
		assertEquals(myMapValue, mapValue.getValue());
		assertEquals(mapValue.getName() + "=" + mapValue.getValue(), mapValue.toString());
	}

}
class MyArrayBean {
	CompatibilityLayer[] services;
	
	public MyArrayBean(CompatibilityLayer[] compatibilityLayers) {
		this.services = compatibilityLayers;
	}
	public CompatibilityLayer[] getServices() {
		return services;
	}
	public void setServices(CompatibilityLayer[] services) {
		this.services = services;
	}
}