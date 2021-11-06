package de.tsl2.nano.h5.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.annotation.extension.With;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.annotation.Constraint;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.fi.Bean;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.annotation.DependencyListener.ListenerType;
import de.tsl2.nano.h5.annotation.Specification.SpecificationType;
import de.tsl2.nano.h5.expression.RuleExpression;

@net.jcip.annotations.NotThreadSafe
public class AnnotationExtensionTest implements ENVTestPreparation {
    static final String MYVIRTUALATTRIBUTE = "myvirtualattribute";

    @BeforeClass //we should not use @Before each test. This would reset the entries done by class annotations...
    public static void setUp() {
        ENVTestPreparation.setUp("h5", true);
        Bean.clearCache();
        BeanContainer.initEmtpyServiceActions();
        //load expression classes
        RuleExpression.expressionPattern();
        NanoH5.registereExpressionsAndPools();
        //process annotations...
        BeanDefinition.getBeanDefinition(BeanType.class).saveDefinition();
        BeanDefinition.getBeanDefinition(Composition.class).saveDefinition();
        BeanDefinition.getBeanDefinition(Base.class).saveDefinition();
    }
    
    @AfterClass
    public static void tearDown() {
//        ENVTestPreparation.tearDown();
    }

    @Test
    public void testCompositor() {
        getVirtualDefinition(de.tsl2.nano.h5.collector.Compositor.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testController() {
        //BeanType <-> Composition.target -> Base.name
        de.tsl2.nano.h5.collector.Controller c = getVirtualDefinition(de.tsl2.nano.h5.collector.Controller.class);
        assertEquals(BeanType.class, c.getTargetType());
        Base instance = new Base();
        c.getCurrentData().add(instance);
        Collection testBeans = BeanContainer.initEmtpyServiceActions();
        testBeans.add(instance);
        
        de.tsl2.nano.bean.def.Bean item = c.getBean(instance);
        Collection<IAction> actions = item.getActions();
        assertTrue(actions.size() > 0);
        int i = 0;
        Map context = null;
        Object result;
        for (IAction a : actions) {
            String actionIdWithRowNumber = de.tsl2.nano.h5.collector.Controller.createActionName(++i, a.getId());
            result = c.doAction(actionIdWithRowNumber, context);
            assertTrue(result instanceof BeanType);
        }
    }

    @Test
    public void testCSheet() {
        de.tsl2.nano.h5.collector.CSheet sheet = getVirtualDefinition(de.tsl2.nano.h5.collector.CSheet.class);
        assertEquals(new BigDecimal(6), sheet.get(0, 1));
    }

    @Test
    public void testQueryAndSpecification() {
        getVirtualDefinition(de.tsl2.nano.h5.collector.QueryResult.class);
    }

    @Test
    public void testVirtualAttributeAndDependencyListener() {
        //TODO: attribute name not correct!
        assertEquals("_" + MYVIRTUALATTRIBUTE, BeanDefinition.getBeanDefinition(BeanType.class).getAttribute("_" + MYVIRTUALATTRIBUTE).getName());
        
        Base base = new Base();
        base.setName("XXX");
        String ruleDefinitionFile = MYVIRTUALATTRIBUTE + ".xml";
        try {
            Bean.getBean(base).setValue("name", "YYY");
            fail("icon should reference the rule '" + ruleDefinitionFile + " which is not defined! So a FileNotFoundException has to be thrown");
        } catch (Exception e) {
            assertTrue(e.getClass().equals(IllegalArgumentException.class) && e.getMessage().contains(MYVIRTUALATTRIBUTE));
        }
    }

    private <T extends BeanDefinition<?>> T getVirtualDefinition(Class<T> type) {
        Collection<BeanDefinition<?>> virtualDefinitions = BeanDefinition.loadVirtualDefinitions();
        for (BeanDefinition<?> beanDef : virtualDefinitions) {
            if (type.equals(beanDef.getClass()))
                return (T) beanDef;
        }
        throw new IllegalStateException("no virtual type " + type + " found!");
    }
}
@With(VirtualAttributeAnnotationFactory.class) @VirtualAttribute(name=AnnotationExtensionTest.MYVIRTUALATTRIBUTE, specificationType=SpecificationType.RULE, expression="myvirtualattribute")
@With(SpecificationAnnotationFactory.class) @Specification(name="myquery", specificationType=SpecificationType.QUERY, expression="select...")
@With(QueryAnnotationFactory.class) @Query(name="myquery", icon="icons/go.png")
@With(CompositorAnnotationFactory.class) @Compositor(baseType=Base.class, baseAttribute="composition", targetAttribute="baseComposition", iconAttribute="icon")
@With(ControllerAnnotationFactory.class) @Controller(baseType=Base.class, baseAttribute="composition", targetAttribute="baseComposition", iconAttribute="icon", increaseAttribute="value")
@With(CSheetAnnotationFactory.class) @CSheet(title="myCSheet", rows=3, cols=3, cells = {
    @CCell(row=0, col=0, value="1"), @CCell(row=0, col=1, value="=A1+5")
})
class BeanType {
    Composition composition;
    String name;
    @Constraint(nullable=false)
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Composition getBaseComposition() {
        return composition;
    }
    public void setBaseComposition(Composition composition) {
        this.composition = composition;
    }
}
class Composition {
    BeanType beanType;
    Base parent;
    public BeanType getBeanType() {
        return beanType;
    }
    public void setBeanType(BeanType beanType) {
        this.beanType = beanType;
    }
    public Base getParent() {
        return parent;
    }
    public void setParent(Base parent) {
        this.parent = parent;
    }
}
class Base implements Serializable {
    private static final long serialVersionUID = 1L;

    String name;
    String icon;
    int value;
    Composition composition;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public Composition getComposition() {
        return composition;
    }
    public void setComposition(Composition composition) {
        this.composition = composition;
    }
    @With(DependencyListenerAnnotationFactory.class) @DependencyListener(rule="myvirtualattribute", listenerType=ListenerType.BOTH, observables= {"name"})
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }
    public int getValue() {
        return value;
    }
    public void setValue(int value) {
        this.value = value;
    }
}
