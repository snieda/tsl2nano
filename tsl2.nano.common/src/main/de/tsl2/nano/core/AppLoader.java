/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.classloader.NestedJarClassLoader;
import de.tsl2.nano.core.classloader.ThreadUtil;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Provides an Application Starter with an own extended classloader and a convenience to handle call arguments (the
 * {@link Argumentator}. The goal is to create the new classloader before loading any classes by the parent classloader.
 * thats, why this class uses as less dependencies/imports as possible!
 * <p/>
 * Use:
 * 
 * <pre>
 * - Extend the AppLoader and create the main method, creating a new instance of your extended loader and calling {@link #start(String[])}.
 * - Override {@link #getManual()} to have a check and print of application usage
 * - Override {@link #createEnvironment(Argumentator)} to interpret arguments and set application environment (see {@link Environment}).
 * 
 * Example:
 * 
 * public class Loader extends AppLoader {
 *    public static void main(String[] args) {
 *        new Loader().start("mypackagepath.MyMainClass", null, args);
 *    }
 * }
 * </pre>
 * 
 * ATTENTION: your extension should do not more than that - to have as less dependencies as possible!
 * 
 * @author ts
 * @version $Revision$
 */
public class AppLoader {
    private static Log LOG = LogFactory.getLog(AppLoader.class);

    private static final String KEY_ISNESTEDJAR = "nano.apploader.isnestedjar";
    
    /**
     * provides a map containing argument names (map-keys) and their description (map-values).
     * 
     * @return map describing all possible main application arguments. Used by {@link Argumentator}.
     */
    protected Map<String, String> getManual() {
        // don't use MapUtil.asMap() to use only core-classes
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("usage", getClass().getSimpleName() + " [environment name or path] {<Main-Class>} [arguments]");
        return map;
    }

    /**
     * createEnvironment
     * 
     * @param environment environment name/path
     * @param args console call arguments (see {@link Argumentator}) to be interpreted.
     */
    protected void createEnvironment(String environment, Argumentator args) {
        System.setProperty(environment, environment + "/");
        BeanClass.createBeanClass("de.tsl2.nano.core.Environment").callMethod(null,
            "create",
            new Class[] { String.class },
            environment);
//        Environment.create(environment);
//        LOG = LogFactory.getLog(AppLoader.class);
    }

    /**
     * extracts mainclass and environment from args and delegates to {@link #start(String, String, String[])}.
     * 
     * @param args main args (must not be null!)
     */
    public void start(String[] args) {
        String mainclass;
        String mainmethod;
        String environment;
        String[] nargs;
        if (args.length == 1) {
            environment = getFileSystemPrefix() + "config";
            mainclass = args[0];
            mainmethod = "main";
            nargs = new String[0];
        } else {
            environment = args[0];
            mainclass = args[1];
            int processed = 2;
            mainmethod = args.length > 2 ? args[processed++] : "main";

            nargs = new String[args.length - processed];
            System.arraycopy(args, processed, nargs, 0, nargs.length);
        }
        start(mainclass, mainmethod, environment, nargs);
    }

    /**
     * delegates to {@link #start(String, String, String, String[])}
     * 
     * @param mainclass main-class to start it's main method with new classloader. must not be null!
     * @param args main args (must not be null!)
     */
    public void start(String mainclass, String[] args) {
        start(mainclass, null, null, args);
    }

    /**
     * starts application loading. will be called by the static main method to work inside an extended class instance.
     * 
     * @param mainclass main-class to start it's main method with new classloader. must not be null!
     * @param environment (optional, default: config) environment name or path
     * @param args main args (must not be null!)
     */
    public void start(String mainclass, String environment, String mainmethod, String[] args) {
        /*
         * check and use the AppLoaders main arguments
         */
        if (isHelpRequest(args)) {
            printHelp();
        }
        if (environment == null) {
            if (args.length > 0) {
                environment = args[0];
                String[] nargs = new String[args.length - 1];
                System.arraycopy(args, 1, nargs, 0, nargs.length);
                args = nargs;
            } else {
                environment = getFileSystemPrefix() + "config";
            }
        }

        if (mainmethod == null)
            mainmethod = "main";

        LOG.info("\n#############################################################"
            + "\nAppLoader preparing launch for:\n  mainclass : "
            + mainclass
            + "\n  mainmethod: "
            + mainmethod
            + "\n  args      : "
            + StringUtil.toString(args, 200)
            + "\n"
            + "  environment: "
            + environment
            + "\n"
            + "#############################################################\n");
        /*
         * create the classloader to be used by the new application
         */
        new File(environment).mkdirs();
        provideClassloader(environment);

        BeanClass<?> bc = BeanClass.createBeanClass(mainclass);

        /*
         * now, we can load the environment with properties and services
         */
        createEnvironment(environment, new Argumentator(bc.getName(), getManual(), args));

        /*
         * prepare cleaning the Apploader
         */
        ThreadUtil.startDaemon("apploader-clean", new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    LOG = null;
                    //stop old logfactory instance
                    LogFactory.stop();
                }
            }
        });

        /*
         * finally, the application will be started inside the
         * new environment and classloader
         */
        bc.callMethod(null, mainmethod, new Class[] { String[].class }, new Object[] { args });
    }

    /**
     * isHelpRequest
     * 
     * @param args arguments to check
     * @return true, if arguments describe a help request
     */
    protected boolean isHelpRequest(String[] args) {
        return args.length == 0 || args[0].matches(".*(\\?|help|man)");
    }

    /**
     * printHelp
     * 
     * @param args
     */
    protected void printHelp() {
        Argumentator.printManual(getManual());
    }

    /**
     * main - to be delegated by your extension
     * 
     * @param args console call arguments
     */
    public static void main(String[] args) {
        new AppLoader().start(args);
    }

    /**
     * creates and sets a new extended classloader for the current threads context
     * 
     * @param environment name/path to be added to the classloader
     */
    protected void provideClassloader(String environment) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        String classPath = System.getProperty("java.class.path");
        /*
         * there are two mechanisms: loading from jar, or loading in an IDE from classpath.
         * 1. loading from a jar-file, the previous classloader must not be given to the new one!
         *    so we have to add the jar-file itself ('java.classpath') and the user.dir to the path.
         * 2. loading from IDE-classpath, we have to use the parent classloader
         */
        ClassLoader cl = classPath.contains(";") || isDalvik() ? contextClassLoader : null;
        NestedJarClassLoader nestedLoader = new NestedJarClassLoader(cl);
        nestedLoader.addLibraryPath(new File(environment).getAbsolutePath());
        if (cl == null) {
            nestedLoader.addFile(classPath);
//            String configDir = System.getProperty("user.dir") + "/" + environment + "/";
//            nestedLoader.addLibraryPath(new File(configDir).getAbsolutePath());
            System.setProperty(KEY_ISNESTEDJAR, Boolean.toString(true));
        } else {
            System.setProperty(KEY_ISNESTEDJAR, Boolean.toString(false));
        }
        nestedLoader.startPathChecker(environment, 2000);
        System.out.println("resetting current thread classloader " + contextClassLoader + " with " + nestedLoader);
        Thread.currentThread().setContextClassLoader(nestedLoader);
    }

    /**
     * convenience to insert new args to the given args
     * 
     * @param args standard arguments
     * @param preArgs arguments to be inserted before.
     * @return new String[] holding preArgs + args
     */
    protected String[] extendArgs(String[] args, String... preArgs) {
        // don't use CollectionUtil.concat(String[].class, preArgs, args) to import only nano-core classes
        String[] newArgs = new String[args.length + preArgs.length];
        System.arraycopy(preArgs, 0, newArgs, 0, preArgs.length);
        System.arraycopy(args, 0, newArgs, preArgs.length, args.length);
        return newArgs;
    }

    public static String getFileSystemPrefix() {
        //on dalvik systems, the  MainActivity.onCreate() should set the syste property
        return isDalvik() ? System.getProperty("android.sdcard.path", "/mnt/sdcard/") : isUnixFS() && new File("/opt").canWrite() ? "/opt/" : "";
    }

    /**
     * isDalvik
     * 
     * @return true if this app is started on android
     */
    public static final boolean isDalvik() {
        return System.getProperty("java.vm.specification.name").startsWith("Dalvik");
    }

    public static final boolean isUnixFS() {
        //TODO: eval the real file-system
        return File.pathSeparatorChar == ':';
    }

    public static boolean isNestingJar() {
        return Boolean.getBoolean(KEY_ISNESTEDJAR);
    }
}
