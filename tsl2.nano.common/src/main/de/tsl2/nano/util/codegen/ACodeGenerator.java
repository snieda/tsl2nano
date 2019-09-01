package de.tsl2.nano.util.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * 
 * Prepares and Defines a Velocity Template Generator
 * 
 * extensions may override the methods:
 * 
 * @see #getDefaultDestinationFile(String)
 * 
 *      The default implementation of this basic generator expects a java bean (class package) and creates a java file.
 * 
 *      Howto start (example): mainClass: de.tsl2.nano.util.codegen.ACodeGenerator arguments:
 *      bin/codegen/beanclass.vm
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 */
public abstract class ACodeGenerator {
    private static ACodeGenerator self = null;
    private VelocityEngine engine;
    String codeTemplate;

    protected static final Log LOG = LogFactory.getLog(ACodeGenerator.class);

    public static final String DEFAULT_DEST_PREFIX = "src/gen";
    
    protected ACodeGenerator() {
        init();
    }

    protected static boolean hasInstance() {
        return self != null;
    }
    /**
     * returns a singelton instance of type ACodeGenerator. if the singelton is null, the argument will be used as
     * singelton.
     * 
     * @param newInstance singelton instance, if singelton instance is null.
     * @return singelton
     */
    public static final ACodeGenerator instance(ACodeGenerator newInstance) {
        if (self == null || !(newInstance.getClass().isAssignableFrom(self.getClass()))) {
            self = newInstance;
        }
        return self;
    }

    /**
     * @param className generator class name
     * @return new singelton class generator instance
     */
    protected static final ACodeGenerator instance(String className) {
        try {
            return instance((ACodeGenerator) BeanClass.load(className).newInstance());
        } catch (final Exception e) {
            ManagedException.forward(e);
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
            LOG.debug("ACodeGenerator has initialized velocity template engine");
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        //Velocity.setProperty( Velocity.RUNTIME_LOG_LOGSYSTEM, new Log());
    }

    /**
     * @see #generate(String, String, String, Properties, ClassLoader)
     */
    public void generate(Object model, String modelFile, String templateFile, Properties properties) throws Exception {
        generate(model, modelFile, templateFile, getDefaultDestinationFile(modelFile), properties == null ? new Properties()
            : properties);
    }

    /**
     * generates a code file through the information of the 'modelFile' and the template 'templateFile'. The
     * generate code file will be stored at 'destFile'.
     * 
     * @param modelFile the source file
     * @param templateFile a velocity template (full path, not classpath!)
     * @param destFile the destination file
     * @throws Exception
     */
    public void generate(Object model, String modelFile,
            String templateFile,
            String destFile,
            Properties properties) throws Exception {

//        if (templateFile == null || !new File(templateFile).canRead()) {
//            throw new IllegalStateException("template file " + templateFile + " not readable.");
//        }
                
        final GeneratorUtility util = getUtilityInstance();
        final VelocityContext context = new VelocityContext();

        LOG.info("generating " + templateFile + " + " + modelFile + "  ==>  " + destFile);
        fillVelocityContext(model, modelFile, templateFile, destFile, properties, util, context);
        final Template template = engine.getTemplate(templateFile);

        final File dir = new File(destFile.substring(0, destFile.lastIndexOf("/")));
        dir.mkdirs();
        final BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

        template.merge(context, writer);
        writer.flush();
        writer.close();
    }

    protected void fillVelocityContext(Object model, String modelFile,
            String templateFile,
            String destFile,
            Properties properties,
            final GeneratorUtility util,
            final VelocityContext context) {
        context.put("path", getDestinationPackageName(modelFile, destFile));
        context.put("model", model);
        context.put("postfix", getDestinationPostfix());
        context.put("util", util);
        context.put("time", new Timestamp(System.currentTimeMillis()));
        context.put("template", templateFile);
        context.put("copyright", "Copyright (c) 2002-2019 Thomas Schneider");
        for (final Object p : properties.keySet()) {
            final Object v = properties.get(p);
            if (context.containsKey(p)) {
                LOG.error("name clash in velocity context on key '" + p + "': existing generator value: " + context.get(p.toString()) + ", user property: " + v + " will be ignored!");
                continue;
            }
            context.put((String) p, v);
            LOG.debug("adding velocity context property: " + p + "=" + v);
        }
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
        final String src = sourceFile.contains(".") ? sourceFile.substring(0, sourceFile.lastIndexOf('.')) : sourceFile;
        int isrc = dest.indexOf(src);
        return isrc > -1 ? dest.substring(isrc) : dest;
    }

    /**
     * override this method to provide a default destination file
     * 
     * @see #getModel(String, ClassLoader)
     * 
     * @param modelFile source file
     * @return default classloader
     */
    protected String getDefaultDestinationFile(String modelFile) {
        boolean unpackaged = Boolean.getBoolean("bean.generation.unpackaged");
        boolean singleFile = Boolean.getBoolean("bean.generation.singleFile");
        if (singleFile)
            modelFile = ""; //only the destination postfix is the name!
        else
            modelFile = unpackaged ? StringUtil.substring(modelFile, ".", null, true) : modelFile.replace('.', '/');
        String path = Util.get("bean.generation.outputpath", DEFAULT_DEST_PREFIX);
        return (path.endsWith("/") ? path : path + "/") + modelFile + getDestinationPostfix();
    }

    protected String getDestinationPostfix() {
        return Util.get("bean.generation.namepostfix", StringUtil.toFirstUpper(extractName(codeTemplate)) + ".java");
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
        return p.indexOf('.') != -1 ? p.substring(0, p.lastIndexOf('.')) : p;
    }

    public static String extractName(String filePath) {
        return StringUtil.substring(filePath, "/", ".", true);
    }
}