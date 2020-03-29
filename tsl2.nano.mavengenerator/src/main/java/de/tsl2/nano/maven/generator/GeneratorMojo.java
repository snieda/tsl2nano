package de.tsl2.nano.maven.generator;

import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import de.tsl2.nano.codegen.ACodeGenerator;
import static de.tsl2.nano.codegen.ACodeGenerator.*;

@Mojo( name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES )
public class GeneratorMojo extends AbstractMojo {
	
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

	@Parameter(alias = "generator", defaultValue="de.tsl2.nano.codegen.PackageGenerator", property=KEY_PREFIX + "generator")
	private String algorithm;
    @Parameter(alias = "packageFilePath", property = KEY_PREFIX + "packageFilePath", required = true )
	private String model;
	@Parameter(alias = "templateFilePath", defaultValue="codegen/beanconstant.vm", property=KEY_PREFIX + "templateFilePath" )
	private String template;
	@Parameter(alias = "package", property=KEY_PACKAGENAME )
	private String filter;
	@Parameter(property=KEY_PROPERTIES)
	private String propertyFile;
	@Parameter( property = KEY_OUTPUTPATH)
	private String outputPath;
	@Parameter( property = KEY_NAMEPREFIX)
	private String destinationPrefix;
    @Parameter( property = KEY_NAMEPOSTFIX, required = false )
	private String destinationPostfix;
    @Parameter( property = KEY_UNPACKAGED, defaultValue = "false", required = false )
	private String unpackaged;
    @Parameter( property = KEY_SINGLEFILE, defaultValue = "false", required = false )
	private String singleFile;
    @Parameter( property = KEY_INSTANCEABLE, defaultValue = "true", required = false )
	private String filterInstanceable;
    @Parameter( property = KEY_ANNOTATED, required = false )
	private String filterAnnotated;
    @Parameter( property = KEY_INSTANCEOF, required = false )
	private String filterInstanceOf;
    @Parameter( property = KEY_KEYWORDLIST, required = false )
	private String keywordList;
    @Parameter( property = KEY_KEYWORDREPL, required = false )
	private String keywordReplacement;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			if (outputPath != null)
				System.setProperty(KEY_OUTPUTPATH, outputPath);
			if (destinationPrefix != null)
				System.setProperty(KEY_NAMEPREFIX, destinationPrefix);
			if (destinationPostfix != null)
				System.setProperty(KEY_NAMEPOSTFIX, destinationPostfix);
			System.setProperty(KEY_UNPACKAGED, unpackaged);
			System.setProperty(KEY_SINGLEFILE, singleFile);
			System.setProperty(KEY_INSTANCEABLE, filterInstanceable);
			System.setProperty(KEY_ANNOTATED, filterAnnotated);
			System.setProperty(KEY_INSTANCEOF, filterInstanceOf);
			System.setProperty(KEY_KEYWORDLIST, keywordList);
			System.setProperty(KEY_KEYWORDREPL, keywordReplacement);

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
