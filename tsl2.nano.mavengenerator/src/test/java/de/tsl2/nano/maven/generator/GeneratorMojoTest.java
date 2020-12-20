package de.tsl2.nano.maven.generator;

import static de.tsl2.nano.codegen.ACodeGenerator.KEY_OUTPUTPATH;

import java.io.File;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.util.ENVTestPreparation;

public class GeneratorMojoTest extends AbstractMojoTestCase implements ENVTestPreparation {

	@Before
	public void setUp() throws Exception {
		ENVTestPreparation.super.setUp("mavengenerator");
		// required for mojo lookups to work
		super.setUp();
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testMojo() throws Exception {
		try {
			String pom = "src/test/resources/test-pom.xml";
//			Mojo mojo = lookupMojo("run", new File(getBasedir(), pom ));
		Mojo mojo = configureMojo(new GeneratorMojo(), extractPluginConfiguration("tsl2.nano.mavengenerator",new File(getBasedir(), pom )));
			assertNotNull( mojo );

			//TODO: mojo @parameter are not filled!
			setVariableValueToObject(mojo, "project", new MavenProject());
			setVariableValueToObject(mojo, "algorithm", "de.tsl2.nano.codegen.PackageGenerator");
			setVariableValueToObject(mojo, "outputPath", getBasedir() + "/target/generated-sources");
			setVariableValueToObject(mojo, "unpackaged", "false");
			setVariableValueToObject(mojo, "singleFile", "false");
			setVariableValueToObject(mojo, "filterInstanceable", "true");
			mojo.execute();
			assertEquals(getBasedir() + "/target/generated-sources", System.getProperty(KEY_OUTPUTPATH));
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MojoExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MojoFailureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
