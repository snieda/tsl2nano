package de.tsl2.nano.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

import de.tsl2.nano.core.execution.IRunnable;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;


/**
 * provides some convenience for a persistence with an entitymanager with native queries
 * and executions. In a JTA context you have to give a sessioncontext.
 * <p/>
 *
 * NOTE: method {@link #get(Class, String, Object...)} works not with native
 * query! Use {@link #select(String, Object...)} instead.
 *
 * @author Thomas Schneider
 */
public class SQLQuery implements IRunnable<Object, String>{
	private static final Logger LOG = Logger.getLogger(SQLQuery.class.getSimpleName());
	/** em.getTransaction() only usable on transaction resource local */
	private EntityManager em;
	/** in JTA context this has to be given to use transactions */
	private SessionContext sessionContext;

	public SQLQuery(String persistenceUnitName) {
		this(Persistence.createEntityManagerFactory(persistenceUnitName).createEntityManager());
	}

	/**
	 * in a container with JTA context you have to give the @Resource SessionContext
	 * to get the UserTransaction
	 */
	public SQLQuery(EntityManager em, SessionContext sessionContext) {
		this(em);
		this.sessionContext = sessionContext;
	}

	public SQLQuery(EntityManager em) {
		this.em = em;
	}

	public EntityManager em() {
		return em;
	}

	public void close() {
		if (em != null && em.isOpen()) {
			if (em != null) {
				if (em.getEntityManagerFactory().isOpen())
					em.getEntityManagerFactory().close();
				if (em.isOpen())
					em.close();
			}
		}
	}

	public int execute(String stmt, Object... args) {
		Query query = em.createNativeQuery(stmt);
		return withTransaction(() -> withParameters(query, args).executeUpdate());
	}

	public List<?> select(String stmt, Object... args) {
		Query query = em.createNativeQuery(stmt);
		return print(stmt, withParameters(query, args).getResultList());
	}

	public <T> Query query(Class<T> model, String constraints, Object... args) {
		Query query = em.createNativeQuery("select * from " + model.getSimpleName() + " " + constraints);
		return withParameters(query, args);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> get(Class<T> model, String constraints, Object... args) {
		String stmt = "select t from " + model.getSimpleName() + " t " + constraints;
		Query query = em.createQuery(stmt);
		return print(stmt, withParameters(query, args).getResultList());
	}
	
	public int getCount(Class<?> model, String constraints, Object... args) {
		return getInt("select count(*) from " + model.getSimpleName() + " " + constraints, args);
	}

	public int getInt(String select, Object... args) {
		Query query = em.createNativeQuery(select);
		Object result = withParameters(query, args).getSingleResult();
		print(select, result);
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

    public SessionContext createSessionContextProxyForTest(ClassLoader classLoader) {
        // to many methods to implement , so we use a proxy
        return (SessionContext) Proxy.newProxyInstance(
                classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader(),
                new Class[] { SessionContext.class }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("getUserTransaction")) {
                            return new PTUserTransaction(em);
                        }
                        throw new UnsupportedOperationException();
                    }
                });
    }

	public <T> T withTransaction(Supplier<T> s) {
		try {
			if (getTransaction().getStatus() == 0) // -> Active
				SQLQuery.log("transaction already active: " + getTransaction() + " => " + s);
			else {
				getTransaction().begin();
				log("transaction begin => " + s);
			}
			T result = s.get();
			print("", result);
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

	public static <T> T print(String stmt, T result) {
		ObjectUtil.print(getTitle(stmt, result), result, getHeader(stmt));
		return result;
	}

	private static String[] getHeader(String stmt) {
		if (!stmt.toLowerCase().contains("select "))
			return null;
		String select = StringUtil.substring(stmt, "select ", " from", 0);
		return select == null || select.length() < 2 || select.contains("*") ? null : select.split("[,]");
	}

	private static String getTitle(String stmt, Object result) {
		return result instanceof Number || !stmt.toLowerCase().contains("from ") ? "\t=> result: "
				: StringUtil.substring(stmt, "from ", " ", 0);
	}

	static void log(Object msg) {
		LOG.info(msg.toString());// System.out.println(msg);
	}

	@Override
	public Object run(String stmt, Object... args) {
		return execute(stmt, args);
	}
}

class PTUserTransaction implements UserTransaction {
	EntityManager em;

	public PTUserTransaction(EntityManager em) {
		this.em = em;
	}

    @Override
    public void begin() {
        if (em.getTransaction().isActive())
            SQLQuery.log("WARN: transaction already active -> using that transaction");
        else
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