/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Dec 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.format.DefaultFormat;

/**
 * Generic Application-Environment. Providing:
 * 
 * <pre>
 * - any services
 * - any properties (optionally load from file)
 * - translation with registered resources bundles
 * - default formatting of objects
 * - running code in compatibility mode
 * </pre>
 * <p/>
 * 
 * Goal: only a single class provides any application specific informations. developers don't have to search for
 * different service/singelton classes. The accessors are static while the internal instances are called through a
 * singelton.
 * <p/>
 * TODO: create a const generator EnvironmentConst to be embedded to all specialized application classes to be used to
 * get properties and services
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "static-access" })
@Default(value = DefaultType.FIELD, required = false)
public class Environment {
//    Log LOG = LogFactory.getLog(Environment.class);

    private static Environment self;
    @ElementMap(entry = "property", key = "name", attribute = true, inline = true, required = false, keyType = String.class, valueType = Object.class)
    private Properties properties;

    /**
     * holds all already loaded services - but wrapped into {@link ServiceProxy}. the {@link #serviceLocator} holds the
     * real service instances.
     */
    @ElementMap(entry = "service", key = "interface", attribute = true, inline = true, required = false, keyType = Class.class, valueType = Object.class)
    Map<Class<?>, Object> services;

    public static final String FRAMEWORK = "de.tsl2.nano";
    public static final String PREFIX = Environment.class.getPackage().getName() + ".";

    public static final String KEY_SYS_BASEDIR = PREFIX + ".basedir";
    public static final String KEY_DEFAULT_FORMAT = PREFIX + "defaultformat";
    public static final String KEY_CONFIG_PATH = PREFIX + "config.path";

    public static final String CONFIG_XML_NAME = "environment.xml";

    public static final String KEY_BUILDINFO = "tsl2.nano.build.informations";

    private static final String KEY_TESTMODE = "tsl2.nano.test";

    static final String DEF_PATHSEPRATOR = "/";

    private Environment() {
        self = this;
    }

    /**
     * environment name
     * 
     * @return environment name - equal to the name of the configuration directory, defined in {@link #getConfigPath()}.
     */
    public static String getName() {
        String path = self.getConfigPath().replace(File.separator, DEF_PATHSEPRATOR);
        if (path.lastIndexOf(DEF_PATHSEPRATOR) == path.length() - 1)
            path = path.substring(0, path.length() - 1);
        return StringUtil.toFirstUpper(StringUtil.substring(path, DEF_PATHSEPRATOR, null, true));
    }

    /**
     * getBuildInformations
     * 
     * @return build informations read through build.properties in jar
     */
    public static String getBuildInformations() {
        String buildInfo = System.getProperty(KEY_BUILDINFO);
        if (buildInfo == null) {
            try {
                InputStream biStream =
                    Environment.class.getClassLoader().getResourceAsStream("build-tsl2.nano.h5.properties");
                if (biStream != null) {
                    Properties bi = new Properties();
                    bi.load(biStream);
                    buildInfo = bi.getProperty("build.name") + "-"
                        + bi.getProperty("build.version")
                        + "-"
                        + bi.getProperty("build.number")
                        + "-"
                        + bi.getProperty("build.time")
                        + ("true".equals(bi.getProperty("build.debug")) ? "-d" : "");
                    System.setProperty(KEY_BUILDINFO, buildInfo);
                    System.getProperties().putAll(bi);
                } else {
                    return "<unknown build informations>";
                }
            } catch (Exception e) {
                return "<unknown build informations>";
            }
        }
        return buildInfo;
    }

    /**
     * provides services through their interface class. if not existing, it tries to load it from xml. if not available,
     * it tries to create it from default construction.
     * 
     * @param <T> service type
     * @param service interface of service
     * @return implementation of service
     */
    public static <T> T get(Class<T> service) {
        Object s = services().get(service);
        if (s == null) {
            self().log("no service found for " + service);
            self().log("available services:\n" + StringUtil.toFormattedString(services(), 500, true));
            String path = getConfigPath(service) + ".xml";
            if (new File(path).canRead()) {
                self().log("loading service from " + path);
                self().addService(service, self().get(XmlUtil.class).loadXml(path, service));
            } else if (BeanClass.hasDefaultConstructor(service)) {
                self().log("trying to create service " + service + " through default construction");
                self().addService(BeanClass.createInstance(service));
            }
        }
        return (T) services().get(service);
    }

    /**
     * isAvailable
     * 
     * @return true, if environment was already created
     */
    public final static boolean isAvailable() {
        return self != null;
    }

    protected final static Environment self() {
        if (self == null) {
            create(System.getProperty(KEY_CONFIG_PATH, System.getProperty("user.dir").replace('\\', '/')));
        }
        return self;
    }

    public static void create(String dir) {
        new File(dir).mkdirs();
        LogFactory.setLogFile(dir + "/" + "logfactory.log");
        LogFactory.setLogFactoryXml(dir + "/" + "logfactory.xml");

        String info = "\n===========================================================\n" + "creating environment "
            + dir
            + "\n"
            + "    build : ${build.info}\n"
            + "    args  : ${sun.java.command}\n"
            + "    dir   : ${user.dir}\n"
            + "    time  : ${nano.tstamp}\n"
            + "    user  : ${user.name}, home: ${user.home}\n"
            + "    lang  : ${user.country}_${user.language}, encoding: ${sun.jnu.encoding}\n"
            + "    encode: ${file.encoding}\n"
            + "    loader: ${main.context.classloader}\n"
            + "    java  : ${java.runtime.version}, ${java.home}\n"
            + "    os    : ${os.name}, ${os.version} ${sun.os.patch.level} ${os.arch}\n"
            + "    system: ${sun.cpu.isalist} ${sun.arch.data.model}\n"
            + "    net-ip: ${inetadress.myip}\n"
            + "===========================================================";
        Properties p = new Properties();
        p.putAll(System.getProperties());
        p.put("nano.tstamp", new Date());
        p.put("main.context.classloader", Thread.currentThread().getContextClassLoader());
        p.put("inetadress.myip", NetUtil.getMyIP());

        //provide some external functions as options for this framework
        CompatibilityLayer layer = new CompatibilityLayer();
        layer.registerMethod("ant",
            "de.tsl2.nano.execution.ScriptUtil",
            "ant",
            true,
            String.class,
            String.class,
            Properties.class);

        layer.registerMethod("reflectionToString",
            "de.tsl2.nano.format.ToStringBuilder",
            "reflectionToString",
            true,
            Object.class);

        File configFile = new File(dir + "/" + CONFIG_XML_NAME);//new File(System.getProperty(KEY_CONFIG_PATH, System.getProperty("user.dir")));
        if (configFile.canRead()) {
            self = XmlUtil.loadXml(configFile.getPath(), Environment.class, layer, false);
//            String configPath = getConfigPath();
//            if (!configPath.endsWith("/") && !configPath.endsWith("\\"))
//                setProperty(KEY_CONFIG_PATH, configPath + "/");
        } else {
            self = new Environment();
            self.properties = new Properties();
//          LOG.warn("no environment.properties available");
        }
        p.put("build.info", getBuildInformations());
        info = StringUtil.insertProperties(info, p);
        LogFactory.log(info);

        self.properties.put(KEY_CONFIG_PATH, new File(dir).getAbsolutePath().replace("\\", "/") + "/");
        new File(self.getTempPath()).mkdir();
        self.services = new Hashtable<Class<?>, Object>();
        registerBundle(PREFIX + "messages", true);
        if (new File("messages.properties").canRead())
            registerBundle("messages", true);
        addService(Profiler.class, Profiler.si());
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        addService(UncaughtExceptionHandler.class, exceptionHandler);
        addService(layer);
        //add frameworks beandef classes as standard-types
//        BeanUtil.addStandardTypePackages("de.tsl2.nano.bean.def");
//        self.persist();
    }

    /**
     * provides all loaded services
     * 
     * @return map of loaded services
     */
    protected final static Map<Class<?>, Object> services() {
        return self().services;
    }

    /**
     * manually add a service to the environment
     * 
     * @param service service to add (should implement at least one interface.
     */
    public static <T> T addService(T service) {
        return addService((Class<T>) (service.getClass().getInterfaces().length > 0 ? service.getClass()
            .getInterfaces()[0] : service.getClass()), service);
    }

    /**
     * manually add a service to the environment
     * 
     * @param service service to add (should implement at least one interface.
     */
    public static <T> T addService(Class<T> interfaze, T service) {
        services().put(interfaze, service);
        self().log("adding service '" + interfaze + "' with implementation " + service);
        return service;
    }

    /**
     * resets the current environment to be empty
     */
    public static void reset() {
        self = null;
    }

    /**
     * WARNING: deletes all config files. no reset on Environment will be done!
     */
    public static boolean deleteAllConfigFiles() {
        File config = new File(getConfigPath());
        boolean done = config.delete();
        if (done) {
            config.mkdirs();
        }
        return done;
    }

    /**
     * fast convenience to get an environment property. see {@link #getProperties()} and {@link #getProperty(String)}.
     * 
     * @param key property key
     * @return property value
     */
    public static final Object get(String key) {
        return get(key, null);
    }

    public static final <T> T get(String key, T defaultValue) {
        T value = (T) self().properties.get(key);
        if (value == null && defaultValue != null) {
            value = defaultValue;
            setProperty(key, value);
        }
        return value;
    }

    /**
     * convenience to get an environment property
     * 
     * @param key property key
     * @return property value
     */
    public static String getProperty(String key) {
        return self().properties.getProperty(key);
    }

    /**
     * @return Returns the properties.
     */
    public static Properties getProperties() {
        return self().properties;
    }

    /**
     * setProperty
     * 
     * @param key key
     * @param value value
     */
    public static void setProperty(String key, Object value) {
        self().properties.put(key, value);
//        FileUtil.saveProperties(CONFIG_FILE_NAME, self().properties);
    }

    /**
     * @param properties The properties to set.
     */
    public static void setProperties(Properties properties) {
        self().properties = properties;
//        FileUtil.saveProperties(CONFIG_FILE_NAME, self().properties);
    }

    /**
     * removes the service, given by it's interface.
     * 
     * @param clazz interface of service
     */
    public static void removeService(Class<?> clazz) {
        services().remove(clazz);
    }

    /**
     * delegates to {@link Messages#registerBundle(ResourceBundle, boolean)}
     * 
     * @param bundlePath new bundle
     * @param head whether to add the bundle on top or bottom.
     */
    public static void registerBundle(String bundlePath, boolean head) {
        ResourceBundle bundle = ResourceBundle.getBundle(bundlePath, Locale.getDefault(), Thread.currentThread()
            .getContextClassLoader());
        self().log("registering resource bundle '" + bundlePath + "'");
        Messages.registerBundle(bundle, head);
    }

    /**
     * Get the translation for the given key from the ResourceBundle pool (see
     * {@link #registerBundle(ResourceBundle, boolean)}. optional translation. tries to get the translation from
     * resoucebundle pool. if not found, the naming part of the key will be returned.
     * 
     * @param key the bundle key
     * @param optional if true, a first-upper pure string will be returned, if not found inside any bundle
     * @param args if given, optional has to be false to insert the arguments into the message
     * @return bundle value the translated value or the key itself if no translation is available
     */
    public static String translate(Object key, boolean optional, Object... args) {
        if (key instanceof Enum)
            return Messages.getString((Enum<?>) key);
        else {
            if (optional && args.length == 0)
                return Messages.getStringOpt((String) key, true);
            else if (args.length > 0)
                return Messages.getFormattedString((String) key, args);
            else
                return Messages.getString((String) key);
        }
    }

