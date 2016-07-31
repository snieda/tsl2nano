/*
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.serviceaccess;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.service.feature.Feature;
import de.tsl2.nano.service.feature.FeatureFactory;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.batch.CachingBatchloader;
import de.tsl2.nano.serviceaccess.aas.principal.Role;
import de.tsl2.nano.serviceaccess.aas.principal.UserPrincipal;

/**
 * the servicefactory will provide the current user and mandator, the server side session beans (packed into proxies)
 * and authorization/feature evaluation mechanisms. it is usable on both sides - the client-side to get services - the
 * server-side to get some properties. never call {@link #getService(Class)} on server-side to get a service - this
 * should be done by dependency injection of ejb 3 (using @EJB).
 * <p>
 * please call {@link #createInstance(ClassLoader)} before any call to the singelton provider {@link #instance()}. it is
 * recommended to use the convenience method {@link #createSession(Object, Object, Subject, Collection, Collection)} to
 * initialize all objects to be used as user session, too.
 * <p>
 * to provide additional properties, use system properties. standard jndi-properties will be found in jndi.properties in
 * classpath. e.g. the 'jndi.prefix' (default: java:module) could be set - or a special name service of jboss etc.<br/>
 * For further informations, see {@link ServiceLocator}.
 * <p/>
 * it is possible to store server-side application specific content into the properties of the service factory.
 * <p/>
 * basic properties will be load from file 'serviceaccess.properties'.
 * 
 * @author TS
 */
public class ServiceFactory {
    protected static final Log LOG = LogFactory.getLog(ServiceFactory.class);
    /** project and serviceaccess properties */
    private Properties properties;
    private String jndiPrefix;

    private IAuthorization auth;
    
    Map<? extends Object, Object> userProperties = Collections.synchronizedMap(new HashMap<String, Object>());

    ServiceLocator serviceLocator = null;

    /**
     * holds all already loaded services - but wrapped into {@link ServiceProxy}. the {@link #serviceLocator} holds the
     * real service instances.
     */
    Map<Class<?>, Object> services = new Hashtable<Class<?>, Object>();

    private static ServiceFactory self = null;

    private ClassLoader classLoader = null;

    /** key name for the jndi-prefix */
    public static final String KEY_JNDI_PREFIX = "serviceaccess.jndi.prefix";
    /** flag to avoid using of jndi - e.g. for testing purposes */
    public static final String NO_JNDI = ServiceLocator.NO_JNDI;
    /** user properties key name for the user object */
    public static final String KEY_USER_OBJECT = "user.object";
    /** user properties key name for the mandator object */
    public static final String KEY_MANDATOR_OBJECT = "mandator.object";
    /** flag to indicate whether a jndi mapping name will be created ({@link #KEY_JNDI_PREFIX} and used. */
    public static final String KEY_USE_JNDI_MAPPING = "serviceaccess.use.jndimapping";

