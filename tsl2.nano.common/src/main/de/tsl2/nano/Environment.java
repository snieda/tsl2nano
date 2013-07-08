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

import java.io.Serializable;
import java.text.Format;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.execution.XmlUtil;
import de.tsl2.nano.format.DefaultFormat;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.bean.BeanClass;
import de.tsl2.nano.util.bean.BeanUtil;
import de.tsl2.nano.util.bean.def.AttributeDefinition;

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
@Default(value=DefaultType.FIELD, required=false)
public class Environment {
//    Log LOG = LogFactory.getLog(Environment.class);

    private static Environment self;
    @ElementMap(entry = "property", key = "name", attribute = true, inline = true, required = false, keyType=String.class, valueType=Object.class)
    private Properties properties;

    /**
     * holds all already loaded services - but wrapped into {@link ServiceProxy}. the {@link #serviceLocator} holds the
     * real service instances.
     */
    Map<Class<?>, Object> services;

    public static final String PREFIX = Environment.class.getPackage().getName() + ".";

    public static final String KEY_DEFAULT_FORMAT = PREFIX + "defaultformat";
    public static final String KEY_CONFIG_PATH = PREFIX + "config.path";

    public static final String CONFIG_XML_NAME = "environment.xml";
    public static final String CONFIG_FILE_NAME = "environment.properties";
    public static final String SERVICE_FILE_NAME = "environment.services";

    private Environment() {
        try {
            properties = FileUtil.loadProperties(CONFIG_FILE_NAME, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            properties = new Properties();
//            LOG.warn("no environment.properties available");
            properties.put(KEY_CONFIG_PATH, System.getProperty("user.dir") + "/");
        }
        services = new Hashtable<Class<?>, Object>();
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
        if (self == null)
            self = new Environment();
        return self;
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
    public static <T> void addService(T service) {
        addService((Class<T>) (service.getClass().getInterfaces().length > 0 ? service.getClass().getInterfaces()[0]
            : service.getClass()), service);
    }

    /**
     * manually add a service to the environment
     * 
     * @param service service to add (should implement at least one interface.
     */
    public static <T> void addService(Class<T> interfaze, T service) {
        services().put(interfaze, service);
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
     * @param bundle new bundle
     * @param head whether to add the bundle on top or bottom.
     */
    public static void registerBundle(ResourceBundle bundle, boolean head) {
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
                return Messages.getStringOpt((String) key);
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
        
        self().get(XmlUtil.class).saveXml(CONFIG_XML_NAME, self());
    }

    /**
     * assignClassloaderToCurrentThread
     */
    public static void assignClassloaderToCurrentThread() {
        ClassLoader cl = get(ClassLoader.class);
        if (cl != null)
            Thread.currentThread().setContextClassLoader(cl);
//        else
//            get(Log.class).warn("no classloader defined!");
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
            if (value != null && !Serializable.class.isAssignableFrom(value.getClass()) || !BeanUtil.isSingleValueType(value.getClass()))
                keyIt.remove();
        }
        Set<Class<?>> serviceKeys = services.keySet();
        for (Iterator<?> keyIt = serviceKeys.iterator(); keyIt.hasNext();) {
            Object key = (Object) keyIt.next();
            Object value = services.get(key);
            if (value != null && !Serializable.class.isAssignableFrom(value.getClass()))
                keyIt.remove();
        }
    }
}
