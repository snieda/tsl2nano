package de.tsl2.nano.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import de.tsl2.nano.core.Arg;
import de.tsl2.nano.core.Argumentator;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
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
 *      The default implementation of this basic generator expects a java bean
 *      (class package) and creates a java file.
 * 
 *      Howto start (example): mainClass:
 *      de.tsl2.nano.codegen.ACodeGenerator arguments:
 *      bin/codegen/beanclass.vm
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 */
public abstract class ACodeGenerator {
    private static final String KEY_PROPERTIES = "properties";
    protected static final String KEY_FILTER = "filter";
    protected static final String KEY_TEMPLATE = "template";
    protected static final String KEY_MODEL = "model";
    private static final String KEY_ALGORITHM = "algorithm";
    private VelocityEngine engine;
    String codeTemplate;
    private GeneratorUtility utilityInstance;
    Properties properties = null;

    protected static final Log LOG = LogFactory.getLog(ACodeGenerator.class);

    public static final String DEFAULT_DEST_PREFIX = "src/gen";

    protected ACodeGenerator() {
        initEngine();
    }

    /**
     * initializes the velocity engine.
     */
    protected void initEngine() {

        // Init Velocity
        // Velocity.init();
        engine = new VelocityEngine();

        engine.addProperty("resource.loader", "file, class, jar");
        engine.addProperty("file.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        engine.addProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        engine.addProperty("jar.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
        try {
            engine.init();
            LOG.debug("ACodeGenerator has initialized velocity template engine");
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        // Velocity.setProperty( Velocity.RUNTIME_LOG_LOGSYSTEM, new Log());
    }

    /**
     * @return properties for the velocity context
     */
    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

    public static void start(String[] mainArgs, Properties props) {
        start(mainArgs, props, 1);
    }
    public static void start(String[] mainArgs, Properties props, int errorExitCode) {
        Argumentator am = new Argumentator(ACodeGenerator.class.getSimpleName(), getManual(), errorExitCode, mainArgs);
        am.start(System.out, a -> {
            ACodeGenerator gen = (ACodeGenerator) BeanClass.createInstance(a.getProperty(KEY_ALGORITHM));
            if (props != null)
                gen.getProperties().putAll(props);
            gen.start(a);
            return gen.getClass().getSimpleName() + " finished successfull";
        });
    }

    public abstract void start(Properties args);

    public void initParameter(Properties args) {
        codeTemplate = args.getProperty(KEY_TEMPLATE);
        if (args.getProperty(KEY_PROPERTIES) != null)
            getProperties().putAll(FileUtil.loadProperties(args.getProperty(KEY_PROPERTIES),
                    Thread.currentThread().getContextClassLoader()));
    }

    /**
     * @see #generate(String, String, String, Properties, ClassLoader)
     */
    public void generate(Object model, String modelFile, String templateFile, Properties properties) {
        generate(model, modelFile, templateFile, getDefaultDestinationFile(modelFile),
                properties == null ? new Properties() : properties);
    }

    /**
     * generates a code file through the information of the 'modelFile' and the
     * template 'templateFile'. The generate code file will be stored at 'destFile'.
     * 
     * @param modelFile    the source file
     * @param templateFile a velocity template (full path, not classpath!)
     * @param destFile     the destination file
     */
    public void generate(Object model, String modelFile, String templateFile, String destFile, Properties properties) {

        // if (templateFile == null || !new File(templateFile).canRead()) {
        // throw new IllegalStateException("template file " + templateFile + " not
        // readable.");
        // }

        final VelocityContext context = new VelocityContext();

        LOG.info("generating " + templateFile + " + " + modelFile + "  ==>  " + destFile);
        fillVelocityContext(model, modelFile, templateFile, destFile, properties, getUtil(), context);
        final Template template = engine.getTemplate(templateFile);

        final File file = new File(destFile).getAbsoluteFile();
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            template.merge(context, writer);
            writer.flush();
            writer.close();
        } catch (ResourceNotFoundException | ParseErrorException | MethodInvocationException | IOException e) {
            ManagedException.forward(e);
        }
    }

    protected void fillVelocityContext(Object model, String modelFile, String templateFile, String destFile,
            Properties properties, final GeneratorUtility util, final VelocityContext context) {
        context.put("path", getDestinationPackageName(modelFile, destFile));
        context.put(KEY_MODEL, model);
        context.put("postfix", getDestinationPostfix());
        context.put("util", util);
        context.put("time", new Timestamp(System.currentTimeMillis()));
        context.put(KEY_TEMPLATE, templateFile);
        context.put("copyright", "Copyright (c) 2002-2019 Thomas Schneider");
        for (final Object p : properties.keySet()) {
            final Object v = properties.get(p);
            if (context.containsKey(p)) {
                LOG.error("name clash in velocity context on key '" + p + "': existing generator value: "
                        + context.get(p.toString()) + ", user property: " + v + " will be ignored!");
                continue;
            }
            context.put((String) p, v);
            LOG.debug("adding velocity context property: " + p + "=" + v);
        }
    }

    protected GeneratorUtility getUtil() {
        if (utilityInstance == null)
            utilityInstance = createUtilityInstance();
        return utilityInstance;
    }

    /**
     * override this method to use your special utility
     * 
     * @return utility instance
     */
    protected GeneratorUtility createUtilityInstance() {
        return new GeneratorUtility();
    }

    /**
     * tries to extract the destination class name. precondition is, that
     * package-path starts equal on model and destination!
     * 
     * @param sourceFile          source class name
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
            modelFile = ""; // only the destination postfix is the name!
        else
            modelFile = unpackaged ? StringUtil.substring(modelFile, ".", null, true) : modelFile.replace('.', '/');
        String path = Util.get("bean.generation.outputpath", DEFAULT_DEST_PREFIX);
        return (path.endsWith("/") ? path : path + "/") + modelFile + getDestinationPostfix();
    }

    protected String getDestinationPostfix() {
        return Util.get("bean.generation.namepostfix", StringUtil.toFirstUpper(extractName(codeTemplate)) + ".java");
    }

    /**
     * extracts the package name from the result of
     * {@link #getDestinationClassName(String, String)}.
     * 
     * @param sourceFile          source class name
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

    public static void main(String args[]) throws Exception {
        start(args, null);
    }

    private static Map<String, Arg<?>> getManual() {
        Collection<Class<ACodeGenerator>> gens = ClassFinder.self().findClass(ACodeGenerator.class);
        List<File> templates =  FileUtil.getFileset("./", "**/*.vm");
        Map<String, Arg<?>> man = new LinkedHashMap<>();
        man.put(KEY_ALGORITHM, new Arg(KEY_ALGORITHM, true, null, new ArrayList<>(gens), null, "(!) generator implementation class", null));
        man.put(KEY_MODEL, new Arg(KEY_MODEL, "(!) model-file to read source informations from"));
        man.put(KEY_TEMPLATE, new Arg(KEY_TEMPLATE, true, null, templates, null, "(!) velocity template-file to generate new code from and input source informations", null));
        man.put(KEY_FILTER, new Arg(KEY_FILTER, "( ) additional source filter"));
        man.put(KEY_PROPERTIES, new Arg(KEY_PROPERTIES, "( ) property-file to read additional informations"));

        man.put("system variables", new Arg("system variables", "\n"
        + " - bean.generation.packagename     : only class in that package\n"
        + " - bean.generation.outputpath      : output base path (default: src/gen)\n"
        + " - bean.generation.nameprefix      : class+package name prefix (default: package + code-template)\n"
        + " - bean.generation.namepostfix     : class name postfix (default: {code-template}.java)\n"
        + " - bean.generation.unpackaged      : no package structure from origin will be inherited (default: false)\n"
        + " - bean.generation.singleFile      : generate only the first occurrency (default: false)\n"));
        return man;
    }
}
