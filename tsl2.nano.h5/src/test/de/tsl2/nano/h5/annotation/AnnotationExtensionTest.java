package de.tsl2.nano.h5.annotation;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.annotation.extension.With;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.BeanDefinition;
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
    public void testQueryAndSpecification() {
        getVirtualDefinition(de.tsl2.nano.h5.QueryResult.class);
    }

    @Test
    public void testVirtualAttributeAndDependencyListener() {
        //TODO: attribute name not correct!
        assertEquals("_" + MYVIRTUALATTRIBUTE, BeanDefinition.getBeanDefinition(BeanType.class).getAttribute("_" + MYVIRTUALATTRIBUTE).getName());
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
class Base {
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
