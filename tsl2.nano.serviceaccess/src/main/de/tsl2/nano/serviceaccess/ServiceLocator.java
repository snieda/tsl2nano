/*
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.serviceaccess;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Implementing the service locator pattern using the initial-context of ejb 3. initial context will be found through
 * jndi.properties in class path.
 * 
 * the lookup will load the given interfaces using the jndi_name as prefix. On framework classes like IGenericService,
 * the full class name without jndi prefix will be used.
 * 
 * @author TS
 */
public class ServiceLocator {
    Log LOG = LogFactory.getLog(ServiceLocator.class);
    private InitialContext context;
    private final Map<String, Object> services = new Hashtable<String, Object>();
    private ClassLoader classLoader;

    /** different namings on different server implementations! */
    private String jndiFactory;
    private Properties jndiProperties;

    public static final String NO_JNDI = "NO_JNDI";
    static final String KEY_NAMING_FACTORY_INITIAL = "java.naming.factory.initial";
    static final String NAMING_FACTORY_INITIAL_JBOSS = "org.jnp.interfaces.NamingContextFactory";
    static final String NAMING_FACTORY_INITIAL_JBOSS_EMBEDDED = "org.jnp.interfaces.LocalOnlyContextFactory";
    static final String NAMING_FACTORY_INITIAL_GLASSFISH = "com.sun.enterprise.naming.SerialInitContextFactory";

    /**
     * constructor
     */
    public ServiceLocator(ClassLoader classLoader) {
        this(classLoader, "jndi.properties");
    }

    /**
     * constructor
     */
    public ServiceLocator(ClassLoader classLoader, String jndiFileName) {
        super();
        try {
            this.classLoader = classLoader;
            if (NO_JNDI.equals(jndiFileName)) {
                jndiFactory = NO_JNDI;
                return;
            } else {
                // we load the properties through the client main plugin!
                jndiProperties = FileUtil.loadProperties(jndiFileName, classLoader);
                jndiProperties.put(Context.SECURITY_CREDENTIALS, "tsl2.nano.serviceaccess");
                // if server is embedded, start the embedded container
                if (isEmbeddedServer()) {
                    //TODO: only a test - not working yet
                    jndiProperties.put(EJBContainer.APP_NAME, "localApp");
                    jndiProperties.put(EJBContainer.MODULES, new File("../eom-workspace/kion.server/bin"));
                    jndiProperties.put(EJBContainer.PROVIDER, "org.glassfish.ejb.embedded.EJBContainerProviderImpl");

                    context = (InitialContext) EJBContainer.createEJBContainer(jndiProperties).getContext();
                } else {
                    LOG.info("creating InitialContext with properties:\n" + StringUtil.toString(jndiProperties, 300));
                    context = new InitialContext(jndiProperties);
                }
            }
            //show some context informations
            Hashtable<?, ?> ctxEnv;
            ctxEnv = context.getEnvironment();
            LOG.info("initServiceLocator() ctxEnv=" + ctxEnv);
            jndiFactory = (String) ctxEnv.get(KEY_NAMING_FACTORY_INITIAL);
            if (LOG.isDebugEnabled()) {
                final StringBuffer listBindings = new StringBuffer();
                final NamingEnumeration<NameClassPair> nameClassPairs = context.list("");
                while (nameClassPairs.hasMoreElements()) {
                    final NameClassPair pair = nameClassPairs.nextElement();
                    listBindings.append("==> " + pair + "\n");
                    try {
                        final NamingEnumeration<Binding> bindings = context.listBindings(pair.getName());
                        while (bindings.hasMoreElements()) {
                            listBindings.append("    " + bindings.next() + "\n");
                        }
                    } catch (final NotContextException e) {
                        LOG.debug(pair.getName() + " throws: " + e.toString());
                    }
                }
                LOG.debug("list bound beans:\n" + listBindings);
            }
        } catch (final NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return initial context
     */
    public InitialContext getInitialContext() {
        return context;
    }

    /**
     * calls the naming lookup on the initial context. if jndiPrefix is null, the full package+class-name will be used,
     * otherwise: jndiPrefix/simple-classname.
     * 
     * @param jndiPrefix jndi prefix
     * @param <T> session interface
     * @param serviceInterface service
     * @return session bean instance
     */
    @SuppressWarnings("unchecked")
    public <T> T lookup(String jndiPrefix, Class<T> serviceInterface) {
//        boolean isFrameworkService = serviceInterface.getPackage().getName().startsWith(PACKAGE_FRAMEWORK);
        final String name = jndiPrefix != null && jndiPrefix.length() > 0
        /*&& !isFrameworkService*/? jndiPrefix + "/" + serviceInterface.getSimpleName() : serviceInterface.getName();

        //dirty workaround
//        if (isJBossServer() && isFrameworkService)
//            name = name.replaceFirst("IGenericService", "GenericServiceBean");

        T s = (T) services.get(name);

        if (s == null) {
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                LOG.debug("doing lookup for: " + name);
                s = (T) context.lookup(name);
                services.put(name, s);
            } catch (final Exception e) {
                LOG.error("", e);
                throw new RuntimeException(e);
            }
        }
        return s;
    }

    /**
     * isGlassfishServer
     * 
     * @return true, if server is glassfish
     */
    public boolean isGlassfishServer() {
        return jndiFactory.equals(NAMING_FACTORY_INITIAL_GLASSFISH);
    }

    /**
     * isJBossServer
     * 
     * @return true, if server is jboss
     */
    public boolean isJBossServer() {
        return jndiFactory.equals(NAMING_FACTORY_INITIAL_JBOSS);
    }

    /**
     * isJBossServer
     * 
     * @return true, if server is jboss
     */
    public boolean isEmbeddedServer() {
        /*
         * on glassfish-standalone, you don't define any properties for jndi,
         * on jboss-embedded, you have to define :
         *   env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
         *   env.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
         * the jboss-beans.xml should define the org.jnp.interfaces.LocalOnlyContextFactory.
         * see https://community.jboss.org/wiki/LocalOnlyContextFactory.
         */
        return !jndiProperties.containsKey(KEY_NAMING_FACTORY_INITIAL) || jndiProperties.containsValue(NAMING_FACTORY_INITIAL_JBOSS_EMBEDDED);
    }

    /**
     * @param initialServices fill some services to the cache (for testing!)
     */
    protected void setInitialServices(Map<String, Object> initialServices) {
        services.putAll(initialServices);
    }
}
