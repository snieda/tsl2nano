package de.tsl2.nano.maven.generator;

import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import de.tsl2.nano.codegen.ACodeGenerator;

@Mojo( name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES )
public class GeneratorMojo extends AbstractMojo {
	
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

	@Parameter(alias = "generator", defaultValue="de.tsl2.nano.codegen.PackageGenerator", property="bean.generation.generator")
	private String algorithm;
    @Parameter(alias = "packageFilePath", property = "bean.generation.packageFilePath", required = true )
	private String model;
	@Parameter(alias = "templateFilePath", defaultValue="codegen/beanconstant.vm", property="bean.generation.templateFilePath" )
	private String template;
	@Parameter(alias = "package", property="bean.generation.package" )
	private String filter;
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
    @Parameter( property = "bean.generation.filter.instanceable", defaultValue = "false", required = false )
	private String filterInstanceable;
    @Parameter( property = "bean.generation.filter.annotated", defaultValue = "false", required = false )
	private String filterAnnotated;
    @Parameter( property = "bean.generation.filter.instanceof", defaultValue = "false", required = false )
	private String filterInstanceOf;

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
			System.setProperty("bean.generation.filter.instanceable", filterInstanceable);
			System.setProperty("bean.generation.filter.annotated", filterAnnotated);
			System.setProperty("bean.generation.filter.instanceof", filterInstanceOf);

			Properties properties = new Properties();
			properties.putAll(System.getenv());
			properties.putAll(System.getProperties());
			properties.putAll(project.getProperties());
			properties.put("project", project);

			ACodeGenerator.start(model == null ? new String[0] 
				: new String[] {algorithm, model, template, filter, propertyFile }, properties);

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
