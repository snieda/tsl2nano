/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 19.05.2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.serviceaccess.ServiceFactory;

/**
 * provides some basic service utils
 * 
 * @author ts, Thomas Schneider
 * @version $Revision$
 */
abstract public class AbstractStatelessServiceBean implements IStatelessService {
    static final Log LOG = LogFactory.getLog(AbstractStatelessServiceBean.class);
    static int DEFAULT_MAX_RESULT = 10000;
    static String DEFAULT_LAZY_RELATION_TYPE = "oneToMany";
    static int DEFAULT_MAX_RECURSION_LEVEL = 20;

    Integer maxresult;
    String lazyRelationType;
    Integer maxrecursionlevel;

    private static boolean isSecurityDomainDefined = true;

    /**
     * the genericPersistenceUnit should be linked to the real used unit-name. you do that in the ejb-jar.xml of your
     * service-beans. without this link, we would have two entitymanagers with more than one transaction!
     */
    @PersistenceContext
    protected EntityManager entityManager;

    /** the session context will be used to check the security */
    @Resource
    private SessionContext sessionContext;

    /**
     * encapsulates the entity manager to be exchangable. don't call the entity manager directly!
     * 
     * @return
     */
    public EntityManager connection() {
        return entityManager;
    }

    /**
     * in your business session beans you will use declarative security checks through annotations like
     * <code>@javax.annotation.security.RolesAllowed(IAktenService.ROLE_AKTE_SUCHEN)</code>. in generic services, you
     * will do dynamic security checks through the resource of the sessioncontext.
     * <p/>
     * the jndi properties should set:
     * 
     * <pre>
     * properties.put(Context.INITIAL_CONTEXT_FACTORY, &quot;org.jboss.security.jndi.JndiLoginInitialContextFactory&quot;);
     * properties.put(Context.URL_PKG_PREFIXES, &quot;=org.jboss.naming:org.jnp.interfaces&quot;);
     * properties.put(Context.PROVIDER_URL, &quot;jnp://localhost:1099&quot;);
     * properties.put(Context.SECURITY_PRINCIPAL, princ);
     * </pre>
     * 
     * the jboss ' login-config.xml' should have:<code>
     <application-policy name = "client-login">
       <authentication>
          <login-module code = "org.jboss.security.ClientLoginModule"
             flag = "required">
          </login-module>
       </authentication>
    </application-policy>     
    </code> and the business bean should have the annotation:
     * 
     * <pre>
     * @SecurityDomain("client-login")
     * </pre>
     * 
     * the principal classes should be available inside the servers lib path.
     */
    protected void checkContextSecurity() {
//        //deprecated --> Exception!
//        LOG.info("caller-id:" + sessionContext.getCallerIdentity());
        //on standalone applications (-->no appserver), the sessionContext is null!
        if (sessionContext != null) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("context-data:" + sessionContext.getContextData());
                    if (isSecurityDomainDefined) {
                        LOG.debug("caller-principal:" + sessionContext.getCallerPrincipal());
                    }
                }
            } catch (final Exception e) {
                LOG.warn("to get the caller principal you have to configure the security domain!");
                LOG.error(e);
                //check security domain only once!
                isSecurityDomainDefined = false;
            }
        } else {
            LOG.trace("standalone app --> no sessionContext available");
        }
    }

    /**
     * setParameter
     * 
     * @param query query
     * @param parameter pars to set
     * @return filled query
     */
    protected Query setParameter(Query query, Collection<?> parameter) {
        int i = 1; //position is one-based!
        for (final Object v : parameter) {
            query.setParameter(i++, v);
        }
        return query;
    }

    /**
     * inline to be performant
     * 
     * @param query query to log
     */
    protected static final void logTrace(Query query) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(formatQuery(query));
        }
    }

    /**
     * only for tests - creates an empty server side factory.
     */
    @Override
    public void initServerSideFactories() {
        if (!ServiceFactory.isInitialized()) {
            ServiceFactory.createInstance(this.getClass().getClassLoader());
            ServiceFactory.instance().createSession(null,
                null,
                null,
                new LinkedList<String>(),
                new LinkedList<String>(),
                null);
        }
    }

    @Override
    public Properties getServerInfo() {
        return System.getProperties();
    }

    /**
     * helper to log a query with its parameters
     * 
     * @param query query to analyse
     * @return informations of given query
     */
    public static String formatQuery(Query query) {
        final StringBuilder buf = new StringBuilder("\nQuery: ");
        buf.append("hints = " + query.getHints() + ", ");
        buf.append("lockmode = " + query.getLockMode() + ", ");
        buf.append("flushmode = " + query.getFlushMode() + ", ");
        buf.append("maxresults = " + query.getMaxResults()/* + ", "*/);
        //there is a illegalaccess problem on the parameters
//        buf.append("\nparameter:\n");
//        Set<Parameter<?>> parameters = query.getParameters();
//        for (Parameter<?> parameter : parameters) {
//            buf.append("    " + new Bean(parameter) + "\n");
//        }
        return buf.toString();
    }

    /**
     * used as max result for queries
     * 
     * @return max result, read from serviceaccess.properties
     */
    protected int getMaxResult() {
        if (maxresult == null) {
            LOG.info("EntityManager:\n" + StringUtil.toFormattedString(connection().getProperties(), 50));
            if (ServiceFactory.isInitialized()) {
                final Properties properties = ServiceFactory.instance().getProperties();
                maxresult = Integer.valueOf(properties.getProperty("maxresult", String.valueOf(DEFAULT_MAX_RESULT)));
            } else {
                LOG.warn("servicefactory not initialized or maxresult not defined, using default value: "
                    + DEFAULT_MAX_RESULT);
                maxresult = DEFAULT_MAX_RESULT;
            }
        }
        return maxresult;
    }

    /**
     * used as criteria for lazy loading
     * 
     * @return lazy loading type, read from serviceaccess.properties
     */
    protected String getLazyRelationType() {
        if (lazyRelationType == null) {
            LOG.info("EntityManager:\n" + StringUtil.toFormattedString(connection().getProperties(), 50));
            if (ServiceFactory.isInitialized()) {
                final Properties properties = ServiceFactory.instance().getProperties();
                maxresult = Integer.valueOf(properties.getProperty("lazyrelationtype", DEFAULT_LAZY_RELATION_TYPE));
            } else {
                LOG.warn("servicefactory not initialized or maxresult not defined, using default value: "
                    + DEFAULT_LAZY_RELATION_TYPE);
                lazyRelationType = DEFAULT_LAZY_RELATION_TYPE;
            }
        }
        return lazyRelationType;
    }

    /**
     * used as maximum for preloading lazy relations
     * 
     * @return max recursion level, read from serviceaccess.properties
     */
    protected int getMaxRecursionLevel() {
        if (maxrecursionlevel == null) {
            LOG.info("EntityManager:\n" + StringUtil.toFormattedString(connection().getProperties(), 50));
            if (ServiceFactory.isInitialized()) {
                final Properties properties = ServiceFactory.instance().getProperties();
                maxrecursionlevel =
                    Integer.valueOf(properties.getProperty("maxrecusionlevel",
                        String.valueOf(DEFAULT_MAX_RECURSION_LEVEL)));
            } else {
                LOG.warn("servicefactory not initialized or maxrecusionlevel not defined, using default value: "
                    + DEFAULT_MAX_RECURSION_LEVEL);
                maxrecursionlevel = DEFAULT_MAX_RECURSION_LEVEL;
            }
        }
        return maxrecursionlevel;
    }

    /**
     * isLazyLoadingOnlyOnOneToMany
     * 
     * @return true, if lazyloading is done only on oneToMany relations.
     */
    protected boolean isLazyLoadingOnlyOnOneToMany() {
        return getLazyRelationType().equals(DEFAULT_LAZY_RELATION_TYPE);
    }

    /**
     * getEntityTypes
     * @return persistence-units entity types
     */
    public Collection<EntityType<?>> getEntityTypes() {
        return entityManager.getEntityManagerFactory().getMetamodel().getEntities();
    }

    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	if (entityManager != null && entityManager.isOpen())
    		entityManager.close();
    }
}
