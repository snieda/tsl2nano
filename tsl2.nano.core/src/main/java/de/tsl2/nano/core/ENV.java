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

import static de.tsl2.nano.core.execution.CompatibilityLayer.TSL2_JARRESOLVER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.classloader.LibClassLoader;
import de.tsl2.nano.core.classloader.NetworkClassLoader;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.ExceptionHandler;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.ChangeEvent;
import de.tsl2.nano.core.serialize.XmlUtil;
import de.tsl2.nano.core.serialize.YamlUtil;
import de.tsl2.nano.core.update.Updater;
import de.tsl2.nano.core.util.CLI;
import de.tsl2.nano.core.util.CLI.Color;
import de.tsl2.nano.core.util.DefaultFormat;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Generic Application-Environment. Providing:
 * 
 * <pre>
 * - any services
 * - any properties (optionally load from file)
 * - translation with registered resources bundles
 * - default formatting of objects
 * - running code in compatibility mode
 * - conveniences for logging
 * </pre>
 * <p/>
 * 
 * Goal: only a single class provides any application specific informations. developers don't have to search for
 * different service/singelton classes. The accessors are static while the internal instances are called through a
 * singelton.
 * <p/>
 * NOTE: thread unsafe!
 * <p/>
 * TODO: create a const generator EnvironmentConst to be embedded to all specialized application classes to be used to
 * get properties and services
 * TODO: use IPreferences mechanism to replace all string-named-properties with enumerated properties.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "static-access" })
@Default(value = DefaultType.FIELD, required = false)
public class ENV implements Serializable {

	/** serialVersionUID */
    private static final long serialVersionUID = 5988200267214868670L;

    public static final String PATH_TEMP = "temp/";
    // workaround on new threads that should inherit the ENV from last creation -> thread unsafe
    private static ENV lastCreated;
    private static ThreadLocal<ENV> selfThread = new ThreadLocal<ENV>();
    
    @SuppressWarnings("rawtypes")
    @ElementMap(entry = "property", key = "name", attribute = true, inline = true, required = false, keyType = String.class, valueType = Object.class)
    private SortedMap properties;

    /**
     * holds all already loaded services - but wrapped into {@link ServiceProxy}. the {@link #serviceLocator} holds the
     * real service instances.
     */
    @ElementMap(entry = "service", key = "interface", attribute = true, inline = true, required = false, keyType = Class.class, valueType = Object.class)
    private Map<Class<?>, Object> services;

    /** if true, this environment will be persisted on any change. */
    transient boolean autopersist = false;

    //TODO: constrain the max size
    transient Map<Class<?>, Log> loggers = new HashMap<Class<?>, Log>();

    public static final String FRAMEWORK = Util.FRAMEWORK_PACKAGE;
    public static final String PREFIX = ENV.class.getPackage().getName() + ".";

    public static final String KEY_SYS_BASEDIR = PREFIX + "basedir";
    public static final String KEY_DEFAULT_FORMAT = PREFIX + "defaultformat";
    public static final String KEY_CONFIG_RELPATH = PREFIX + "config.relative.path";
    public static final String KEY_CONFIG_PATH = PREFIX + "config.path";

	public static final String PREFIX_ENVNAME = ".nanoh5.";
    public static final String CONFIG_NAME = "environment";
    public static final String KEY_BUILDINFO = "tsl2.nano.build.informations";

    public static final String KEY_TESTMODE = "tsl2.nano.test";

    static final String DEF_PATHSEPRATOR = "/";
    static final String UNKNOWN_BUILD_INFORMATIONS = "<unknown build informations>";

    private ENV() {
    }

