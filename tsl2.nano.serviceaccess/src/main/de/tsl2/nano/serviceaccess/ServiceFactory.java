/*
 * Copyright � 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 M�nchen. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielf�ltigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.serviceaccess;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
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
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.Messages;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.service.feature.Feature;
import de.tsl2.nano.service.feature.FeatureFactory;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.batch.CachingBatchloader;
import de.tsl2.nano.serviceaccess.aas.principal.Role;
import de.tsl2.nano.serviceaccess.aas.principal.UserPrincipal;
import de.tsl2.nano.util.FileUtil;

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
 * the property file <code>project.properties</code> must be present on base path - to load 'jndi.file.name'. an
 * optional property file 'serviceacces.properties' will be loaded if available - to be used by services itself.
 * <p/>
 * it is possible to store server-side application specific content into the properties of the service factory.
 * <p/>
 * basic properties will be load from file 'serviceaccess.properties'.
 * 
 * @author TS
 */
public class ServiceFactory {
    //TODO: extend common-Service
    protected static final Log LOG = LogFactory.getLog(ServiceFactory.class);
    /** project and serviceaccess properties */
    private Properties properties;
    private String jndi_prefix;

    Subject subject;
    Map<? extends Object, Object> userProperties = new HashMap<String, Object>();

    ServiceLocator serviceLocator = null;

    /**
     * holds all already loaded services - but wrapped into {@link ServiceProxy}. the {@link #serviceLocator} holds the
     * real service instances.
     */
    Map<Class<?>, Object> services = new Hashtable<Class<?>, Object>();

    private static ServiceFactory self = null;

    private ClassLoader classLoader = null;

    /** key name for the jndi file name - inside the project.properties */
    private static final String KEY_JNDI_FILE = "jndi.file.name";
    /** flag to avoid using of jndi - e.g. for testing purposes */
    public static final String NO_JNDI = ServiceLocator.NO_JNDI;
    /** user properties key name for the user object */
    public static final String KEY_USER_OBJECT = "user.object";
    /** user properties key name for the mandator object */
    public static final String KEY_MANDATOR_OBJECT = "mandator.object";

    /**
     * private instance creation
     * 
     * @param classloader project classloader
     * @param jndiFileName (optional) jndi file name
     */
    private ServiceFactory(ClassLoader classloader, String jndiFileName) {
        super();
        try {
            this.classLoader = classloader;
            //loading client properties
            if (jndiFileName != null) {
                properties = new Properties();
                properties.put(KEY_JNDI_FILE, jndiFileName);
            } else {
                properties = FileUtil.loadProperties("project.properties", classLoader);
            }
            //loading server properties (directly from appserver start path)
            //IMPROVE: load it through a file connector
            final File file = new File("serviceaccess.properties");
            try {
                properties.load(new FileReader(file));
            } catch (final Exception e) {
                LOG.info("couldn't load optional properties from " + file.getAbsolutePath());
            }
            // set the default jndi prefix
            jndi_prefix = properties.getProperty("project.name");
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
            String jndiFile = properties.getProperty(KEY_JNDI_FILE);
            if (jndiFile == null)
                throw new FormattedException("ServiceLocator couldn't find a value for key '" + KEY_JNDI_FILE
                    + "' inside the application properties");
            serviceLocator = new ServiceLocator(classLoader, jndiFile);
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
        return getService(jndi_prefix, serviceClass);
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
        return subject;
    }

    /**
     * @param subject the subject to set
     */
    public void setSubject(Subject subject) {
        this.subject = subject;

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
            ForwardedException.forward(e);
        }
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
     * hasRole
     * 
     * @param roleName
     * @return true, if user has role
     */
    public boolean hasRole(String roleName) {
        return hasPrincipal(new Role(roleName));
    }

    /**
     * checkPrincipal, throws SecurityException if not
     * 
     * @param principal principal
     */
    public void checkPrincipal(Principal principal) {
        if (!hasPrincipal(principal)) {
            final String msg = Messages.getFormattedString(Messages.getString("swartifex.login.noprincipal"),
                new Object[] { getUserObject(), principal.getName() });
            throw new SecurityException(msg);
        }
    }

    /**
     * hasPrincipal
     * 
     * @param principal {@link Principal}
     * @return true, if subject contains this principal
     */
    public boolean hasPrincipal(Principal principal) {
        if (getSubject() == null) {
            LOG.warn("ServiceFactory.hasPrincipal: no subject defined!");
            return false;
        }
        final Set<? extends Principal> subjectPrincipals = getSubject().getPrincipals(principal.getClass());

//        /*
//         * if no principal was defined, the principal-permissions will be
//         * switched off.
//         */
//        if (subjectPrincipals.isEmpty()) {
//            LOG.warn("ServiceFactory.hasPrincipal: no principal of type " + principal.getClass().getSimpleName()
//                + " was defined ==> permissions-system will be switched off for this type!");
//            return true;
//        }

        final boolean hasPrincipal = subjectPrincipals.contains(principal);
        if (!hasPrincipal) {
            LOG.warn(principal.getClass().getSimpleName() + " was not set for: " + principal.getName());
        }
        return hasPrincipal;
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
    public static void createInstance(ClassLoader classLoader) {
        createInstance(classLoader, null);
    }

    /**
     * must be called before using the singelton {@link #instance()}.
     * 
     * @param classLoader the classLoader to set
     * @param jndiFileName (optional) if null, the name will be loaded through property file 'project.properties'. if
     *            {@link #NO_JNDI}, no jndi-context will be used - you should provide services through
     *            {@link #setInitialServices(Map)}.
     */
    public static void createInstance(ClassLoader classLoader, String jndiFileName) {
        self = new ServiceFactory(classLoader, jndiFileName);
        LOG.info("ServiceFactory singelton instance assigned: " + self);
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
            throw new FormattedException("swartifex.login.noconnection");
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
     * e.g.: System.setProperty("java.security.auth.login.config", "../kion/config/jaas-login.config");
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
            ForwardedException.forward(e);
        }
    }

    /**
     * does an automated jaas-logout without GUI. be sure to have set the property:<br>
     * "java.security.auth.login.config".<br>
     * e.g.: System.setProperty("java.security.auth.login.config", "../kion/config/jaas-login.config");
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
            ForwardedException.forward(e);
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
            subject = null;
            services.clear();
            classLoader = null;
            CachingBatchloader.instance().reset();
            self = null;
        }
    }
}