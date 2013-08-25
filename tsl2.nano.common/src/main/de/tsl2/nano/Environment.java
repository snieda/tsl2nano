/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Dec 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.text.Format;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.execution.CompatibilityLayer;
import de.tsl2.nano.execution.Profiler;
import de.tsl2.nano.execution.XmlUtil;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanClass;
import de.tsl2.nano.util.bean.BeanUtil;

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

    public static final String PREFIX = Environment.class.getPackage().getName() + ".";

    public static final String KEY_SYS_BASEDIR = PREFIX + ".basedir";
    public static final String KEY_DEFAULT_FORMAT = PREFIX + "defaultformat";
    public static final String KEY_CONFIG_PATH = PREFIX + "config.path";

    public static final String CONFIG_XML_NAME = "environment.xml";

    private Environment() {
        self = this;
    }

    /**
     * getName
     * @return
     */
    public static Object getName() {
        return StringUtil.toFirstUpper(StringUtil.substring(self.getConfigPath(), File.separator, null, true)
            .replace('/', ' '));
    }

    /**
     * provides services through their interface class.
     * 
     * @param <T> service type
     * @param service interface of service
     * @return implementation of service
     */
    public static <T> T get(Class<T> service) {
        Object s = services().get(service);
        if (s == null && BeanClass.hasDefaultConstructor(service)) {
            self().addService(BeanClass.createInstance(service));
        }
        return (T) services().get(service);
    }

    protected final static Environment self() {
        if (self == null) {
            create(System.getProperty(KEY_CONFIG_PATH, System.getProperty("user.dir")));
        }
        return self;
    }

    public static void create(String dir) {
        new File(dir).mkdirs();
        File configFile = new File(dir + "/" + CONFIG_XML_NAME);//new File(System.getProperty(KEY_CONFIG_PATH, System.getProperty("user.dir")));
        if (configFile.canRead()) {
            self = XmlUtil.loadXml(configFile.getPath(), Environment.class, new CompatibilityLayer(), false);
            String configPath = getConfigPath();
            if (!configPath.endsWith("/"))
                setProperty(KEY_CONFIG_PATH, configPath + "/");
        } else {
            self = new Environment();
            self.properties = new Properties();
//          LOG.warn("no environment.properties available");
            self.properties.put(KEY_CONFIG_PATH, dir + "/");
        }
        self.services = new Hashtable<Class<?>, Object>();
        registerBundle(PREFIX + "messages", true);
        addService(Profiler.class, Profiler.si());
        self.persist();
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
        return service;
    }

    /**
     * resets the current environment to be empty
     */
    public static void reset() {
        self = null;
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
        Messages.registerBundle(bundle, head);
    }

    /**
     * Get the translation for the given key from the ResourceBundle pool (see
     * {@link #registerBundle(ResourceBundle, boolean)}. optional translation. tries to get the translation from
     * resoucebundle pool. if not found, the naming part of the key will be returned.
     * 
     * @param key the bundle key
     * @param optional if true, a first-upper pure string will be returned, if not found inside any bundle
     * @return bundle value the translated value or the key itself if no translation is available
     */
    public static String translate(Object key, boolean optional) {
        if (key instanceof Enum)
            return Messages.getString((Enum<?>) key);
        else {
            if (optional)
                return Messages.getStringOpt((String) key, true);
            else
                return Messages.getString((String) key);
        }
    }

    /**
     * formats the given object
     * 
     * @param obj object to format
     * @return formatted object
     */
    public static String format(Object obj) {
        Object formatter = get(KEY_DEFAULT_FORMAT);
        if (!(formatter instanceof Format)) {
            if (formatter == null) {
                formatter = new DefaultFormat();
            } else if (formatter instanceof String) {
                formatter = BeanClass.createBeanClass((String) formatter).createInstance();
            } else {
                throw new IllegalArgumentException("environments default formatter must be an instance of java.text.Format but is: " + formatter.getClass());
            }
            self().setProperty(KEY_DEFAULT_FORMAT, formatter);
        }
        return ((Format) formatter).format(obj);
    }

    public static String getConfigPath(Class<?> type) {
        return self().properties.getProperty(KEY_CONFIG_PATH) + type.getSimpleName().toLowerCase();
    }

    public static String getConfigPath() {
        return self().properties.getProperty(KEY_CONFIG_PATH);
    }

    /**
     * persists (saves) the current environment
     */
    public static void persist() {
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

        self().get(XmlUtil.class).saveXml(getConfigPath() + CONFIG_XML_NAME, self());
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

    @Persist
    protected void initSerialization() {
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
}
