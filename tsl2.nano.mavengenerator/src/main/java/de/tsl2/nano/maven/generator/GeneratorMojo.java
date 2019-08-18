package de.tsl2.nano.maven.generator;

import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.util.codegen.ClassGenerator;
import de.tsl2.nano.util.codegen.PackageGenerator;

@Mojo( name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES )
public class GeneratorMojo extends AbstractMojo {
	
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

	@Parameter(defaultValue="de.tsl2.nano.util.codegen.PackageGenerator", property="bean.generation.generator")
	private String generator;
    @Parameter(property = "bean.generation.packageFilePath", required = true )
	private String packageFilePath;
	@Parameter(defaultValue="codegen/beanconstant.vm", property="bean.generation.templateFilePath" )
	private String templateFilePath;
	@Parameter(property="bean.generation.propertyFile")
	private String propertyFile;
	@Parameter( property = "bean.generation.outputpath")
	private String outputPath;
	@Parameter( property = "bean.generation.nameprefix")
	private String destinationPrefix;
    @Parameter( property = "bean.generation.namepostfix", required = false )
	private String destinationPostfix;
    @Parameter( property = "bean.generation.unpackaged", defaultValue = "false", required = false )
	private String unpackaged;
    @Parameter( property = "bean.generation.singleFile", defaultValue = "false", required = false )
	private String singleFile;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			if (outputPath != null)
				System.setProperty("bean.generation.outputpath", outputPath);
			if (destinationPrefix != null)
				System.setProperty("bean.generation.nameprefix", destinationPrefix);
			if (destinationPostfix != null)
				System.setProperty("bean.generation.namepostfix", destinationPostfix);
			System.setProperty("bean.generation.unpackaged", unpackaged);
			System.setProperty("bean.generation.singleFile", singleFile);

			ClassGenerator genInstance = ClassGenerator.instance((ClassGenerator)BeanClass.createInstance(generator));
			if (genInstance instanceof PackageGenerator) {
				Properties properties = ((PackageGenerator)genInstance).getProperties();
				properties.putAll(System.getenv());
				properties.putAll(System.getProperties());
				properties.putAll(project.getProperties());
				properties.put("project", project);
			}

			PackageGenerator.main(packageFilePath == null ? new String[0] 
				: new String[] { packageFilePath, templateFilePath, generator, propertyFile });

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
