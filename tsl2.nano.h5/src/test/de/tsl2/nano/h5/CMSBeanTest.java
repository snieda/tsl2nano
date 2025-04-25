package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.NetUtil;

public class CMSBeanTest implements ENVTestPreparation {

    @Before
    public void setUp() {
        ENVTestPreparation.setUp();
    }    

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testProvideCMSBeans() {
        String url = NetUtil.isOnline() ? SampleApplicationBean.SF_BASE_URL_FILE : "file:doc/sample-applications/";
        BeanCollector<List<BeanDefinition>, BeanDefinition> beans = CMSBean.provideCMSBeans(url + "README.MD");
        ConcurrentUtil.sleep(2000);
        beans.getCurrentData().forEach(b -> System.out.println(((BeanDefinition)b).toValueMap(null)));

        assertEquals(11, beans.getCurrentData().size());

        BeanDefinition defBestellung = beans.getCurrentData().stream().filter(b -> b.getName().equals("bestellung")).findFirst().get();
        assertFalse(defBestellung.getAttributes().stream().anyMatch(a -> String.valueOf(((IAttribute)a).getValue(null)).contains("${")));
        assertTrue(String.valueOf(defBestellung.getAttribute("value").getValue(null)).endsWith("bestellung/bestellung.zip"));
        assertTrue(String.valueOf(defBestellung.getAttribute("image").getValue(null)).endsWith("bestellung/attachment/bestellung-controller.jpg"));

        assertEquals(IPresentable.TYPE_ATTACHMENT, defBestellung.getAttribute("image").getPresentation().getType());
        
        assertEquals(2, defBestellung.getActions().size());
        assertTrue(defBestellung.getAction("downloadAndExtract0") instanceof SpecifiedAction);

    }
}
