package de.tsl2.nano.codegen;

import org.apache.commons.logging.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ENVTestPreparation;

public class PackageGeneratorTest implements ENVTestPreparation {
    private static final Log LOG = LogFactory.getLog(PackageGeneratorTest.class);
    private static String BASE_DIR_GENERATOR;

    @BeforeClass
    public static void setUp() {
        BASE_DIR_GENERATOR = ENVTestPreparation.setUp();
    }

    @AfterClass
    public static void tearDown() {
        ENVTestPreparation.tearDown();
    }
    
    @Test
    public void testPackageGeneration() throws Exception {
        String file = BASE_DIR_GENERATOR + /*"tsl2.nano.generator/target/*/ "classes/" + this.getClass().getPackage().getName();
        ACodeGenerator.main(new String[] {PackageGenerator.class.getName(), file, "codegen/beanconstant.vm" });
        //TODO: check file creation!
    }
}
