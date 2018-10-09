package de.my.logbook;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.my.logbook.entity.Entry;
import de.my.logbook.entity.ValueType;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.h5.collector.Controller;
import de.tsl2.nano.h5.expression.RuleExpression;

public class LogbookApplicationTest implements ENVTestPreparation {

    @BeforeClass
    public static void setUp() {
        ENVTestPreparation.setUp();
        BeanDefinition.clearCache();
        BeanContainer.initEmtpyServiceActions();
        //load expression classes
        RuleExpression.expressionPattern();
        
        //process annotations...
		BeanDefinition.getBeanDefinition(Entry.class).saveDefinition();
		BeanDefinition.getBeanDefinition(ValueType.class).saveDefinition();
    }
    
    @AfterClass
    public static void tearDown() {
        ENVTestPreparation.tearDown();
    }

	@Test
	public void testEntityCreation() {
		//initialize a test BeanContainer
		ValueType valueType = new ValueType();
		valueType.setId("1");
		valueType.setName("testValueType");
        Collection testBeans = BeanContainer.initEmtpyServiceActions();
        testBeans.add(valueType);
        
		//load the defined/annotated controller
		Controller<Collection<ValueType>, ValueType> controller = (Controller<Collection<ValueType>, ValueType>) BeanDefinition.getBeanDefinition(BeanDefinition.PREFIX_VIRTUAL + "/" + Controller.createBeanDefName(Entry.class, ValueType.class));
        assertEquals(Entry.class, controller.getTargetType());
		controller.setForceUserInteraction(true);
		
		//use the controller
		controller.getCurrentData().add(valueType);
		String actionId = controller.getBean(valueType).getActions().iterator().next().getId();
		Entry newEntry = (Entry) controller.doAction(Controller.createActionName(1, actionId), null);
		assertEquals(valueType.getId(), newEntry.getType().getId());
		assertEquals(0d, newEntry.getValue(), 0.1d);
	}

}
