package de.tsl2.nano.annotation.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

@With(TestAnnotationFactory.class) @TestAnnotation(myValue="MEINTEST")
public class AnnotationFactoryTest {

    @Test
    public void testWith() {
        AnnotationFactory.with(this, this.getClass());
    }
}

class TestAnnotationFactory implements AnnotationFactory<AnnotationFactoryTest, TestAnnotation> {
    @Override
    public void build(AnnotationFactoryTest instance, TestAnnotation annotation) {
        assertTrue(instance != null);
        assertEquals("MEINTEST", annotation.myValue());
    }
    
}
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@With(TestAnnotationFactory.class)
@interface TestAnnotation {
    String myValue();
}
