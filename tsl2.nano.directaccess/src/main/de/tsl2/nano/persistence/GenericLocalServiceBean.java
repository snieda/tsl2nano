/**
 * 
 */
package de.tsl2.nano.persistence;

import java.io.File;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.log.LogFactory;
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
        this(entityManager, Environment.get("use.database.replication", true));
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
        connection().getTransaction().begin();
        try {
            Collection<T> persistCollection = super.persistCollection(beans);
            connection().getTransaction().commit();
            return persistCollection;
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ForwardedException.forward(ex);
            return null;
        }
    }

    /**
     * @see de.tsl2.nano.service.util.GenericServiceBean#persist(java.lang.Object, boolean, boolean)
     */
    @Override
    public <T> T persist(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        connection().getTransaction().begin();
        try {
            T refreshObject = super.persist(bean, refreshBean, flush);
            connection().getTransaction().commit();
            return refreshObject;
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ForwardedException.forward(ex);
            return null;
        }
    }

    /**
     * @see de.tsl2.nano.service.util.GenericServiceBean#remove(java.lang.Object)
     */
    @Override
    public void remove(Object bean) {
        connection().getTransaction().begin();
        try {
            super.remove(bean);
            connection().getTransaction().commit();
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ForwardedException.forward(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int executeQuery(String queryString, boolean nativeQuery, Object[] args) {
        connection().getTransaction().begin();
        try {
            int count = super.executeQuery(queryString, nativeQuery, args);
            connection().getTransaction().commit();
            return count;
        } catch (Exception ex) {
            connection().getTransaction().rollback();
            ForwardedException.forward(ex);
            return -1;
        }
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
        Environment.assignClassloaderToCurrentThread();
        LOG.info("current threads classloader: " + Thread.currentThread().getContextClassLoader());
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
        // ForwardedException.forward(e);
        // }
    }
}
