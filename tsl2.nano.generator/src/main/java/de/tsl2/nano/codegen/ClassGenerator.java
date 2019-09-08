package de.tsl2.nano.codegen;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.velocity.VelocityContext;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * 
 * class to generate code from a given file. This basic implementation uses class-files as source to generate java code.
 * 
 * extensions may override the methods:
 * 
 * @see #getModel(String, ClassLoader)
 * @see #getDefaultClassloader()
 * @see #getDefaultDestinationFile(String)
 * 
 *      The default implementation of this basic generator expects a java bean (class package) and creates a java file.
 * 
 *      Howto start (example): mainClass: de.tsl2.nano.util.codegen.ClassGenerator arguments:
 *      bin/codegen/beanclass.vm
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 */
public class ClassGenerator extends ACodeGenerator {
    protected static final Log LOG = LogFactory.getLog(ClassGenerator.class);

    public static final String POSTFIX_CLS = ".class";

    protected void fillVelocityContext(Object model, String modelFile,
            String templateFile,
            String destFile,
            Properties properties,
            final GeneratorUtility util,
            final VelocityContext context) {
        super.fillVelocityContext(model, modelFile, templateFile, destFile, properties, util, context);
        context.put("package", getDestinationPackageName(modelFile, destFile));
        context.put("class", model);
    }

    @Override
    protected GeneratorBeanUtility getUtil() {
        return (GeneratorBeanUtility)super.getUtil();
    }
    /**
     * override this method to use your special utility
     * 
     * @return utility instance
     */
    protected GeneratorUtility createUtilityInstance() {
        return new GeneratorBeanUtility();
    }

    /**
     * override this method to provide your class model (the source class)
     * 
     * @param modelFile source file name (normally a beans java file name)
     * @return
     */
    protected Object getModel(String modelFile) {
        //      assert modelFile.endsWith(".class") : "ClassGenerator.getModel(): The modelFile has to point to the package path of a java class file!";
        try {
            final Class clazz = getDefaultClassloader().loadClass(modelFile);
            return BeanClass.getBeanClass(clazz);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        int isrc = dest.indexOf(src);
        return isrc > -1 ? dest.substring(isrc) : dest;
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
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void start(Properties args) {
        super.initParameter(args);
        generate(getModel(args.getProperty(KEY_MODEL)), args.getProperty(KEY_MODEL), args.getProperty(KEY_TEMPLATE), getProperties());
    }



    public static String extractName(String filePath) {
        return StringUtil.substring(filePath, "/", ".", true);
    }
}
