/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 07.12.2008
 * 
 * Copyright: (c) Thomas Schneider 2008, all rights reserved
 */
package de.tsl2.nano.util.codegen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * <pre>
 * This class is only used manually to recreate the default presenters.
 * It is able to load model classes from a given path or a given jar-file.
 * 
 * If you use a jar-file, be sure to set the classpath to find all needed classes!
 * 
 * how-to-start: start the main of this class with parameter:
 *   1. package path to find model classes to generate presenters for
 *   2. class name of presenter specialization
 *   3. property-file name
 *   
 * example:
 *  PackageGenerator bin/org/anonymous/project/ de.tsl2.nano.codegen.PresenterGenerator
 *  PackageGenerator lib/mymodel.jar de.tsl2.nano.codegen.PresenterGenerator
 *  
 * To constrain the generation to a specific package path, set the environment variable "bean.generation.packagename".
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class PackageGenerator extends ClassGenerator {
    private static final String POSTFIX_PACKAGE = "presenter";
    Properties properties = null;
    private String packagePath;

    /**
     * main
     * 
     * @param args will be ignored
     * @throws Exception on any error
     */
    public static void main(String args[]) throws Exception {
        if (args.length == 0 || args.length > 3) {
            String help = 
                    "syntax : PackageGenerator <package-file-path> [code-template] [[presenter-class-name [property-file]]\n"
                  + "example: PackageGenerator bin/mylocale/mycompany/mypackagepath codegen/beanconstant.vm de.tsl2.nano.codegen.PackageGenerator\n"
                  + "\nreading system variables:\n"
                  + " - bean.generation.packagename: only class in that package\n"
                  + " - bean.generation.outputpath : output base path\n"
                  + " - bean.generation.namepostfix: class name postfix (default: Const.java)\n";
            System.out.println("help");
            System.exit(1);
        }

        PackageGenerator gen;
        if (args.length > 2) {
            gen = (PackageGenerator) ClassGenerator.instance(args[2]);
            if (args.length > 3 && args[3].length() > 0) {
                gen.getProperties().putAll(FileUtil.loadProperties(args[3], Thread.currentThread().getContextClassLoader()));
            }
        } else {
            gen = (PackageGenerator) ClassGenerator.instance(new PackageGenerator());
        }
        gen.packagePath = args[0];
        gen.codeTemplate = args.length > 1 ? args[1] : "codegen/beanconstant.vm";
        gen.generate();
    }

    /**
     * starts the generate process
     */
    @SuppressWarnings("rawtypes")
    public void generate() {
        final Collection<Class> classes = getModelClasses();
        for (final Iterator<Class> iterator = classes.iterator(); iterator.hasNext();) {
            try {
                generate(iterator.next());
            } catch (final Exception e) {
                ManagedException.forward(e);
            }
        }
    }

    /**
     * override this class to define your model classes
     * 
     * @return all types to generate information classes for
     */
    @SuppressWarnings({ "rawtypes" })
    protected Collection<Class> getModelClasses() {
        final ClassLoader classLoader = getDefaultClassloader();
        Collection<Class> modelClasses;
        String[] classNames;
        String p;
        if (packagePath.endsWith(".jar")) {
            p = null;
            classNames = FileUtil.readFileNamesFromZip(packagePath, "*" + POSTFIX_CLS);
            if (classNames == null) {
                throw new ManagedException("the given jar-file doesn't exist!");
            }
        } else {
            final File packageFilePath = new File(packagePath);
            if (!packageFilePath.isDirectory()) {
                throw new ManagedException("the given package-file-path is not a directory: " + packagePath);
            }
            if (!packageFilePath.canRead()) {
                throw new ManagedException("the given package-file-path is not readable!");
            }
            classNames = packageFilePath.list();
            p = getPackage(classLoader, packagePath.replace('/', '.'), classNames);
        }
        modelClasses = new ArrayList<Class>(classNames.length);
        for (int i = 0; i < classNames.length; i++) {
            if (classNames[i].endsWith(POSTFIX_CLS)) {
                String className = StringUtil.substring(classNames[i], null, POSTFIX_CLS);
                if (p != null) {
                    className = p + "." + className;
                }
                className = className.replace('/', '.');
                LOG.info("loading class: " + className);
                try {
                    modelClasses.add(classLoader.loadClass(className));
                } catch (final Exception e) {
                    ManagedException.forward(e);
                }
            }
        }
        if (modelClasses.isEmpty())
            LOG.warn("NOTHING TO DO: NO CLASSES FOUND IN " + packagePath);
        return modelClasses;
    }

    /**
     * getPackage
     * 
     * @param fullpath path with classpath + package path
     * @param classLoader classloader
     * @return package path
     */
    private String getPackage(ClassLoader classLoader, String fullpath, String[] classNames) {
        String p = fullpath;
        String className = null;
        for (int i = 0; i < classNames.length; i++) {
            if (classNames[i].endsWith(POSTFIX_CLS)) {
                className = StringUtil.substring(classNames[i], null, POSTFIX_CLS);
                className = className.replace('/', '.');
                break;
            }
        }
        if (className == null) {
            LOG.warn("COULDN'T EVALUATE ANY PACKAGE. NO CLASSES FOUND!");
            return p;
        }
        String pckName = Util.get("bean.generation.packagename", null);
        while (true) {
            try {
                Class<?> cls = classLoader.loadClass(p + (p != null ? "." : "") + className);
                if (pckName == null || cls.getName().matches(pckName + ".*")) {
                    break; //-->Ok
                }
                LOG.info("ignoring class " + cls.getName() + " not beeing in package " + pckName);
            } catch (final Exception e) {
                LOG.debug(e);
            }
            //evaluate the package-path for the classloader
            if (p != null) {
                final int ii = p.indexOf('.');
                if (ii == -1) {
                    throw new ManagedException("can''t evaluate the class package path for " + packagePath);
                }
                p = p.substring(ii + 1);
            }
        }
        return p;
    }

    /**
     * starts the generator for the given model class
     * 
     * @param type type to generate
     * @throws Exception on any error
     */
    protected void generate(Class<?> type) throws Exception {
        final String modelFile = type.getCanonicalName();
        //if the canonical name is null, it's a local or anonymous class
        if (modelFile == null || type.getEnclosingClass() != null) {
            return;
        }
        final String destFile = getDefaultDestinationFile(modelFile);
        final Properties p = getProperties();
        ClassGenerator.instance().generate(modelFile, getTemplate(type), destFile, p, getDefaultClassloader());
        p.put("constClass", getDestinationClassName(modelFile, destFile));
    }

    /**
     * @return properties for the velocity context
     */
    protected Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

    /**
     * override this method, to define the path to your own template.
     * @param type 
     * 
     * @return file name of presenter constant class
     */
    protected String getTemplate(Class<?> type) {
        return codeTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultDestinationFile(String modelFile) {
        modelFile = super.getDefaultDestinationFile(modelFile);
        modelFile = appendPackage(modelFile, POSTFIX_PACKAGE);
        return Util.get("bean.generation.outputpath", "") + "/" + modelFile;
    }

    /**
     * appends the given package suffix to the given model class package path
     * 
     * @param modelFile bean or model java class
     * @param packageName source package
     * @return package + file name
     */
    protected String appendPackage(String modelFile, String packageName) {
        final int i = modelFile.lastIndexOf('/');
        modelFile = modelFile.substring(0, i) + "/"
            + packageName
            + modelFile.substring(modelFile.lastIndexOf('/'), modelFile.length());
        return modelFile;
    }
}
