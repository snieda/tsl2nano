package de.tsl2.nano.script;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;


/**
 * provides some convenience for persistence entitymanager with native queries
 * and executions. In a JTA context you have to give a sessioncontext.
 * <p/>
 *
 * NOTE: method {@link #get(Class, String, Object...)} works not with native
 * query!
 *
 * @author E_Schneider.Thomas
 */
public class PersistenceTool {
	private static final Logger LOG = Logger.getLogger(PersistenceTool.class.getSimpleName());
	/** em.getTransaction() only usable on transaction resource local */
	private EntityManager em;
	/** in JTA context this has to be given to use transactions */
	private SessionContext sessionContext;

	public PersistenceTool(String persistenceUnitName) {
		this(Persistence.createEntityManagerFactory(persistenceUnitName).createEntityManager());
	}

	/**
	 * in a container with JTA context you have to give the @Resource SessionContext
	 * to get the UserTransaction
	 */
	public PersistenceTool(EntityManager em, SessionContext sessionContext) {
		this(em);
		this.sessionContext = sessionContext;
	}

	public PersistenceTool(EntityManager em) {
		this.em = em;
	}

	public EntityManager em() {
		return em;
	}

	public void close() {
		if (em != null && em.isOpen()) {
			em.close();
		}
	}

	public int execute(String stmt, Object... args) {
		Query query = em.createNativeQuery(stmt);
		return withTransaction(() -> /* print( */withParameters(query, args).executeUpdate()/* ) */);
	}

	public List<?> select(String stmt, Object... args) {
		Query query = em.createNativeQuery(stmt);
		return withParameters(query, args).getResultList();
	}

	public <T> Query query(Class<T> model, String constraints, Object... args) {
		Query query = em.createNativeQuery("select * from " + model.getSimpleName() + " " + constraints);
		return withParameters(query, args);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> get(Class<T> model, String constraints, Object... args) {
		Query query = em.createQuery("select t from " + model.getSimpleName() + " t " + constraints);
		return withParameters(query, args).getResultList();
	}

	public int getCount(Class<?> model, String constraints, Object... args) {
		return getInt("select count(*) from " + model.getSimpleName() + " " + constraints, args);
	}

	public int getInt(String select, Object... args) {
		Query query = em.createNativeQuery(select);
		Object result = withParameters(query, args).getSingleResult();
		log("\t => result: " + result);
		return result != null ? ((Number) result).intValue() : Integer.MIN_VALUE;
	}

	public Query withParameters(Query query, Object... args) {
		log("stmt: " + query.toString());
		for (int i = 0; i < args.length; i++) {
			query = query.setParameter(i + 1, args[i]);
		}
		return query;
	}

	public void rollbackIfActive() {
		try {
			if (getTransaction().getStatus() == 0) {
				getTransaction().rollback();
				log("<= transaction rolled back!");
			}
		} catch (IllegalStateException | SecurityException | SystemException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private UserTransaction getTransaction() {
		return sessionContext != null ? sessionContext.getUserTransaction() : new PTUserTransaction(em);
	}

	public <T> T withTransaction(Supplier<T> s) {
		try {
			if (getTransaction().getStatus() == 0) // -> Active
				PersistenceTool.log("transaction already active: " + getTransaction() + " => " + s);
			else {
				getTransaction().begin();
				log("transaction begin => " + s);
			}
			T result = s.get();
			log("\t=> result: " + result);
			getTransaction().commit();
			log("<= transaction commit");
			return result;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} finally {
			rollbackIfActive();
		}
	}

	<T> T print(T result) {
		log("\t=> rows: " + result);
		return result;
	}

	static void log(Object msg) {
		LOG.info(msg.toString());// System.out.println(msg);
	}
}

class PTUserTransaction implements UserTransaction {
	EntityManager em;

	public PTUserTransaction(EntityManager em) {
		this.em = em;
	}

	@Override
	public void begin() {
		em.getTransaction().begin();
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		em.getTransaction().commit();
	}

	@Override
	public int getStatus() throws SystemException {
		/*
		 * STATUS_ACTIVE 0 STATUS_COMMITTED 3 STATUS_COMMITTING 8 STATUS_MARKED_ROLLBACK
		 * 1 STATUS_NO_TRANSACTION 6 STATUS_PREPARED 2 STATUS_PREPARING 7
		 * STATUS_ROLLEDBACK 4 STATUS_ROLLING_BACK 9 STATUS_UNKNOWN 5
		 */
		return em.getTransaction().isActive() ? 0 : 5;
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		em.getTransaction().rollback();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		em.getTransaction().setRollbackOnly();
	}

	@Override
	public void setTransactionTimeout(int arg0) throws SystemException {
	}

	@Override
	public String toString() {
		return em.getTransaction().toString();
	}
}