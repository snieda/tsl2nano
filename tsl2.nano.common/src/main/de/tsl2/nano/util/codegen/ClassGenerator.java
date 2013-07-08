package de.tsl2.nano.util.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.util.bean.BeanClass;

/**
 * 
 * class to generate code from a given file. This implementation uses source files of type java.
 * 
 * please override the methods...
 * 
 * @see #getModel(String, ClassLoader)
 * @see #getDefaultClassloader()
 * @see #getDefaultDestinationFile(String) ...to have your own ClassGenerator.
 * 
 *      The default implementation of this basic generator expects a java bean (class package) and creates a java file.
 * 
 *      Howto start (example): mainClass: de.tsl2.nano.util.codegen.ClassGenerator arguments:
 *      bin/codegen/beanclass.vm
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 */
public class ClassGenerator {
    private static ClassGenerator self = null;
    private VelocityEngine engine;

    protected static final Log LOG = LogFactory.getLog(ClassGenerator.class);

    protected ClassGenerator() {
        init();
    }

    /**
     * instance
     * 
     * @return singelton instance
     */
    public static final ClassGenerator instance() {
        return instance(new ClassGenerator());
    }

    /**
     * returns a singelton instance of type ClassGenerator. if the singelton is null, the argument will be used as
     * singelton.
     * 
     * @param newInstance singelton instance, if singelton instance is null.
     * @return singelton
     */
    protected static final ClassGenerator instance(ClassGenerator newInstance) {
        if (self == null) {
            self = newInstance;
        }
        return self;
    }

    /**
     * @param className generator class name
     * @return new singelton class generator instance
     */
    protected static final ClassGenerator instance(String className) {
        try {
            return instance((ClassGenerator) Class.forName(className).newInstance());
        } catch (final Exception e) {
            ForwardedException.forward(e);
            return null;
        }
    }

    /**
     * initializes the velocity engine.
     */
    protected void init() {

        // Init Velocity
        //Velocity.init();
        engine = new VelocityEngine();

        engine.addProperty("resource.loader", "file, class, jar");
        engine.addProperty("file.resource.loader.class",
            "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        engine.addProperty("class.resource.loader.class",
            "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        engine.addProperty("jar.resource.loader.class", "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
        try {
            engine.init();
            LOG.debug("ClassGenerator has initialized velocity template engine");
        } catch (final Exception e) {
            throw new ForwardedException(e);
        }
        //Velocity.setProperty( Velocity.RUNTIME_LOG_LOGSYSTEM, new Log());
    }

    /**
     * @see #generate(String, String, String, Properties, ClassLoader)
     */
    public void generate(String modelFile, String templateFile, Properties properties) throws Exception {
        generate(modelFile, templateFile, getDefaultDestinationFile(modelFile), properties == null ? new Properties()
            : properties, getDefaultClassloader());
    }

    /**
     * generates a java class file through the information of the 'modelFile' and the template 'templateFile'. The
     * generate class file will be stored at 'destFile'.
     * 
     * @param modelFile the source file ({@linkplain #getModel(String, ClassLoader)})
     * @param templateFile a velocity template (full path, not classpath!)
     * @param destFile the destination file
     * @param classLoader used in ({@linkplain #getModel(String, ClassLoader)})
     * @throws Exception
     */
    public void generate(String modelFile,
            String templateFile,
            String destFile,
            Properties properties,
            ClassLoader classLoader) throws Exception {

        final GeneratorUtility util = getUtilityInstance();
        final VelocityContext context = new VelocityContext();

        LOG.info("generating " + templateFile + " + " + modelFile + "  ==>  " + destFile);
        context.put("package", getDestinationPackageName(modelFile, destFile));
        context.put("class", getModel(modelFile, classLoader));
        context.put("util", util);
        context.put("time", new Timestamp(System.currentTimeMillis()));
        context.put("template", templateFile);
        context.put("copyright", "Copyright (c) 2002-2011 Thomas Schneider");
        for (final Object p : properties.keySet()) {
            final Object v = properties.get(p);
            context.put((String) p, v);
            LOG.debug("adding velocity context property: " + p + "=" + v);
        }
        final Template template = engine.getTemplate(templateFile);

        final File dir = new File(destFile.substring(0, destFile.lastIndexOf("/")));
        dir.mkdirs();
        final BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

        template.merge(context, writer);
        writer.flush();
        writer.close();
    }

    /**
     * override this method to use your special utility
     * 
     * @return utility instance
     */
    protected GeneratorUtility getUtilityInstance() {
        return new GeneratorUtility();
    }

    /**
     * override this method to provide your model
     * 
     * @param modelFile source file name (normally a beans java file name)
     * @param classLoader classloader to be used to load modelFile class.
     * @return
     */
    protected Object getModel(String modelFile, ClassLoader classLoader) {
        //      assert modelFile.endsWith(".class") : "ClassGenerator.getModel(): The modelFile has to point to the package path of a java class file!";
        try {
            final Class clazz = classLoader.loadClass(modelFile);
            return new BeanClass(clazz);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * override this method to provide a default classloader
     * 
     * @see #getModel(String, ClassLoader)
     * 
     * @param modelFile source file
     * @return default classloader
     */
    protected String getDefaultDestinationFile(String modelFile) {
        modelFile = modelFile.replace('.', '/');
        //      return modelFile.replaceAll("(\\.java)", "UI\\1");
        return "src/gen/" + modelFile + "UI.java";
    }

    /**
     * tries to extract the destination class name. precondition is, that package-path starts equal on model and
     * destination!
     * 
     * @param sourceFile source class name
     * @param destinationFileName dest file name
     * @return dest class name
     */
    protected String getDestinationClassName(String sourceFile, String destinationFileName) {
        String dest = destinationFileName.replace('/', '.');
        dest = dest.substring(0, dest.lastIndexOf('.'));
        final String src = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
        return dest.substring(dest.indexOf(src));
    }

    /**
     * extracts the package name from the result of {@link #getDestinationClassName(String, String)}.
     * 
     * @param sourceFile source class name
     * @param destinationFileName dest file name
     * @return dest package name
     * @see #getDestinationClassName(String, String)
     */
    protected String getDestinationPackageName(String sourceFile, String destinationFileName) {
        final String p = getDestinationClassName(sourceFile, destinationFileName);
        return p.substring(0, p.lastIndexOf('.'));
    }

    /**
     * override this method to use your classloader
     * 
     * @return generators default classloader
     */
    protected ClassLoader getDefaultClassloader() {
        return this.getClass().getClassLoader();
    }

    /**
     * @param args model file name and template
     * @throws Exception any exception!
     */
    public static void main(String args[]) throws Exception {

        if (args.length != 2) {
            System.out.print("Syntax: ClassGenerator <model> <template>");
            System.exit(1);
        }

        final String modelFile = args[0];
        final String templateFile = args[1];

        ClassGenerator.instance().init();
        ClassGenerator.instance().generate(modelFile, templateFile, null);

    }
}