    /**
     * environment name
     * 
     * @return environment name - equal to the name of the configuration directory, defined in {@link #getConfigPath()}.
     */
    public static String getName() {
        String path = self().getConfigPath().replace(File.separator, DEF_PATHSEPRATOR);
        if (path.lastIndexOf(DEF_PATHSEPRATOR) == path.length() - 1) {
            path = path.substring(0, path.length() - 1);
        }
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
                    ENV.class.getClassLoader().getResourceAsStream("build-tsl2.nano.h5.properties");
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
                    bi.keySet().removeAll(System.getProperties().keySet());
                    System.getProperties().putAll(bi);
                } else {
                    return UNKNOWN_BUILD_INFORMATIONS;
                }
            } catch (Exception e) {
                return UNKNOWN_BUILD_INFORMATIONS;
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
    public static synchronized <T> T get(Class<T> service) {
        T serviceImpl = (T) services().get(service);
        if (serviceImpl == null) {
            ENV self = self();
            debug(self, "no service found for " + service);
            debug(self, "available services:\n" + StringUtil.toFormattedString(services(), 500, true));
            String path = getConfigPath(service) + getFileExtension();
            if (new File(path).canRead()) {
                self.info("loading service from " + path);
                serviceImpl = self.addService(service, self.get(XmlUtil.class).loadXml(path, service));
            } else if (!service.isInterface()
                && BeanClass.hasDefaultConstructor(service, !Util.isFrameworkClass(service))) {
                self.info("trying to create service " + service + " through default construction");
                serviceImpl = self.addService(BeanClass.createInstance(service));
                if (serviceImpl instanceof Serializable) {
                    get(XmlUtil.class).saveXml(path, serviceImpl);
                }
            }
        }
        return serviceImpl;
    }

    /**
     * isAvailable
     * 
     * @return true, if environment was already created
     */
    public final static boolean isAvailable() {
        ENV self = selfThread.get() != null ? selfThread.get() : lastCreated; // the lastCreated is a workaround...
        return self != null && self.properties != null && self.services != null;
    }

    protected final static ENV self() {
        if (selfThread.get() != null) {
            return selfThread.get();
        } else if (lastCreated != null) {
            selfThread.set(lastCreated);
            return lastCreated;
        } else if (lastCreated == null || lastCreated.properties == null || lastCreated.services == null) {
            System.out.println("WARN: NO ENV was created before. creating a default instance");
            lastCreated = create(
                    System.getProperty(KEY_CONFIG_PATH, System.getProperty("user.dir").replace('\\', '/')));
            selfThread.set(lastCreated);
            return lastCreated;
        } else // will never be reached ;-) - unclean
            throw new IllegalStateException("no environment available. please call ENV.create(...) before!");
    }

    public static ENV create(String dir) {
        ENV self;
        FileUtil.userDirFile(dir).mkdirs();
        dir = dir.endsWith("/") ? dir : dir + "/";
        String name = dir + StringUtil.substring(dir, PREFIX_ENVNAME, "/");
        LogFactory.setLogFile(name + ".log");
        LogFactory.setLogFactoryXml(dir + "logfactory.xml");
        String buildInfo = getBuildInformations();
        
        LogFactory.log("\n===========================================================\n"
            + CLI.tag(new String(FileUtil.getFileBytes("tsl-logo.txt", null)), Color.YELLOW)
            + "creating environment "
            + dir
            + "\n"
            + SystemUtil.createInfo(buildInfo)
            + "===========================================================");

        //provide some external functions as options for this framework
        CompatibilityLayer layer = provideCompatibilityLayer();

        File configFile = getConfigFile(dir, ".xml");//new File(System.getProperty(KEY_CONFIG_PATH, System.getProperty("user.dir")));
        if (configFile.canRead()) {
            self = XmlUtil.loadXml(configFile.getPath(), ENV.class, layer, false, true);
//            String configPath = getConfigPath();
//            if (!configPath.endsWith("/") && !configPath.endsWith("\\"))
//                setProperty(KEY_CONFIG_PATH, configPath + "/");
        } else if ((configFile = getConfigFile(dir, ".yml")).canRead()) {
            self = YamlUtil.load(new File(configFile.getPath()), ENV.class);
        } else {
            self = new ENV();
            selfThread.set(self);
            self.properties = createSyncSortedMap();
            configFile = getConfigFile(dir, getFileExtension());
//          LOG.warn("no environment.properties available");
        }
        selfThread.set(self);
        self.services = createServiceMap();
        addService(layer);
        addService(ClassLoader.class, Util.getContextClassLoader());

        self.properties.put(KEY_CONFIG_RELPATH, dir + (dir.endsWith("/") ? "" : "/"));
        self.properties.put(KEY_CONFIG_PATH, FileUtil.userDirFile(dir).getAbsolutePath().replace("\\", "/") + "/");

        registerBundle(PREFIX + "messages", true);
        if (FileUtil.hasResource("messages.properties")) {
            registerBundle("messages", true);
        }
        addService(Profiler.class, Profiler.si());
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        addService(UncaughtExceptionHandler.class, exceptionHandler);

        self.update(configFile, buildInfo);
        //add frameworks beandef classes as standard-types
//        BeanUtil.addStandardTypePackages("de.tsl2.nano.bean.def");
//        self.persist();
        LogFactory.log("==> ENV " + name + " created successful!");
        return lastCreated = self;
    }

    private static CompatibilityLayer provideCompatibilityLayer() {
        CompatibilityLayer layer = new CompatibilityLayer();
        layer.registerMethod("ant",
            "de.tsl2.nano.execution.ScriptUtil",
            "ant",
            true,
            String.class,
            String.class,
            Properties.class);

        layer.registerMethod("antbuild",
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
        return layer;
    }

    private void update(File configFile, String buildInfo) {
        if (buildInfo == null || UNKNOWN_BUILD_INFORMATIONS.equals(buildInfo)) {
            warn(this, UNKNOWN_BUILD_INFORMATIONS + " --> " + "no version update check");
            return;
        }
        String versionURL = get("app.update.url", "https://sourceforge.net/projects/tsl2nano/files/latest/download?source=navbar");
        String currentVersion = get("app.update.current.version", "0.0.0");
        Updater updater = new Updater("h5",
        		versionURL,
        		currentVersion,
        		ENV.get("app.update.last", new java.sql.Date(System.currentTimeMillis())),
        		ENV.get("app.update.interval.days", 30));
        if (versionURL != null)
            if (updater.checkAndUpdate(currentVersion, versionURL)) {
		        if (updater.run(configFile.getPath(), buildInfo, self()))
		            setProperty("app.update.current.version", buildInfo);
//            } else if (currentVersion == null) {
//	            setProperty("app.update.current.version", buildInfo);
            }
    }

    public File getConfigFile() {
        return getConfigFile(getConfigPath(), getFileExtension());
    }

    private static File getConfigFile(String dir, String ext) {
        return new File(dir + "/" + CONFIG_NAME + ext);
    }

    /**
     * createPropertyMap
     * 
     * @return
     */
    @SuppressWarnings("rawtypes")
    static SortedMap createSyncSortedMap() {
        return Collections.synchronizedSortedMap(new TreeMap());
    }

    /**
     * provides all loaded services
     * 
     * @return map of loaded services
     */
    public final static Map<Class<?>, Object> services() {
        return self().services;
    }

    /**
     * serviceservice* manually add a service to the environment
     * 
     * @param to ( addshould implement at least one interface.
     */
    public static <T> T addService(T service) {
        Class<T> interfaze =
            (Class<T>) (service.getClass().getInterfaces().length > 0 ? service.getClass().getInterfaces()[0] : null);
        if (interfaze == null || interfaze.getName().startsWith("java.lang")
            || interfaze.getName().startsWith("java.io"))
            interfaze = (Class<T>) service.getClass();
        return addService(interfaze, service);
    }

    /**
     * manually add a service to the environment
     * 
     * @param service service to add (should implement at least one interface.
     */
    public static <T> T addService(Class<T> interfaze, T service) {
        services().put(interfaze, service);
        self().info("adding service '" + interfaze + "' with implementation " + service);
        return service;
    }

    /**
     * resets the current environment to be empty
     */
    public static void reset() {
        //TODO: extend from java.io.Closable and walk through all services to close them!
    	//TODO: dependencies
        ResourceBundle.clearCache();
//        PersistableSingelton.clearCache();
//        PersistentCache.clearCache();
        selfThread.set(null);
        lastCreated = null;
    }

    /**
     * WARNING: deletes all config files. no reset on Environment will be done!
     */
    public static boolean deleteEnvironment() {
        if (getConfigPath() == null) {
            warn(ENV.class, "no environment active to be deleted!");
            return false;
        }
        File config = new File(getConfigPath());
        boolean done = FileUtil.deleteRecursive(config);
        if (done) {
            config.mkdirs();
        } else {
            info("couldn't delete environment " + config);
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

    /** 
     * asks the user (if UI is available!) to set the default boolean value for the given key.
     * if not responding (timeout 4000ms) or responding with 'Yes', default value will be used,
     * otherwise the negation of defaultValue will be used 
     */
    public static final boolean getAsking(String key, boolean defaultValue) {
        Object definedValue = get(key, null);
        if (definedValue instanceof String)
            definedValue = Boolean.valueOf((String)definedValue);
        Boolean askedValue = definedValue != null ? (Boolean) definedValue : Message.ask("do you want to set the property <i>" + key + "</i><p/>with default value: <i>" + defaultValue + "</i>", defaultValue);
        return get(key, askedValue);
    }

    /**
     * reads key-value from ENV.properties (stored in environment.xml). First it searches in System.properties. 
     * if not existing in ENV.properties, defaultvalue or system.property will be stored in environment.xml.
     * @param key system or ENV property
     * @param defaultValue default value to be set
     * @return system ENV or default value
     */
    public static final <T> T get(String key, T defaultValue) {
        //system properties win...
    	String sysValue = System.getProperty(key);
    	T value = (T) (sysValue != null ? sysValue : self().properties.get(key));
        if (value == null && defaultValue != null) {
            value = defaultValue;
            setProperty(key, value);
        } else if (value != null) {
    		value = (T) (defaultValue != null ? ObjectUtil.wrap(value, defaultValue.getClass()) : value);
    		if (!self().properties.containsKey(key)) {
    			setProperty(key, value);
    		}
    	}
        return value;
    }

    /**
     * usable as persistable counter (will be stored to file system)
     * 
     * @param key the values name to be changed
     * @param diff addition
     * @return new value
     */
    public static final <T extends Number> T counter(String key, T diff) {
        Object value = self().properties.get(key);
    	assert value != null || diff != null;
        if (value == null && diff != null) {
            value = diff;
        } else {
            if (NumberUtil.isInteger(diff.getClass())) {
                value = (((Number) value).intValue() + diff.intValue());
            } else if (NumberUtil.isFloating(diff.getClass())) {
                value = (((Number) value).doubleValue() + diff.doubleValue());
            } else {
                value = (((Number) value).longValue() + diff.longValue());
            }
        }
        setProperty(key, value);
        return (T) value;
    }

    /**
     * convenience to get an environment property
     * 
     * @param key property key
     * @return property value
     */
    public static String getProperty(String key) {
        return (String) self().properties.get(key);
    }

    /**
     * @return Returns the properties.
     */
    @SuppressWarnings("rawtypes")
    public static SortedMap getProperties() {
        return self().properties;
    }

    public static Map<String, Object> concatProperties(Map<String, Object> context) {
        LinkedHashMap<String, Object> p = new LinkedHashMap<>(getProperties());
        p.putAll(context);
        return p;
    }
    
    /**
     * setProperty
     * 
     * @param key key
     * @param value value
     */
    public static void setProperty(String key, Object value) {
    	Object oldValue = self().properties.get(key);
        self().properties.put(key, value);
        if (self().autopersist) {
            self().persist();
        }
        // if (value != null && Util.isSimpleType(value.getClass()))
        //     System.setProperty(key, value.toString());
        self().handleChange(key, oldValue, value); 
    }

    private synchronized void handleChange(String key, Object oldValue, Object value) {
    	if (!isAvailable())
    		return;
    	ChangeEvent e = null;
    	for (Object s : services.values()) {
			if ( s instanceof IEnvChangeListener) {
				if (e == null) {
					e = new ChangeEvent(key, oldValue, value);
				}
				((IEnvChangeListener) s).accept(e);
			}
		}
	}

	/**
     * @param properties The properties to set.
     */
    @SuppressWarnings("rawtypes")
	public static void setProperties(SortedMap properties) {
        if (self().properties == null) {
            self().properties = createSyncSortedMap();
        	self().properties.putAll(properties);
        } else { // perhaps a property map holding only strings
        	SortedMap p = self().properties;
            properties.forEach( (k, v) -> {
            	if (p.containsKey(k)) 
            		p.put(k, ObjectUtil.wrap(v, p.get(k).getClass()));
            	else 
            		p.put(k, v);});
        }
        Util.trY( () -> self().handleChange("*", null, null), false);
        if (self().autopersist) {
            self().persist();
        }
    }

    public Map<Class<?>, Object> getServices() {
		return services;
	}
    public void setServices(Map<Class<?>, Object> services) {
    	//do nothing - only for bean evaluating...
//    	services.forEach((k, v) -> { if (!k.isAssignableFrom(v.getClass())) throw new IllegalArgumentException(v + " must be of type " + k.getName());} );
//		this.services = services;
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
        ResourceBundle bundle = ResourceBundle.getBundle(bundlePath, Locale.getDefault(), Util.getContextClassLoader());
        self().info("registering resource bundle '" + bundlePath + "'");
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
        if (key instanceof Enum) {
            return Messages.getString((Enum<?>) key);
        } else {
            if (optional && args.length == 0) {
                return StringUtil.replaceAll((CharSequence) key, "[\\w\\.\\:\\\\/]+",
                    new ITransformer<String, String>() {
                        @Override
                        public String transform(String toTransform) {
                            return Messages.getStringOpt(toTransform, true);
                        }
                    });
            } else if (args.length > 0) {
                return Messages.getFormattedString((String) key, args);
            } else {
                return Messages.getString((String) key);
            }
        }
    }

    /**
     * formats the given object
     * 
     * @param obj object to format
     * @return formatted object
     */
    public static String format(Object obj) {
        return self() != null && self().services != null
                ? ((Format) services().getOrDefault(Format.class, new DefaultFormat())).format(obj)
                : new DefaultFormat().format(obj);
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

    /**
     * absolute base path for this environment
     * 
     * @return
     */
    public static String getConfigPath() {
        return getProperty(KEY_CONFIG_PATH);
    }

    /**
     * relative base path for this environment
     * 
     * @return
     */
    public static String getConfigPathRel() {
        return getProperty(KEY_CONFIG_RELPATH);
    }

    /**
     * absolute temp path for this environment
     * 
     * @return
     */
    public static String getTempPath() {
        String tpath = getConfigPath() + PATH_TEMP;
        new File(tpath).mkdir();
		return tpath;
    }

    /**
     * relative temp path for this environment
     * 
     * @return
     */
    public static String getTempPathRel() {
        self().getTempPath(); // -> mkdir()
        return getConfigPathRel() + PATH_TEMP;
    }

    /**
     * points to the temp path starting from classpath. nothing more than the relative path {@link #PATH_TEMP}.
     * 
     * @return
     */
    public static String getTempPathURL() {
        return PATH_TEMP;
    }

    /**
     * getMainPackage
     * 
     * @return app.main.package (stored in 'app.main.package').
     */
    public static String getApplicationMainPackage() {
        String pck = (String) get("app.main.package");
        if (pck == null) {
            pck = "org.nano" + (getName().startsWith(".") ? "" : ".") + getName().toLowerCase().trim();
            self().info("WARNING: no 'app.main.package' defined in environment! using default: " + pck);
            self().setProperty("app.main.package", pck);
        }
        return pck;
    }

    public static <T> T load(String name, Class<T> type) {
        return load(name, type, true);
    }
    /**
     * loads from xml or yaml file. see property app.configuration.persist.yaml.
     * 
     * @param name relative path + file name (if {@link #getConfigPath()} is not included, it will be inserted.
     * @param type type to de-serialize
     * @return java instance
     */
    public static <T> T load(String name, Class<T> type, boolean renameOnError) {
        File path = getEnvPath(name, type);
        // ManagedException.assertion("exist", path.exists(), path);
        if (self().get("app.configuration.persist.yaml", false)) {
            return self().get(YamlUtil.class).load(path, type);
        } else {
            return self().get(XmlUtil.class).loadXml(path.getAbsolutePath(), type, renameOnError);
        }
    }

    public static File getEnvPath(String name, Class<?> type) {
        name = StringUtil.substring(name, null, getFileExtension());
        String path = cleanpath(name);
        return FileUtil.userDirFile(path + getFileExtension());
    }

    private static String cleanpath(String name) {
        name = StringUtil.substring(name, null, getFileExtension());
        return name.toLowerCase().contains(getName().toLowerCase()) || FileUtil.isAbsolute(name) ? name : getConfigPath() + name;
    }

    /**
     * see {@link #load(String, Class)}
     * 
     * @param name relative path + file name (if {@link #getConfigPath()} is not included, it will be inserted.
     * @param obj object to serialize
     */
    public static void save(String name, Object obj) {
        String path = cleanpath(name);
        if (self().get("app.configuration.persist.yaml", false)) {
            self().get(YamlUtil.class).dump(obj, path + getFileExtension());
        } else {
            self().get(XmlUtil.class).saveXml(path + getFileExtension(), obj);
        }
    }
    
    public static String getFileExtension() {
    	return ENV.get("app.configuration.persist.yaml", false) ? ".yml" : ".xml";
    }

    /**
     * @return Returns the autopersist.
     */
    public static final boolean isAutopersist() {
        return self().autopersist;
    }

    /**
     * @param autopersist The autopersist to set.
     */
    public static final void setAutopersist(boolean autopersist) {
        self().autopersist = autopersist;
    }

    /**
     * persists the given object through configured xml persister.
     * 
     * @param obj object to serialize to xml.
     */
    public static void persist(Object obj) {
        persist(obj.getClass().getSimpleName().toLowerCase(), obj);
    }

    /**
     * persists the given object through configured xml persister.
     * 
     * @param obj object to serialize to xml.
     */
    public static void persist(String name, Object obj) {
        if (self().get("app.configuration.persist.yaml", false)) {
            self().get(YamlUtil.class).dump(obj, getConfigPath() + name + getFileExtension());
        } else {
            self().get(XmlUtil.class).saveXml(getConfigPath() + name + getFileExtension(), obj);
        }
    }

    /**
     * isPersisted
     * 
     * @return true, if an environment was created on file system
     */
    public final static boolean isPersisted() {
        return new File(getConfigPath() + CONFIG_NAME + getFileExtension()).exists();
    }

    /**
     * persists the current environment - all transient properties and services will be lost!!!
     */
    public final static synchronized void persist() {
        //save backup while some key/values will be removed if not serializable
        SortedMap tempProperties = createSyncSortedMap();
        tempProperties.putAll(self().properties);
        Map<Class<?>, Object> tempServices = new Hashtable<Class<?>, Object>(services());
        String configPath = getConfigPath();
        try {
            save(configPath + CONFIG_NAME, self());
        } finally {
            services().putAll(tempServices);
            self().properties.putAll(tempProperties);
        }
    }

    /**
     * reloads the current environment. a reset will be done to reload the environment from saved file.
     */
    public static void reload() {
        String envDir = getConfigPathRel();
        SortedMap tempProperties = createSyncSortedMap();
        tempProperties.putAll(self().properties);
        Map<Class<?>, Object> tempServices = new Hashtable<Class<?>, Object>(services());

        reset();
        create(envDir);

        if (!(Thread.currentThread().getContextClassLoader() instanceof URLClassLoader))
            assignENVClassloaderToCurrentThread(AppLoader.provideClassloader_(envDir));
        //don't overwrite the new values
        MapUtil.removeAll(tempProperties, self().properties.keySet());
        MapUtil.removeAll(tempServices, self().services().keySet());

        //add the not existing old values (programmatically added and not persisted!)
        Messages.reload();
        services().putAll(tempServices);
        self().properties.putAll(tempProperties);
        // don't know why there remain nulls
        MapUtil.removeAllNulls(services());
        MapUtil.removeAllNulls(self().properties);
    }

    /**
     * assignClassloaderToCurrentThread
     */
    public static void assignClassloaderToCurrentThread() {
        ClassLoader cl = (ClassLoader) self().services.get(ClassLoader.class);
        if (cl != null) {
            Thread.currentThread().setContextClassLoader(cl);
        } else {
            addService(ClassLoader.class, Thread.currentThread().getContextClassLoader());
//            get(Log.class).warn("no classloader defined!");
        }
    }

    /**
     * delegates to {@link #assignENVClassloaderToCurrentThread(LibClassLoader)} 
     * using a new instance of {@link NetworkClassLoader}.
     */
    public static void assignENVClassloaderToCurrentThread() {
    	assignENVClassloaderToCurrentThread(new NetworkClassLoader((ClassLoader) self().services.get(ClassLoader.class)));
    }
    
    /**
     * @param cl classloader to add {@link ENV#getConfigPath()} and to be set to current thread
     */
    public static <CL extends LibClassLoader> void assignENVClassloaderToCurrentThread(CL cl) {
    	cl.addLibraryPath(getConfigPath());
    	addService(ClassLoader.class, cl);
    	assignClassloaderToCurrentThread();
    }

    /**
     * loads a resource file through the environments classloader
     * 
     * @param fileName resource
     * @return inputstream
     */
    public static InputStream getResource(String fileName) {
        ClassLoader classLoader = (ClassLoader) self().services.get(ClassLoader.class);
        if (classLoader == null) {
            classLoader = addService(Thread.currentThread().getContextClassLoader());
        }
        return classLoader.getResourceAsStream(fileName);
    }

    /**
     * loads a properties file (only from environment directory!) with a default key sorting.
     * 
     * @param fileName property file to load
     * @return loaded or new properties
     */
    public static Properties getSortedProperties(String fileName) {
        String rc = ENV.getConfigPath() + fileName;
        File rcFile = new File(rc);
        //create a sorted property map
        Properties p = MapUtil.createSortedProperties();
        if (rcFile.canRead()) {
            try {
                p.load(new FileReader(rcFile));
            } catch (Exception e) {
                ManagedException.forward(e);
            }
        }
        return p;
    }

    /**
     * copies the existing file in environment to the temp dir.
     * 
     * @param environmentFile
     */
    public static final void saveBackup(String environmentFile) {
        FileUtil.copy(getConfigPath() + environmentFile,
            getTempPath() + FileUtil.getUniqueFileName(environmentFile));
    }

    public static final void moveBackup(String environmentFile) {
        FileUtil.userDirFile(getConfigPath() + environmentFile)
        	.renameTo(FileUtil.userDirFile(getTempPath() + FileUtil.getUniqueFileName(environmentFile)));
    }

    /**
     * saveResourceToFileSystem
     * 
     * @param resourceName any resource nested in application jar
     * @return true, if resource was saved. if application wasn't started from jar (perhaps in an ide), it returns
     *         always false.
     */
    public static final boolean extractResourceToDir(String resourceName, String destinationDir) {
        return extractResourceToDir(resourceName, destinationDir, false, false, true);
    }

    public static final boolean extractResourceToDir(String resourceName,
            String destinationDir,
            boolean flat,
            boolean executable,
            boolean logError) {
        //put build informations into system-properties
        getBuildInformations();
        //perhaps enrich resource name with version-number from build-infos etc.
        resourceName = System.getProperty(resourceName, resourceName);
        //perhaps get a templates destination name
        String destName = System.getProperty(resourceName + ".destination", resourceName);
        return !AppLoader.isNestingJar() && resourceName.endsWith("ar") 
        		? false : extractResource(resourceName, destinationDir + destName, flat, executable, logError);
    }

    public static final boolean hasResourceOrFile(String resourceName) {
    	String name = System.getProperty(resourceName, resourceName);
    	return get(ClassLoader.class).getResourceAsStream(name) != null || new File(getConfigPath() + name).exists();
    }
    
    public static final boolean extractResource(String resourceName, boolean flat, boolean executable, boolean logError) {
        return extractResourceToDir(resourceName, "", flat, executable, logError);
    }

    public static final boolean extractResource(String resourceName, boolean flat, boolean executable) {
        return extractResourceToDir(resourceName, "", flat, executable, true);
    }
    
    public static final boolean extractResource(String resourceName, boolean executable) {
        return extractResourceToDir(resourceName, "", false, executable, true);
    }

    public static final boolean extractResource(String resourceName) {
        return extractResourceToDir(resourceName, "", false, false, true);
    }

    public static final boolean extractResource(String resourceName,
            String fileName,
            boolean flat,
            boolean executable) {
        return extractResource(resourceName, fileName, flat, executable, true);
    }
    
    /**
     * saves the resource (contained in your jar) to the file-system into the {@link #getConfigPath()} - only if not
     * done yet.
     * 
     * @param resourceName resource name
     * @return true if new file was created
     */
    public static final boolean extractResource(String resourceName,
            String fileName,
            boolean flat,
            boolean executable,
            boolean logError) {
        File destFile = new File(fileName);
        File file =
            destFile.isAbsolute() ? destFile : new File(getConfigPath() + (flat ? destFile.getName() : fileName)).getAbsoluteFile();
        if (!file.exists()) {
            logger(ENV.class).debug("extracting resource " + resourceName);
            if (file.getParentFile() != null)
                file.getParentFile().mkdirs();
            InputStream res = get(ClassLoader.class).getResourceAsStream(resourceName);
            try {
                if (res == null /*|| res.available() <= 0*/) {
                	if (resourceName.endsWith("ar"))
                		return false; // jar files are available only in nested mode
                    throw new IllegalStateException("the resource '" + resourceName
                        + "' of our main-jar-file is not available or empty!");
                }
                FileUtil.write(res, new FileOutputStream(file), fileName, true);
                if (executable)
                    file.setExecutable(true);
                return true;
            } catch (Exception e) {
                ManagedException.forward(e, logError);
            }
        }
        return false;
    }

    /**
     * searches for the given jar-file names in current classloader and loads them perhaps through an
     * internet-repository (like with maven).
     * 
     * @param dependencyNames jar-file names to check.
     * @return any loader information
     */
    public static final Object loadJarDependencies(String... dependencyNames) {
        //evaluate already loaded jars (no class-cast possible --> using reflection!)
        String[] nestedJars =
            (String[]) BeanClass.call(Thread.currentThread().getContextClassLoader(), "getNestedJars");
        File[] environmentJars = FileUtil.getFiles(getConfigPath(), ".*[.]jar");
        Collection<String> availableJars =
            new ArrayList<String>((nestedJars != null ? nestedJars.length : 0) + environmentJars.length);
        if (nestedJars != null) {
            availableJars.addAll(Arrays.asList(nestedJars));
        }
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

    public static final String getPackagePrefix(String mvnNamePart) {
    	return get(CompatibilityLayer.class).isAvailable(TSL2_JARRESOLVER)
        	? (String) get(CompatibilityLayer.class).run(TSL2_JARRESOLVER, "getPackage", new Class[] { String.class }, mvnNamePart)
        	: null;
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
            askForOnline(unresolvedDependencies);
            return loadDependencies(unresolvedDependencies.toArray(new String[0]));
        }
        return "nothing to do!";
    }

    public static boolean askForOnline(List<String> unresolvedDependencies) {
        boolean shouldDownload = Message.ask("should the framework download the unresolved classes:<p/>" + StringUtil.toFormattedString(unresolvedDependencies, -1, true, "<br/>"), true);
        System.setProperty("tsl2nano.offline", Boolean.valueOf(!shouldDownload).toString());
        return shouldDownload;
    }

    /**
     * tries to interpret the given dependency names and to load them perhaps through an internet-repository (like with
     * maven).
     * 
     * @param dependencyNames names including the organisation/product name to be extracted.
     * @return any loader information, or null, if nothing was done.
     */
    //TODO: check moving to NetworkClassloader
    public static final Object loadDependencies(String... dependencyNames) {
        //check for own framework dependencies
        boolean foreignDependencies = false;
        for (int i = 0; i < dependencyNames.length; i++) {
            if (!dependencyNames[i].startsWith(FRAMEWORK)) {
                foreignDependencies = true;
                break;
            }
        }
        if (!foreignDependencies) {
            return null;
        }
        //load the dependencies through maven, if available
        ENV.getAsking("tsl2nano.offline", true);
        ENV.getAsking("app.translate.bundle.project", false);
        if (get("classloader.usenetwork.loader", true) && NetUtil.isOnline()
            && get(CompatibilityLayer.class).isAvailable(TSL2_JARRESOLVER)) {
            Message.send("resolving dependencies: "
                    + StringUtil.toString(dependencyNames, 300));
            
            if (!get("classloader.usenetwork.loader.asked", false) 
                && !Message.ask("load dependencies from maven:<p/>" + StringUtil.toString(dependencyNames, 300) + "? ", true)) {
                setProperty("classloader.usenetwork.loader", false);
                setProperty("classloader.usenetwork.loader.asked", true);
                return "user denied - no dependencies loaded";
            }

            Object result = get(CompatibilityLayer.class).run(TSL2_JARRESOLVER, "install", new Class[] { String[].class },
                new Object[] { dependencyNames });
            if (result == null || result.toString().startsWith("FAILED"))
                throw new IllegalStateException("couldn't resolve dependencies:\n"
                    + StringUtil.toFormattedString(dependencyNames, 100, true));
        } else {
            throw new IllegalStateException("couldn't resolve dependencies:\n"
                + StringUtil.toFormattedString(dependencyNames, 100, true));
        }
        return "dependency loading successfull";
    }

    public static boolean isTestMode() {
        return Boolean.getBoolean(KEY_TESTMODE);
    }

    public static boolean isDebugEnabled(Class<?> cls) {
        return LogFactory.getLog(cls).isDebugEnabled();
    }

    @Commit
    protected void initDeserialization() {
    	if (services != null && !(services instanceof SortedMap)) {
    		SortedMap<Class<?>,Object> serviceMap = createServiceMap();
    		serviceMap.putAll(services);
    		services = serviceMap;
    	}
    }
    
    @Persist
    protected void initSerialization() {
        if (properties == null)
            properties = createSyncSortedMap();
        /*
         * remove the environment path itself - should not be reloaded
         */
        properties.remove(KEY_CONFIG_PATH);

        /*
         * remove all not-serializable objects
         */

        Set<Object> keySet = properties.keySet();
        List<Object> unserializableProperties = new LinkedList<>();
        for (Iterator<?> keyIt = keySet.iterator(); keyIt.hasNext();) {
            Object key = keyIt.next();
            Object value = properties.get(key);
            if (value != null && (isNotSerializable(value)
                || !ObjectUtil.isSingleValueType(value.getClass())) || value instanceof ClassLoader) {
                keyIt.remove();
                unserializableProperties.add(key);
            }
        }
        if (unserializableProperties.size() > 0) {
            info("removing properties from serialization while its value is not serializable or doesn't have a default constructor:\n\t" + unserializableProperties);
        }
        if (services == null)
            services = createServiceMap();
        Set<Class<?>> serviceKeys = services.keySet();
        List<Object> unserializableServices = new LinkedList<>();
        for (Iterator<?> keyIt = serviceKeys.iterator(); keyIt.hasNext();) {
            Object key = keyIt.next();
            Object value = services.get(key);
            if (isNotSerializable(value)) {
                keyIt.remove();
                unserializableServices.add(key);
            }
        }
        if (unserializableServices.size() > 0) {
            info("removing services from serialization while its value is not serializable or doesn't have a default constructor:\n\t" + unserializableServices);
        }
    }

    /**
     * createServiceMap
     * 
     * @return
     */
    static SortedMap<Class<?>, Object> createServiceMap() {
        return Collections.synchronizedSortedMap(new TreeMap<>((o1, o2) -> o1.getName().compareTo(o2.getName())));
    }

    /**
     * Serializable must be implemented and a default constructor must be present. simple-xml needs at least one non
     * transient field!
     * 
     * @param value
     * @return
     */
    private boolean isNotSerializable(Object value) {
        return value != null
            && (!Serializable.class.isAssignableFrom(value.getClass()) || (!ObjectUtil.isStandardType(value) && !BeanClass
                .hasDefaultConstructor(value.getClass())) || !isProxyHandlerSerializable(value));
    }

    /** 
     * The Proxy itself is always serliazable while the invocationhandler maybe not! 
     * @return true if it is not a proxy or the proxies handler is serializable
     */
    private boolean isProxyHandlerSerializable(Object value) {
        return !Proxy.isProxyClass(value.getClass()) || Serializable.class.isAssignableFrom(Proxy.getInvocationHandler(value).getClass());
    }

    /**
     * logger
     * 
     * @param caller
     * @return
     */
    protected static Log logger(Object caller) {
        Log log = self().loggers.get(caller.getClass());
        if (log == null) {
            log = LogFactory.getLog(caller.getClass());
        }
        return log;
    }

    /**
     * convenience to call a standard logger without creating a member.
     * 
     * @param caller the caller instance
     * @param item item to be logged
     */
    public static void error(Object caller, Object item) {
        logger(caller).error(item);
    }

    /**
     * convenience to call a standard logger without creating a member.
     * 
     * @param caller the caller instance
     * @param item item to be logged
     */
    public static void warn(Object caller, Object item) {
        logger(caller instanceof Class ? caller : caller.getClass()).warn(item);
    }

    /**
     * convenience to call a standard logger without creating a member.
     * 
     * @param caller the caller instance
     * @param item item to be logged
     */
    public static void info(Object caller, Object item) {
        logger(caller.getClass()).info(item);
    }

    protected static void info(Object item) {
        logger(ENV.class).info(item);
    }

    /**
     * convenience to call a standard logger without creating a member.
     * 
     * @param caller the caller instance
     * @param item item to be logged
     */
    public static void debug(Object caller, Object item) {
        logger(caller.getClass()).debug(item);
    }

    /**
     * convenience to call a standard logger without creating a member.
     * 
     * @param caller the caller instance
     * @param item item to be logged
     */
    public static void trace(Object caller, Object item) {
        logger(caller.getClass()).trace(item);
    }

    public static boolean isModeStrict() {
        return get("app.mode.strict", false);
    }

    public static boolean isModeOffline() {
        return Boolean.getBoolean("tsl2nano.offline");
    }
}
