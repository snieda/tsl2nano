package de.tsl2.nano.codegen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import de.tsl2.nano.core.cls.BeanClass;

public class GeneratorUtilityTest {

    @Test
    public void testObjectCreation() {
        GeneratorUtility util = new GeneratorUtility();
        util.put("GeneratorUtility", GeneratorUtility.class.getName());
        assertTrue(util.get("objGeneratorUtility") instanceof GeneratorUtility);
    }

    @Test
    public void testClassCreation() {
        GeneratorUtility util = new GeneratorUtility();
        util.put("GeneratorUtility", GeneratorUtility.class.getName());
        assertTrue(GeneratorUtility.class.isAssignableFrom((Class<?>) util.get("clsGeneratorUtility")));
    }

    @Test
    public void testBeanClassCreation() {
        GeneratorUtility util = new GeneratorUtility();
        util.put("GeneratorUtility", GeneratorUtility.class.getName());
        assertTrue(GeneratorUtility.class.isAssignableFrom(((BeanClass) util.get("blsGeneratorUtility")).getClazz()));
    }

    @Test
    public void testToString() {
        GeneratorUtility util = new GeneratorUtility();
        Object list = Arrays.asList("eins", "zwei", "drei");
        assertEquals("eins, zwei, drei", util.toString(list));
    }

    @Test
    public void testEval() {
        GeneratorUtility util = new GeneratorUtility();
        util.put("objGeneratorUtility", util);
        util.put("eins", "EINS");
        assertEquals("EINS", util.eval(util.get("objGeneratorUtility"), "get(eins)"));
    }
}