//    public static String translate(String key, Object... args) {
//        return Messages.getFormattedString((String) key, args);
//    }
//
    /**
     * formats the given object
     * 
     * @param obj object to format
     * @return formatted object
     */
    public static String format(Object obj) {
        Object formatter = get(KEY_DEFAULT_FORMAT, DefaultFormat.class.getName());
        if (!(formatter instanceof Format)) {
            if (formatter instanceof String) {
                formatter = BeanClass.createBeanClass((String) formatter).createInstance();
            } else {
                throw new IllegalArgumentException(
                    "environments default formatter must be an instance of java.text.Format but is: "
                        + formatter.getClass());
            }
            self().setProperty(KEY_DEFAULT_FORMAT, formatter);
        }
        return ((Format) formatter).format(obj);
    }

    /**
     * getConfigPath
     * 
     * @param type type to evaluate simple file name from
     * @return default environment directory + simple class name
     */
    public static String getConfigPath(Class<?> type) {
        return getConfigPath() + type.getSimpleName().toLowerCase();
    }

    public static String getConfigPath() {
        return self().properties.getProperty(KEY_CONFIG_PATH);
    }

    public static String getTempPath() {
        return getConfigPath() + "temp/";
    }

    /**
     * getMainPackage
     * 
     * @return application main package (stored in 'application.main.package').
     */
    public static String getApplicationMainPackage() {
        String pck = (String) get("application.main.package");
        if (pck == null) {
            pck = "org.nano." + getName().toLowerCase().trim();
            self().log("WARNING: no 'application.main.package' defined in environment! using default: " + pck);
            self().setProperty("application.main.package", pck);
        }
        return pck;
    }

    public static <T> T load(String name, Class<T> type) {
        return self().get(XmlUtil.class).loadXml(getConfigPath(type) + ".xml", type);
    }

    /**
     * persists the given object through configured xml persister.
     * 
     * @param obj object to serialize to xml.
     */
    public static void persist(Object obj) {
        self().get(XmlUtil.class).saveXml(getConfigPath(obj.getClass()) + ".xml", obj);
    }

    /**
     * isPersisted
     * 
     * @return true, if an environment was created on file system
     */
    public final static boolean isPersisted() {
        return new File(getConfigPath() + CONFIG_XML_NAME).exists();
    }

    /**
     * persists the current environment - all transient properties and services will be lost!!!
     */
    public final static void persist() {
        Properties tempProperties = new Properties();
        tempProperties.putAll(self().properties);
        Map<Class<?>, Object> tempServices = new Hashtable<Class<?>, Object>(services());

        String configPath = getConfigPath();
        self().get(XmlUtil.class).saveXml(configPath + CONFIG_XML_NAME, self());

        services().putAll(tempServices);
        self().properties.putAll(tempProperties);
    }

    /**
     * reloads the current environment. a reset will be done to reload the environment from saved file.
     */
    public static void reload() {
//        Properties properties = self().properties;
//        Properties p = new Properties();
//        Set<Object> keys = properties.keySet();
//        for (Object k : keys) {
//            p.put(k, properties.get(k));
//        }
//        try {
//            FileUtil.saveProperties(CONFIG_FILE_NAME, p);
//        } catch (Exception e) {
//            get(Log.class).warn(e);
//        }
//
//        self().get(XmlUtil.class).saveXml(SERVICE_FILE_NAME, self().services);

        Properties tempProperties = new Properties();
        tempProperties.putAll(self().properties);
        Map<Class<?>, Object> tempServices = new Hashtable<Class<?>, Object>(services());

        reset();
        create(getConfigPath());

        services().putAll(tempServices);
        self().properties.putAll(tempProperties);
    }

    /**
     * assignClassloaderToCurrentThread
     */
    public static void assignClassloaderToCurrentThread() {
        ClassLoader cl = (ClassLoader) self().services.get(ClassLoader.class);
        if (cl != null)
            Thread.currentThread().setContextClassLoader(cl);
        else {
            addService(ClassLoader.class, Thread.currentThread().getContextClassLoader());
//            get(Log.class).warn("no classloader defined!");
        }
    }

    /**
     * loads a resource file through the environments classloader
     * 
     * @param fileName resource
     * @return inputstream
     */
    public static InputStream getResource(String fileName) {
        ClassLoader classLoader = (ClassLoader) self().services.get(ClassLoader.class);
        if (classLoader == null)
            classLoader = addService(Thread.currentThread().getContextClassLoader());
        return classLoader.getResourceAsStream(fileName);
    }

    public static final boolean extractResourceToDir(String resourceName, String destinationDir) {
        //put build informations into system-properties
        getBuildInformations();
        resourceName = System.getProperty(resourceName, resourceName);
        return AppLoader.isNestingJar() ? extractResource(resourceName, destinationDir + resourceName) : false;
    }

    /**
     * saveResourceToFileSystem
     * 
     * @param resourceName any resource nested in application jar
     * @return true, if resource was saved. if application wasn't started from jar (perhaps in an ide), it returns
     *         always false.
     */
    public static final boolean extractResource(String resourceName) {
        //put build informations into system-properties
        getBuildInformations();
        resourceName = System.getProperty(resourceName, resourceName);
        return AppLoader.isNestingJar() ? extractResource(resourceName, resourceName) : false;
    }

    /**
     * copies the existing file in environment to the temp dir.
     * 
     * @param environmentFile
     */
    public static final void saveBackup(String environmentFile) {
        FileUtil.copy(getConfigPath() + environmentFile,
            getTempPath() + environmentFile + "." + System.currentTimeMillis());
    }

    /**
     * saves the resource (contained in your jar) to the file-system into the {@link #getConfigPath()} - only if not
     * done yet.
     * 
     * @param resourceName resource name
     * @return true if new file was created
     */
    public static final boolean extractResource(String resourceName, String fileName) {
        File destFile = new File(fileName);
        File file = destFile.isAbsolute() ? destFile : new File(getConfigPath() + fileName);
        if (!file.exists()) {
            InputStream res = get(ClassLoader.class).getResourceAsStream(resourceName);
            try {
                if (res == null /*|| res.available() <= 0*/)
                    throw new IllegalStateException("the resource '" + resourceName
                        + "' of our main-jar-file is not available or empty!");
                FileUtil.write(res, new FileOutputStream(file), true);
                return true;
            } catch (Exception e) {
                ManagedException.forward(e);
            }
        }
        return false;
    }

    /**
     * searches for the given jar-file names in current classloader and to loads them perhaps through an
     * internet-repository (like with maven).
     * 
     * @param dependencyNames jar-file names to check.
     * @return any loader information
     */
    public static final Object loadJarDependencies(String... dependencyNames) {
        //evaluate already loaded jars (no class-cast possible --> using reflection!)
        String[] nestedJars =
            (String[]) BeanClass.call(Thread.currentThread().getContextClassLoader().getParent(), "getNestedJars");
        File[] environmentJars = FileUtil.getFiles(getConfigPath(), ".*[.]jar");
        Collection<String> availableJars =
            new ArrayList<String>((nestedJars != null ? nestedJars.length : 0) + environmentJars.length);
        if (nestedJars != null)
            availableJars.addAll(Arrays.asList(nestedJars));
        for (int i = 0; i < environmentJars.length; i++) {
            availableJars.add(environmentJars[i].getName());
        }

        //check given dependencies
        List<String> unresolvedDependencies = new ArrayList<String>(dependencyNames.length);
        for (int i = 0; i < dependencyNames.length; i++) {
            if (!availableJars.contains(dependencyNames[i])) {
                unresolvedDependencies.add(dependencyNames[i]);
            }
        }
        if (unresolvedDependencies.size() > 0) {
            return loadDependencies(unresolvedDependencies.toArray(new String[0]));
        }
        return "nothing to do!";
    }

    public static final Object loadClassDependencies(String... dependencyNames) {
        CompatibilityLayer cl = get(CompatibilityLayer.class);
        //check given dependencies
        List<String> unresolvedDependencies = new ArrayList<String>(dependencyNames.length);
        for (int i = 0; i < dependencyNames.length; i++) {
            if (!cl.isAvailable(dependencyNames[i])) {
                unresolvedDependencies.add(dependencyNames[i]);
            }
        }
        if (unresolvedDependencies.size() > 0) {
            return loadDependencies(unresolvedDependencies.toArray(new String[0]));
        }
        return "nothing to do!";
    }

    /**
     * tries to interpret the given dependency names and to load them perhaps through an internet-repository (like with
     * maven).
     * 
     * @param dependencyNames names including the organisation/product name to be extracted.
     * @return any loader information, or null, if nothing was done.
     */
    public static final Object loadDependencies(String... dependencyNames) {
        //check for own framework dependencies
        boolean foreignDependencies = false;
        for (int i = 0; i < dependencyNames.length; i++) {
            if (!dependencyNames[i].startsWith(FRAMEWORK)) {
                foreignDependencies = true;
                break;
            }
        }
        if (!foreignDependencies)
            return null;
        //load the dependencies through maven, if available
        String clsJarResolver = "de.tsl2.nano.jarresolver.JarResolver";
        if (get("classloader.usenetwork.loader", true) && NetUtil.isOnline()
            && get(CompatibilityLayer.class).isAvailable(clsJarResolver)) {
            Message
                .send("downloading unresolved dependencies: "
                    + StringUtil.toString(dependencyNames, 300));
            get(CompatibilityLayer.class).run(clsJarResolver, "main", new Class[] { String[].class },
                new Object[] { dependencyNames });
        } else {
            throw new IllegalStateException("The following dependencies couldn't be resolved:\n"
                + StringUtil.toFormattedString(dependencyNames, 100, true));
        }
        return "dependency loading successfull";
    }

    public static boolean isTestMode() {
        return Boolean.getBoolean(KEY_TESTMODE);
    }

    @Persist
    protected void initSerialization() {
        /*
         * remove the environment path itself - should not reloaded
         */
        properties.remove(KEY_CONFIG_PATH);

        /*
         * remove all not serialiable objects
         */
        Set<Object> keySet = properties.keySet();
        for (Iterator<?> keyIt = keySet.iterator(); keyIt.hasNext();) {
            Object key = (Object) keyIt.next();
            Object value = properties.get(key);
            if (value != null && !Serializable.class.isAssignableFrom(value.getClass())
                || !BeanUtil.isSingleValueType(value.getClass()))
                keyIt.remove();
        }
        //TODO: sort it by key name
        Set<Class<?>> serviceKeys = services.keySet();
        for (Iterator<?> keyIt = serviceKeys.iterator(); keyIt.hasNext();) {
            Object key = (Object) keyIt.next();
            Object value = services.get(key);
            if (value != null && !Serializable.class.isAssignableFrom(value.getClass()))
                keyIt.remove();
        }
    }

    protected void log(Object obj) {
        System.out.println(obj);
    }

    protected void trace(Object obj) {
        if (true)
            log(obj);
    }

}
