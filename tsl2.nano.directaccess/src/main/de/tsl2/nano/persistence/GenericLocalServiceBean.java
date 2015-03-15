/**
 * 
 */
package de.tsl2.nano.persistence;

import java.io.File;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.persistence.replication.GenericReplicatingServiceBean;

/**
 * This service is not a real session bean - but through the given {@link EntityManager} it is possible, to do the same
 * as the service bean. For Tests and Local purpose only.
 * 
 * @author Thomas Schneider
 */
@SuppressWarnings("rawtypes")
public class GenericLocalServiceBean extends GenericReplicatingServiceBean {
    private static final Log LOG = LogFactory.getLog(GenericLocalServiceBean.class);

    /**
     * default
     */
    public GenericLocalServiceBean() {
        this(createEntityManager("genericPersistenceUnit"));
    }

    /**
     * constructor
     * 
     * @param entityManager persistence entity manager
     */
    public GenericLocalServiceBean(EntityManager entityManager) {
        this(entityManager, ENV.get("use.database.replication", false));
    }

    /**
     * constructor
     * 
     * @param entityManager persistence entity manager
     */
    public GenericLocalServiceBean(EntityManager entityManager, boolean createReplication) {
        super(entityManager, createReplication);
    }

    /**
     * setEntityManager
     * 
     * @param entityManager persistence entity manager
     */
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * @see de.tsl2.nano.service.util.GenericServiceBean#persist(java.util.Collection)
     */
    @Override
    public <T> Collection<T> persistCollection(Collection<T> beans, Class... lazyRelations) {
        requireTransaction();
        try {
            Collection<T> persistCollection = super.persistCollection(beans);
            connection().getTransaction().commit();
            return persistCollection;
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ManagedException.forward(ex);
            return null;
        }
    }

    /**
     * @see de.tsl2.nano.service.util.GenericServiceBean#persist(java.lang.Object, boolean, boolean)
     */
    @Override
    public <T> T persist(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        requireTransaction();
        try {
            T refreshObject = super.persist(bean, refreshBean, flush);
            connection().getTransaction().commit();
            return refreshObject;
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ManagedException.forward(ex);
            return null;
        }
    }

    /**
     * @see de.tsl2.nano.service.util.GenericServiceBean#remove(java.lang.Object)
     */
    @Override
    public void remove(Object bean) {
        requireTransaction();
        try {
            super.remove(bean);
            connection().getTransaction().commit();
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ManagedException.forward(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int executeQuery(String queryString, boolean nativeQuery, Object[] args) {
        requireTransaction();
        try {
            int count = super.executeQuery(queryString, nativeQuery, args);
            connection().getTransaction().commit();
            return count;
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ManagedException.forward(ex);
            return -1;
        }
    }

    /**
     * requireTransaction
     * @return true, if new transaction was created
     */
    private boolean requireTransaction() {
        if (!connection().getTransaction().isActive()) {
            connection().getTransaction().begin();
            return true;
        }
        return false;
    }

    /**
     * @return application context entity manager
     */
    public static EntityManager createEntityManager(String persistenceUnitName) {
        LOG.info("current application path: " + new File(System.getProperty("user.dir")).getAbsolutePath());
        /*
         * the following resource must be found:
         * GenericLocalServiceBean.class.getClassLoader().getResource("META-INF/persistence.xml");
         * so we have to 'extend' the classpath of the hibernate-lib
         */
        ENV.assignClassloaderToCurrentThread();
        LOG.info("current threads classloader: " + Thread.currentThread().getContextClassLoader());
        
//        LOG.info(StringUtil.toFormattedString(BeanClass.call(Persistence.class, "getProviders"), 100));
        
        /*
         * if an orm tool has no javax.persistence provider implementation, it is possible to invoke through
         * setting it as service in the Environment
         */
        EntityManager entityManager = ENV.get(EntityManager.class);
        if (entityManager != null) {
            LOG.info("using a spezialized (not through javax.persistence) entitymanager: " + entityManager);
            return entityManager;
        }
        return Persistence.createEntityManagerFactory(persistenceUnitName).createEntityManager();
        // obtain the initial JNDI context
        // Context initCtx;
        // try {
        // initCtx = new InitialContext();
        // EntityManagerFactory emf = (EntityManagerFactory) initCtx
        // .lookup("java:comp/env/persistence/genericPersistenceUnit");
        // // perform JNDI lookup to obtain entity manager factory
        // // use factory to obtain application-managed entity manager
        // entityManager = emf.createEntityManager();
        // } catch (NamingException e) {
        // ManagedException.forward(e);
        // }
    }
}
