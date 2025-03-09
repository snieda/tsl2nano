package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;

public class StatelessActionBeanTest implements ENVTestPreparation {
    @Test
    public void testStatelessActionBeanUsage() {
        StatelessActionBean<StatelessTestBean> bean = new StatelessActionBean<>(StatelessTestBean.class);
        assertEquals(1, bean.getActions().size());
        assertEquals("create", bean.getActionByName("create").getShortDescription());
        
        BeanDefinition<StatelessTestBean> bean2 = BeanDefinition.getBeanDefinition(StatelessTestBean.class);
        assertEquals(bean, bean2);
        bean.saveDefinition();
        ENV.reset();
        bean2 = BeanDefinition.getBeanDefinition(StatelessTestBean.class);
        assertEquals(bean, bean2);
    }
    @Test
    public void testLoadingStatelessActionBeans() {
        // TODO: create bean-jar file and call NanoH5.createVirtualActionBeans()
    }
}

class StatelessTestBean {
    public Boolean create(TypeBean myEntity) {
        return true;
    }

    protected void internalMethod() {}
}