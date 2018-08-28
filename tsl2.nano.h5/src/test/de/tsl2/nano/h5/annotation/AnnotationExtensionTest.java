package de.tsl2.nano.h5.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.annotation.extension.With;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.fi.Bean;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.h5.annotation.DependencyListener.ListenerType;
import de.tsl2.nano.h5.annotation.Specification.SpecificationType;
import de.tsl2.nano.h5.expression.RuleExpression;

public class AnnotationExtensionTest implements ENVTestPreparation {
    static final String MYVIRTUALATTRIBUTE = "myvirtualattribute";

    @BeforeClass
    public static void setUp() {
        ENVTestPreparation.setUp();
        BeanDefinition.clearCache();
        BeanContainer.initEmtpyServiceActions();
        //load expression classes
        RuleExpression.expressionPattern();
        
        //process annotations...
        BeanDefinition.getBeanDefinition(BeanType.class).saveDefinition();
        BeanDefinition.getBeanDefinition(Composition.class).saveDefinition();
        BeanDefinition.getBeanDefinition(Base.class).saveDefinition();
    }
    
    @AfterClass
    public static void tearDown() {
        ENVTestPreparation.tearDown();
    }

    @Test
    public void testCompositor() {
        getVirtualDefinition(de.tsl2.nano.h5.Compositor.class);
    }

    @Test
    public void testCSheet() {
        de.tsl2.nano.h5.CSheet sheet = getVirtualDefinition(de.tsl2.nano.h5.CSheet.class);
        assertEquals(new BigDecimal(6), sheet.get(0, 1));
    }

    @Test
    public void testQueryAndSpecification() {
        getVirtualDefinition(de.tsl2.nano.h5.QueryResult.class);
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
            assertTrue(e.getMessage().contains(FileNotFoundException.class.getSimpleName()) && e.getMessage().contains(ruleDefinitionFile));
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
@With(CompositorAnnotationFactory.class) @Compositor(baseType=Base.class, baseAttribute="name", targetAttribute="composition", iconAttribute="icon")
@With(CSheetAnnotationFactory.class) @CSheet(title="myCSheet", rows=3, cols=3, cells = {
    @CCell(row=0, col=0, value="1"), @CCell(row=0, col=1, value="=A1+5")
})
class BeanType {
    Composition composition;

    public Composition getComposition() {
        return composition;
    }
    public void setComposition(Composition composition) {
        this.composition = composition;
    }
}
class Composition {
    BeanType beanType;
    Base target;
    public BeanType getBeanType() {
        return beanType;
    }
    public void setBeanType(BeanType beanType) {
        this.beanType = beanType;
    }
    public Base getTarget() {
        return target;
    }
    public void setTarget(Base target) {
        this.target = target;
    }
}
class Base implements Serializable {
    private static final long serialVersionUID = 1L;

    String name;
    String icon;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    @With(DependencyListenerAnnotationFactory.class) @DependencyListener(rule="myvirtualattribute", listenerType=ListenerType.BOTH, observables= {"name"})
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }
}
