/*
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.serviceaccess;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Implementing the service locator pattern using the initial-context of ejb 3.1. Initial context will be found through
 * jndi.properties in class path.
 * 
 * the lookup will load the given interfaces using a jndi_name as prefix. On framework classes like IGenericService, the
 * full class name without jndi prefix will be used.
 * <p/>
 * The property 'jndi.prefix' (default: java:module) can be set through system properties or standard jndi.properties
 * file. if a property {@link #NO_JNDI} is found, no initial context will be used (e.g. for testing purposes).
 * <p/>
 * Tip: If you move your application from an old ejb-version using jndi-mapped names (not supported on ejb 3.1), bind
 * your beans to this mapped name.<br/>
 * Example:
 * 
 * <pre>
 * .bindings.
 *  .lookup name="java:jboss/exported/<my-jndi-prefix>/<my-new-bean-name>" lookup="java:jboss/exported/<my-ear-file>/<my-jar-file>/<my-standard-bean-name>!<my-interface-path>"/.
 * ./bindings.
 * </pre>
 * 
 * @author TS
 */
public class ServiceLocator {
    Log LOG = LogFactory.getLog(ServiceLocator.class);
    private InitialContext context;
    private final Map<String, Object> services = Collections.synchronizedMap(new Hashtable<String, Object>());
    private ClassLoader classLoader;

    /** using cached services directly */
    public static final String NO_JNDI = "NO_JNDI";

    /**
     * constructor
     */
    public ServiceLocator(ClassLoader classLoader, Properties env) {
        super();
        createContext(classLoader, env);
    }

    /**
     * createContext
     * 
     * @param classLoader
     * @param env
     * @throws RuntimeException
     */
    private void createContext(ClassLoader classLoader, Properties env) throws RuntimeException {
        try {
            this.classLoader = classLoader;
            if (env.get(NO_JNDI) != null) {
                return;
            } else {
                LOG.info("creating InitialContext with properties:\n" + StringUtil.toString(env, 300));
                Thread.currentThread().setContextClassLoader(classLoader);
                context = new InitialContext(env);
            }
            //show some context informations
            traceContextInfo();
        } catch (final NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * context debugging info
     * 
     * @throws NamingException
     */
    private void traceContextInfo() throws NamingException {
        Hashtable<?, ?> ctxEnv;
        ctxEnv = context.getEnvironment();
        LOG.info("Initial Context: " + ctxEnv);
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
                } catch (final Exception e) {
                    LOG.debug(pair.getName() + " throws: " + e.toString());
                }
            }
            LOG.debug("list bound beans:\n" + listBindings);
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
     * <p/>
     * 
     * <b>New since j2ee6 (tutorial-extract):</b>
     * 
     * <pre>
     * Portable JNDI Syntax
     * Three JNDI namespaces are used for portable JNDI lookups: java:global, java:module, and java:app.
     * 
     * The java:global JNDI namespace is the portable way of finding remote enterprise beans using JNDI lookups. JNDI addresses are of the following form:
     * 
     * java:global[/application name]/module name/enterprise bean name[/interface name]
     * Application name and module name default to the name of the application and module minus the file extension. Application names are required only if the application is packaged within an EAR. The interface name is required only if the enterprise bean implements more than one business interface.
     * 
     * The java:module namespace is used to look up local enterprise beans within the same module. JNDI addresses using the java:module namespace are of the following form:
     * 
     * java:module/enterprise bean name/[interface name]
     * The interface name is required only if the enterprise bean implements more than one business interface.
     * 
     * The java:app namespace is used to look up local enterprise beans packaged within the same application. That is, the enterprise bean is packaged within an EAR file containing multiple Java EE modules. JNDI addresses using the java:app namespace are of the following form:
     * 
     * java:app[/module name]/enterprise bean name[/interface name]
     * The module name is optional. The interface name is required only if the enterprise bean implements more than one business interface.
     * </pre>
     * 
     * @param jndiPrefix jndi prefix
     * @param <T> session interface
     * @param serviceInterface service
     * @return session bean instance
     */
    @SuppressWarnings("unchecked")
    public <T> T lookup(String jndiPrefix, Class<T> serviceInterface) {
//        boolean isFrameworkService = serviceInterface.getPackage().getName().startsWith(PACKAGE_FRAMEWORK);
        final String name = jndiPrefix != null && jndiPrefix.length() > 0 ? jndiPrefix + "/"
            + serviceInterface.getSimpleName() : serviceInterface.getName();

        T s = (T) services.get(name);

        if (s == null) {
            s = (T) lookup(name);
            services.put(name, s);
        }
        return s;
    }

    /**
     * direct lookup
     * @param name
     * @return context object
     */
    public Object lookup(final String name) {
        try {
            LOG.debug("doing lookup for: " + name);
            Thread.currentThread().setContextClassLoader(classLoader);
            return context.lookup(name);
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * @param initialServices fill some services to the cache (for testing!)
     */
    protected void setInitialServices(Map<String, Object> initialServices) {
        services.putAll(initialServices);
    }
}
