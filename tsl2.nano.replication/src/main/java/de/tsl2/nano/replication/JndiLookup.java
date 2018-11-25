package de.tsl2.nano.replication;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.tsl2.nano.replication.util.ULog;
import de.tsl2.nano.replication.util.Util;

public class JndiLookup {
	private static final String JNDI_PROPERTIES = "jndi.properties";
	private InitialContext initialContext;
	private String prefix;

	private InitialContext initialContext() {
		if (initialContext == null) {
			prefix = Util.getProperty("jndi.prefix", null, "(example: ejb:/myapplication/) in your system properties!");
			initialContext = createInitialContext();
		}
		return initialContext;
	}

	private static InitialContext createInitialContext() {
		ClassLoader origin = null;
		try {
			origin = Util.linkResourcePath(JNDI_PROPERTIES, EntityReplication.CONFIG_DIR + JNDI_PROPERTIES);
			return new InitialContext();
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} finally {
			if (origin != null)
				Thread.currentThread().setContextClassLoader(origin);
		}
	}

	public <T> T lookup(Class<T> bean, Class<T> interfaze) {
		return lookup(bean.getSimpleName() + "!" + interfaze.getCanonicalName());
	}
	public <T> T lookup(Class<T> clazz) {
		return lookup(clazz.getCanonicalName());
	}
	@SuppressWarnings("unchecked")
	public <T> T lookup(String name) {
		try {
			return (T) ULog.call(name, () -> initialContext().lookup(prefix + name));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static FindByIdAccess createSessionBeanFromJndi() {
		ULog.log("creating EJBSession-Bean through jndi...", false);
		String jndiBean = Util.getProperty("jndi.sessionbean", null, "for a jndi lookup");
		String jndiInterface = Util.getProperty("jndi.sessioninterface", null, " for a jndi lookup"); 
		JndiLookup jndiLookup = new JndiLookup();
		Class beanCls, beanInterface;
		try {
			beanCls = Thread.currentThread().getContextClassLoader().loadClass(jndiBean);
			beanInterface = Thread.currentThread().getContextClassLoader().loadClass(jndiInterface);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		Serializable ejbBean = (Serializable) jndiLookup.lookup(beanCls, beanInterface);
		String jndiFinder = Util.getProperty("jndi.find.method", null, " for a jndi lookup");
		return jndiLookup.new FindByIdAccess(ejbBean, jndiFinder);
	}

	public class FindByIdAccess {
		private Serializable bean;
		private String methodName;
		
		public FindByIdAccess(Serializable bean, String methodName) {
			ULog.log("defining jndi-session-call: " + bean + "." + methodName);
			this.bean = bean;
			this.methodName = methodName;
		}
		@SuppressWarnings("unchecked")
		public <T> T call(Class<T> cls, Serializable id) {
			long start = System.currentTimeMillis();
			try {
				ULog.log("remote jndi call on " + methodName + "() with id: " + id, false);
				return (T) bean.getClass().getMethod(methodName, new Class[] {String.class}).invoke(bean, id);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				ULog.log("\n" + e.toString());
				return (T) Util.handleException(e);
			} finally {
				ULog.log(" " + (System.currentTimeMillis() - start) + " ms");
			}
		}
	}
}