    /**
     * private instance creation
     * 
     * @param classloader project classloader
     * @param jndiFileName (optional) jndi file name
     */
    private ServiceFactory(ClassLoader classloader) {
        super();
        try {
            this.classLoader = classloader;
            properties = System.getProperties();
            //loading server properties (from class-path)
            try {
                properties.load(FileUtil.getResource("serviceaccess.properties", classloader));
            } catch (final Exception e) {
                LOG.info("couldn't load optional properties from serviceaccess.properties in classpath");
            }
            // set the default jndi prefix
            if (Boolean.valueOf(properties.getProperty(KEY_USE_JNDI_MAPPING, "false")))
                jndiPrefix = properties.getProperty(KEY_JNDI_PREFIX, ENV.getName().toLowerCase().trim());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@link #createInstance(ClassLoader)} must be called once before!
     * 
     * @return singelton instance
     */
    public static final ServiceFactory instance() {
        assert self != null : "please call createInstance(..) before!";
        ServiceFactory.checkConnection();
        return self;
    }

    /**
     * @return the serviceLocator
     */
    protected ServiceLocator getServiceLocator() {
        if (serviceLocator == null) {
            serviceLocator = new ServiceLocator(classLoader, properties);
        }
        return serviceLocator;
    }

    /**
     * for client side use to get a server side service. never call it on server side! see
     * {@link ServiceLocator#lookup(String, Class)}
     * 
     * @param <T> type of service
     * @param serviceClass service interface
     * @return service instance
     */
    public <T> T getService(Class<T> serviceClass) {
        return getService(jndiPrefix, serviceClass);
    }

    /**
     * for client side use to get a server side service. never call it on server side! see
     * {@link ServiceLocator#lookup(String, Class)}
     * 
     * @param <T> type of service
     * @param jndiPrefix
     * @param serviceClass service interface
     * @return service instance
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(String jndiPrefix, Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            final T delegate = getServiceLocator().lookup(jndiPrefix, serviceClass);
            service = ServiceProxy.createBeanImplementation(serviceClass, delegate, classLoader);
            services.put(serviceClass, service);
        }
        return service;
    }

    /**
     * @return the subject
     */
    public Subject getSubject() {
        return auth != null ? auth.getSubject() : null;
    }

    /**
     * @param subject the subject to set
     */
    public void setSubject(Subject subject) {
        auth = new Authorization(subject);

        /*
         * for server side checks (e.g. sessioncontext.getCallerPrincipal()), we have to add 
         * the SECURITY_PRINCIPAL to the initial context additionally, the server has to know, 
         * which security domain to use - in jboss we do that in login-conf.xml. the application should
         * define a jboss.xml inside it's META-INF - perhaps using the 'client-login'.
         */
        try {
            final Set<UserPrincipal> userPrincipals = subject != null ? subject.getPrincipals(UserPrincipal.class)
                : null;
            final UserPrincipal userPrincipal = userPrincipals != null && userPrincipals.size() > 0 ? userPrincipals.iterator()
                .next()
                : null;
            if (userPrincipal == null) {
                LOG.warn("No user principal defined --> services are not able to retrieve sessioncontext.getCallerPrincipal");
                getServiceLocator().getInitialContext().removeFromEnvironment(Context.SECURITY_PRINCIPAL);
            } else {
                getServiceLocator().getInitialContext().addToEnvironment(Context.SECURITY_PRINCIPAL, userPrincipal);
            }
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * hasRole
     * 
     * @param roleName
     * @return true, if user has role
     */
    public boolean hasRole(String roleName) {
        return hasPrincipal(new Role(roleName));
    }

    /**
     * hasPrincipal
     * 
     * @param principal {@link Principal}
     * @return true, if subject contains this principal
     */
    public boolean hasPrincipal(Principal principal) {
        return auth != null ? auth.hasPrincipal(principal) : false;
    }

    /**
     * @return user depended properties, defined by {@link #setUserProperties(Map)}. but be careful: only one
     *         servicefactory instance will be on a jvm - if more than one may be connected, you can't use this feature.
     *         it is only useful on system-processes using exactly one system-user (like a batch-process does). if you
     *         have to access user properties in a secure context, use the subject (accessible through the ejb session
     *         context)
     */
    public Map<? extends Object, Object> getUserProperties() {
        return userProperties;
    }

    /**
     * @return system properties. this are properties, loaded from server.properties. it is possible, to add application
     *         specific content.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * convenience to add some properties - e.g. application specific configurations.
     * 
     * @param additionalProperties
     */
    public void setProperties(Properties additionalProperties) {
        properties.putAll(additionalProperties);
    }

    /**
     * @param userProperties the userProperties to set (see {@link #getUserProperties()}).
     */
    public void setUserProperties(Map<? extends Object, Object> userProperties) {
        this.userProperties = userProperties;
    }

    /**
     * getUserObject
     * 
     * @return user object, if defined!
     */
    public Object getUserObject() {
        return userProperties.get(KEY_USER_OBJECT);
    }

    /**
     * getMandatorObject
     * 
     * @return mandator object, if defined!
     */
    public Object getMandatorObject() {
        return userProperties.get(KEY_MANDATOR_OBJECT);
    }

    /**
     * @param initialServices fill some base services (e.g. for testing)
     */
    public void setInitialServices(Map<String, Object> initialServices) {
        getServiceLocator().setInitialServices(initialServices);
    }

    /**
     * must be called before using the singelton {@link #instance()}.
     * 
     * @param classLoader the classLoader to set
     */
    public static void create(ClassLoader classLoader) {
        createInstance(classLoader);
        instance().createSession(null, null, null, new LinkedList<String>(), new LinkedList<String>(), null);
    }

    /**
     * must be called before using the singelton {@link #instance()}.
     * 
     * @param classLoader the classLoader to set
     */
    public static ServiceFactory createInstance(ClassLoader classLoader) {
        self = new ServiceFactory(classLoader);
        LOG.info("ServiceFactory singelton instance assigned: " + self);
        return self;
    }

    /**
     * sets all user and module specific properties.<br>
     * the servicefactory will provide the current user and mandator, the server side session beans (packed into
     * proxies) and authorization/feature evaluation mechanisms.
     * 
     * @param userObject user
     * @param mandatorObject mandator
     * @param subject subject (optional) if null, a new Subject will be created
     * @param userRoles roles (optional) Principals of type Role will be set
     * @param features (optional) Principals of type Feature will be set
     * @param (optional) featureInterfacePrefix package postfix (default: [empty])
     */
    public void createSession(Object userObject,
            Object mandatorObject,
            Subject subject,
            Collection<String> userRoles,
            Collection<String> features,
            String featureInterfacePrefix) {
        if (LOG.isDebugEnabled()) {
            String info = "\n===========================================================\n"
                + "application server properties:\n"
                + "    args  : ${sun.java.command}\n"
                + "    dir   : ${user.dir}\n"
                + "    time  : ${tstamp}\n"
                + "    user  : ${user.name}, home: ${user.home}\n"
                + "    lang  : ${user.country}_${user.language}, encoding: ${sun.jnu.encoding}\n"
                + "    encode: ${file.encoding}\n"
                + "    java  : ${java.runtime.version}, ${java.home}\n"
                + "    os    : ${os.name}, ${os.version} ${sun.os.patch.level} ${os.arch}\n"
                + "    system: ${sun.cpu.isalist} ${sun.arch.data.model}\n"
                + "===========================================================";
            Properties p = ServiceFactory.getGenService().getServerInfo();
            p.put("tstamp", new Date());
            LOG.debug(StringUtil.insertProperties(info, p));
        }

        LOG.info("initializing service-factory session (user:" + userObject
            + ", mandator:"
            + mandatorObject
            + ", subject:"
            + subject
            + ", userRoles:"
            + (userRoles != null ? userRoles.size() : "--")
            + ", features:"
            + (features != null ? features.size() : "--"));
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put(ServiceFactory.KEY_USER_OBJECT, userObject);
        m.put(ServiceFactory.KEY_MANDATOR_OBJECT, mandatorObject);
        setUserProperties(m);

        //set the subject
        if (subject == null) {
            subject = new Subject();
        }
        setSubject(subject);
        //add all user roles as principals
        if (userRoles != null) {
            for (final String roleName : userRoles) {
                subject.getPrincipals().add(new Role(roleName));
            }
        }
        //add all application modules as principals
        if (features != null) {
            for (final String moduleName : features) {
                subject.getPrincipals().add(new Feature(moduleName));
            }
        }
        //instantiate a feature factory
        FeatureFactory.createInstance(null, classLoader, featureInterfacePrefix, null, null, null);

    }

    /**
     * isInitialized
     * 
     * @return true, if createInstance(..) was called.
     */
    public static boolean isInitialized() {
        return self != null;
    }

    /**
     * if servicefactory is not initialized, an exception will be thrown
     */
    public static void checkConnection() {
        if (!ServiceFactory.isInitialized()) {
            LOG.error("Server-Connection Lost! May be caused by Server-Restart or Instruction-Error (no previously call of ServiceFactory.createInstance(..))");
            throw new ManagedException("tsl2nano.login.noconnection");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + ": properties: "
            + properties
            + "\nuserproperties: "
            + userProperties
            + "\ncached services: "
            + this.services;
    }

    /**
     * convenience to get the remote generic service
     * 
     * @return generic service
     */
    public static final IGenericService getGenService() {
        return instance().getService(IGenericService.class);
    }

    /**
     * does an automated jaas-login without GUI. be sure to have set the property:<br>
     * "java.security.auth.login.config".<br>
     * e.g.: System.setProperty("java.security.auth.login.config", "../myproject/config/jaas-login.config");
     * <p>
     * 
     * the specific LoginModule must be found through the classloader!
     * 
     * @param moduleName name of AAS LoginModule (e.g.: LoginJaas)
     * @param classloader for ServiceFactory to get service classes
     * @param user user to login
     * @param passwd users password
     * @return true, if successful, otherwise false
     */
    public static void login(String moduleName, ClassLoader classLoader, final String user, final String passwd) {
        final CallbackHandler callbackHandler = new CallbackHandler() {
            @Override
            public void handle(Callback[] arg) throws IOException, UnsupportedCallbackException {
                ((NameCallback) arg[0]).setName(user);
                ((PasswordCallback) arg[1]).setPassword(passwd.toCharArray());
            }
        };
        try {
            if (!ServiceFactory.isInitialized()) {
                ServiceFactory.createInstance(classLoader);
            }
            final LoginContext lc = new LoginContext(moduleName, callbackHandler);
            lc.login();
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * does an automated jaas-logout without GUI. be sure to have set the property:<br>
     * "java.security.auth.login.config".<br>
     * e.g.: System.setProperty("java.security.auth.login.config", "../myproject/config/jaas-login.config");
     * <p>
     * 
     * the specific LoginModule must be found through the classloader!
     * 
     * @param moduleName name of AAS LoginModule (e.g.: LoginJaas)
     * @param classloader for ServiceFactory to get service classes
     * @param user user to login
     * @return true, if successful, otherwise false
     */
    public static boolean logout(String moduleName, ClassLoader classLoader, final String user) {
        if (!ServiceFactory.isInitialized()) {
            return false;
        }
        final CallbackHandler callbackHandler = new CallbackHandler() {
            @Override
            public void handle(Callback[] arg) throws IOException, UnsupportedCallbackException {
                ((NameCallback) arg[0]).setName(user);
            }
        };
        try {
            final LoginContext lc = new LoginContext(moduleName, callbackHandler);
            lc.logout();
            ServiceFactory.instance().logout();
            return true;
        } catch (final Exception e) {
            ManagedException.forward(e);
            return false;
        }
    }

    /**
     * logout, removes all account informations stored in this servicefactory.
     */
    public void logout() {
        setSubject(null);
        userProperties.clear();
    }

    public CachingBatchloader getCache() {
        return CachingBatchloader.instance();
    }

    /**
     * resets all properties of this servicefactory.
     * 
     * @param complete if true, all caches and instances will be reseted
     */
    public void reset(boolean complete) {
        logout();
        properties.clear();
        if (complete) {
            userProperties = null;
            auth = null;
            services.clear();
            classLoader = null;
            serviceLocator = null;
            CachingBatchloader.instance().reset();
            self = null;
        }
    }
}
