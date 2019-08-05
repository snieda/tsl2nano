package de.tsl2.nano.maven.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


import de.tsl2.nano.util.codegen.PackageGenerator;

@Mojo( name = "run", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class GeneratorMojo extends AbstractMojo {
	
	@Parameter(defaultValue="de.tsl2.nano.util.codegen.PackageGenerator", property="bean.generation.generator")
	private String generator;
    @Parameter(property = "tsl2nano.packageFilePath", required = true )
	private String packageFilePath;
	@Parameter(defaultValue="codegen/beanconstant.vm", property="bean.generation.templateFilePath" )
	private String templateFilePath;
	@Parameter(property="bean.generation.propertyFile")
	private String propertyFile;
	@Parameter( property = "bean.generation.nameprefix")
	private String destinationPrefix;
    @Parameter( property = "bean.generation.namepostfix", required = false )
	private String destinationPostfix;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			PackageGenerator.main(new String[] { packageFilePath, templateFilePath, generator, propertyFile });
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
