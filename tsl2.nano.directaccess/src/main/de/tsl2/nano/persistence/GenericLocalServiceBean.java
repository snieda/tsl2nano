/**
 * 
 */
package de.tsl2.nano.persistence;

import java.io.File;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.Environment;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.service.util.GenericServiceBean;

/**
 * This service is not a real session bean - but through the given {@link EntityManager} it is possible, to do the same
 * as the service bean. For Tests and Local purpose only.
 * 
 * @author Thomas Schneider
 */
public class GenericLocalServiceBean extends GenericServiceBean {
    private static final Log LOG = LogFactory.getLog(GenericLocalServiceBean.class);

    /**
	 * 
	 */
    public GenericLocalServiceBean() {
        super();
        this.entityManager = createEntityManager();
    }

    /**
     * constructor
     * 
     * @param entityManager persistence entity manager
     */
    public GenericLocalServiceBean(EntityManager entityManager) {
        super();
        this.entityManager = entityManager;
    }

    /**
     * getEntityManager
     * 
     * @param entityManager persistence entity manager
     */
    public EntityManager getEntityManager() {
        return entityManager;
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
        entityManager.getTransaction().begin();
        try {
            Collection<T> persistCollection = super.persistCollection(beans);
            entityManager.getTransaction().commit();
            return persistCollection;
        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            ForwardedException.forward(ex);
            return null;
        }
    }

    /**
     * @see de.tsl2.nano.service.util.GenericServiceBean#persist(java.lang.Object, boolean, boolean)
     */
    @Override
    public <T> T persist(T bean, boolean refreshBean, boolean flush, Class... lazyRelations) {
        entityManager.getTransaction().begin();
        try {
            T refreshObject = super.persist(bean, refreshBean, flush);
            entityManager.getTransaction().commit();
            return refreshObject;
        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            ForwardedException.forward(ex);
            return null;
        }
    }

    /**
     * @see de.tsl2.nano.service.util.GenericServiceBean#remove(java.lang.Object)
     */
    @Override
    public void remove(Object bean) {
        entityManager.getTransaction().begin();
        try {
            super.remove(bean);
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            ForwardedException.forward(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int executeQuery(String queryString, boolean nativeQuery, Object[] args) {
        entityManager.getTransaction().begin();
        try {
            int count = super.executeQuery(queryString, nativeQuery, args);
            entityManager.getTransaction().commit();
            return count;
        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            ForwardedException.forward(ex);
            return -1;
        }
    }

    /**
     * @return application context entity manager
     */
    public static EntityManager createEntityManager() {
        LOG.info("current application path: " + new File(System.getProperty("user.dir")).getAbsolutePath());
        /*
         * the following resource must be found:
         * GenericLocalServiceBean.class.getClassLoader().getResource("META-INF/persistence.xml");
         * so we have to 'extend' the classpath of the hibernate-lib
         */
		Thread.currentThread().setContextClassLoader(Environment.get(ClassLoader.class));
        return Persistence.createEntityManagerFactory("genericPersistenceUnit").createEntityManager();
        // obtain the initial JNDI context
        // Context initCtx;
        // try {
        // initCtx = new InitialContext();
        // EntityManagerFactory emf = (EntityManagerFactory) initCtx
        // .lookup("java:comp/env/persistence/kionPersistenceUnit");
        // // perform JNDI lookup to obtain entity manager factory
        // // use factory to obtain application-managed entity manager
        // entityManager = emf.createEntityManager();
        // } catch (NamingException e) {
        // // TODO Auto-generated catch block
        // ForwardedException.forward(e);
        // }
    }
}