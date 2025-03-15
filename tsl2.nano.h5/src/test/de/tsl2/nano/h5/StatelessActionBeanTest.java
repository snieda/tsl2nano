package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.specification.actions.Action;

public class StatelessActionBeanTest implements ENVTestPreparation {
    @Before
    public void setUp() {
        ENVTestPreparation.setUp();
    }
    
    @Test
    public void testStatelessActionBeanUsage() {
        Pool.registerTypes(Action.class);

        StatelessActionBean<StatelessTestBean> bean = new StatelessActionBean<>(StatelessTestBean.class);
        bean.saveDefinition();
        assertEquals(1, bean.getActions().size());
        assertEquals("Create", bean.getActions().iterator().next().getShortDescription());
        assertEquals("Create", bean.getActionByName("create").getShortDescription());
        
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