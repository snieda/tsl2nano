package de.tsl2.nano.codegen;

import org.apache.commons.logging.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;

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
        String file = /*BASE_DIR_GENERATOR + *//*"tsl2.nano.generator/target/*/
                FileUtil.userDirFile("classes/" + this.getClass().getPackage().getName()).getAbsolutePath();
        ACodeGenerator.main(new String[] {PackageGenerator.class.getName(), file, "codegen/beanconstant.vm" });
        //TODO: check file creation!
    }
    @Test
    public void testPackageGenerationWithPackageFilter() throws Exception {
        String file = /*BASE_DIR_GENERATOR + *//*"tsl2.nano.generator/target/*/
                FileUtil.userDirFile("classes/" + this.getClass().getPackage().getName()).getAbsolutePath();
        ACodeGenerator.main(new String[] {PackageGenerator.class.getName(), file, "codegen/beanconstant.vm", this.getClass().getPackage().getName() + "..*Package.*" });
        //TODO: check file creation!
    }
}
