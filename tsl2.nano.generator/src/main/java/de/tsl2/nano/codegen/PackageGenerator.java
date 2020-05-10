/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: 07.12.2008
 * 
 * Copyright: (c) Thomas Schneider 2008, all rights reserved
 */
package de.tsl2.nano.codegen;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.classloader.RuntimeClassloader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.ClassFinder;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * <pre>
 * This class is able to load model classes from a given path or a given jar-file.
 * 
 * If you use a jar-file, be sure to set the classpath to find all needed classes!
 * 
 * how-to-start: start the main of this class with parameter:
 *   1. package path to find model classes to generate presenters for
 *   2. velocity template file
 *   3. class name of generator specialization (default: PackageGenerator)
 *   4. property-file name (default: null, system-properties will be used anyway)
 *   
 * example:
 *  PackageGenerator bin/org/anonymous/project/ de.tsl2.nano.codegen.PackageGenerator
 *  PackageGenerator lib/mymodel.jar de.tsl2.nano.codegen.PresenterGenerator
 *  
 * To constrain the generation to a specific package path, set the environment variable "bean.generation.packagename".
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class PackageGenerator extends ClassGenerator {
    private String packagePath;
    private String classpathEntry;

    /**
     * main
     * 
     * @param args will be ignored
     * @throws Exception on any error
     */

    @Override
    public void initParameter(Properties args) {
        super.initParameter(args);
        packagePath = args.getProperty("model");
    }

    @Override
    public void start(Properties args) {
        initParameter(args);
        generate();
    }

    /**
     * starts the generate process
     */
    public void generate() {
        final Collection<Class<?>> classes = getModelClasses();
        prepareProperties(classes);
        boolean singleFile = Boolean.getBoolean(KEY_SINGLEFILE);
        for (final Iterator<Class<?>> iterator = classes.iterator(); iterator.hasNext();) {
            try {
                generate(iterator.next());
                if (singleFile)
                    break;
            } catch (final Exception e) {
                ManagedException.forward(e);
            }
        }
    }

    private void prepareProperties(Collection<Class<?>> classes) {
        List<BeanClass<?>> allClasses = classes.stream().map(c -> BeanClass.getBeanClass(c)).collect(Collectors.toList());
        getProperties().put("allClasses", allClasses);
    }

    /**
     * override this class to define your model classes
     * 
     * @return all types to generate information classes for
     */
    @SuppressWarnings({ "rawtypes" })
    protected Collection<Class<?>> getModelClasses() {
        ClassLoader classLoader = getDefaultClassloader();
        Collection<Class<?>> modelClasses;
        String[] classNames;
        String p;
        if (packagePath.endsWith(".jar")) {
            p = null;
            classNames = FileUtil.readFileNamesFromZip(packagePath, "*" + POSTFIX_CLS);
            if (classNames == null) {
                throw new ManagedException("the given jar-file has no classes or doesn't exist: " + packagePath);
            }
        } else if (packagePath.matches("(\\w+\\.)+\\w+") && !(new File(packagePath).isDirectory())) {
            LOG.info("packagePath was given as java-class-package -> searching through ClassFinder!");
            p = packagePath;
            Collection<Class> classes =  ClassFinder.self().fuzzyFind(packagePath + ".*", Class.class, 0, null).values();
            classNames = new String[classes.size()];
            int i = 0;
            for (Class cls : classes) {
                classNames[i++] = cls.getName();
            }
        } else {
            File packageFilePath = new File(packagePath);
            if (!packageFilePath.isDirectory()) {
                if (packageFilePath.getParentFile().isDirectory())
                    if (packagePath.contains(".") && (packageFilePath = toFilePath(packageFilePath)).isDirectory())
                        LOG.info("packagePath transformed to: " + packageFilePath.getPath());
                    else
                        throw new ManagedException("the given package-file-path is not a directory: " + packagePath);
            }
            if (!packageFilePath.canRead()) {
                throw new ManagedException("the given package-file-path is not readable: " + packagePath);
            }
            classNames = packageFilePath.list();
            p = packagePath.replace('/', '.');
            Class<?> classInPackage = findClassInPackage(classLoader, p, classNames);
            if (classInPackage != null) {
                p = classInPackage.getPackage().getName();
                classLoader = classInPackage.getClassLoader();
            }
        }
        modelClasses = new ArrayList<>(classNames.length);
        String pckName = Util.get(KEY_PACKAGENAME, null);
        for (int i = 0; i < classNames.length; i++) {
            if (classNames[i].endsWith(POSTFIX_CLS)) {
                String className = StringUtil.substring(classNames[i], null, POSTFIX_CLS);
                if (p != null) {
                    className = p + "." + className;
                }
                className = className.replace('/', '.');
                if (pckName != null && !className.matches(pckName + ".*")) {
                    LOG.info("ignoring filtered class: " + className);
                    continue;
                }
                LOG.info("trying to load class: " + className);
                try {
                    Class cls = classLoader.loadClass(className);
                    if (checkClassFilter(cls)) {
                        modelClasses.add(cls);
                    }
                } catch (final Exception e) {
                    ManagedException.forward(e);
                }
            }
        }
        if (modelClasses.isEmpty())
            LOG.warn("NOTHING TO DO: NO CLASSES FOUND IN " + packagePath);
        return modelClasses;
    }

    private boolean checkClassFilter(Class<?> cls) {
        boolean instanceable = Boolean.getBoolean(KEY_INSTANCEABLE);
        if (instanceable && !Util.isInstanceable(cls)) {
            LOG.info("ignoring not 'instanceable' class: " + cls.getName());
            return false;
        }
        String annotated = System.getProperty(KEY_ANNOTATED);
        if (annotated != null) {
            if (!cls.isAnnotationPresent(BeanClass.load(annotated))) {
                LOG.info("ignoring  class not annotated with " + annotated + ": " + cls.getName());
                return false;
            }
        }
        String instanceOf = System.getProperty(KEY_INSTANCEOF);
        if (instanceOf != null) {
            if (!BeanClass.load(instanceOf).isAssignableFrom(cls)) {
                LOG.info("ignoring  class not instanceof " + instanceOf + ": " + cls.getName());
                return false;
            }
        }
		return true;
	}

	private File toFilePath(File packageFilePath) {
        return new File(packageFilePath.getParent() + "/" + packageFilePath.getName().replace('.', '/'));
    }

    /**
     * getPackage
     * 
     * @param fullpath    path with classpath + package path
     * @param classLoader classloader
     * @return package path
     */
    private Class<?> findClassInPackage(ClassLoader classLoader, String fullpath, String[] classNames) {
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
            return null;
        }
        String pckName = Util.get(KEY_PACKAGENAME, null);
        RuntimeClassloader extendedClassLoader = null;
        String pClassName = null;
        Class<?> classInPackage = null;
        while (true) {
            try {
                pClassName = p + (p != null ? "." : "") + className;
                classInPackage = classLoader.loadClass(pClassName);
            } catch (final Exception e) {
                LOG.debug("couldn't load class: " + e.toString());
                if (!fullpath.equals(p)) {
                    classpathEntry = StringUtil.substring(packagePath.replace('.', '/'), null, p.replace(".", "/"));
                    LOG.info("reload class with extended classpath: " + classpathEntry);
                    extendedClassLoader = new RuntimeClassloader(new URL[0], classLoader);
                    extendedClassLoader.addFile(classpathEntry);
                    try {
                        classInPackage = extendedClassLoader.loadClass(pClassName);
                        LOG.info("classInPackage: " + classInPackage); //trick to generate an Exception, if 'wrong name' class was loaded!
                    } catch (Throwable e1) { // parent module may collaps on loading that class - but class is only temporarily used!
                        classInPackage = null;
                        pClassName = null;
                        LOG.debug(e1);
                    }
                } else {
                    pClassName = null;
                }
            } finally {
                if (classInPackage != null && pClassName != null) {
                    if (pckName == null || pClassName.matches(pckName + ".*")) {
                        LOG.info("package evaluated: '" + p + "'. starting to load " + classNames.length + " classes");
                        Thread.currentThread().setContextClassLoader(classInPackage.getClassLoader());
                        break; //-->Ok
                    }

                    LOG.info("ignoring class " + pClassName + " not beeing in package " + pckName);
                }
            }
            //evaluate the package-path for the classloader
            if (p != null) {
                final int ii = p.indexOf('.');

                if (ii == -1) {
                    throw new ManagedException("can''t evaluate the class package path for " + packagePath + ". the classloader is not able to load any part of it.");
                }
                p = p.substring(ii + 1);
            }
        }
        return classInPackage;
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
        super.generate(getModel(modelFile), modelFile, getTemplate(type), destFile, p);
        p.put("constClass", getDestinationClassName(modelFile, destFile));
    }

    /**
     * override this method, to define the path to your own template.
     * 
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
        boolean unpackaged = Boolean.getBoolean(KEY_UNPACKAGED);
        return unpackaged ? modelFile : appendPackage(modelFile, extractName(codeTemplate));
    }

    /**
     * appends the given package suffix to the given model class package path
     * 
     * @param modelFile   bean or model java class
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
