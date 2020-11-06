package de.tsl2.nano.replication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import de.tsl2.nano.replication.serializer.SerializeBytes;
import de.tsl2.nano.replication.serializer.SerializeXML;
import de.tsl2.nano.replication.serializer.Serializer;
import de.tsl2.nano.replication.util.H2Util;
import de.tsl2.nano.replication.util.ULog;
import de.tsl2.nano.replication.util.Util;


/** <pre>
 * Replicates entities through hibernatesession.replicate(). EntityManagers will be load through their persistence unit names. 
 * To use it, 
 *   - create a META-INF/persistence.xml in your classpath
 *   - describe the two persistence-units (source and destination)
 *   - call: new EntityReplication(mySourceUnit, myDestUnit).replicate(new HibReplication(myDestUnit)::strategyHibReplicate, myBeans)
 * or through serlialization:
 *   - EntityReplication.setPersistableID(e -> (IPersistable)e).getId)
 *   - new EntityReplication().replicate(myBeans)
 *   - ...
 *   - new EntityReplication().load(myID, myBeanType.class)
 * </pre>
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class EntityReplication {
	public static final String CONFIG_DIR = "REPL-INF/";
	private static final String PERS_JNDI = "JNDI";
	static final String DEFAULT_PERSISTENCE_XML = "META-INF/persistence.xml";
	
	private EntityManager src;
	private EntityManager dest;
	AtomicReference<EntityManager> tmp = new AtomicReference<EntityManager>();
	private static String persistenceXmlPath = DEFAULT_PERSISTENCE_XML;
	private static Function persistableID;
	
	private JndiLookup.FindByIdAccess jndiEJBSession;
	
	public EntityReplication(String srcPersistenceUnit, String destPersistenceUnit) {
		if (srcPersistenceUnit.equals(PERS_JNDI))
			jndiEJBSession = JndiLookup.createSessionBeanFromJndi();
		if (!isKeywordOrNull(srcPersistenceUnit)) {
			src = createEntityManager(srcPersistenceUnit, Boolean.getBoolean("entityreplication.load.src.extrathread"));
		}
		if (!isKeywordOrNull(destPersistenceUnit)) {
			dest = createEntityManager(destPersistenceUnit, false);
		}
		Util.assert_(src != null || dest != null, "at least one persistence-unit-name must be given!");
	}

	private ClassLoader definePersitenceXmlPath() {
		if (System.getProperty("persistencexml.path") != null)
			persistenceXmlPath = System.getProperty("persistencexml.path");
		if (persistenceXmlPath != null) {
			ClassLoader orgin = Util.linkResourcePath(DEFAULT_PERSISTENCE_XML, persistenceXmlPath);
			return orgin;
		}
		return null;
	}

	protected EntityManager createEntityManager(String punit, boolean threadScope) {
		long start = System.currentTimeMillis();
		if (threadScope) {
			ULog.log("creating EntityManager for '" + punit + "' in new thread scope...", false);
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					ClassLoader cl = definePersitenceXmlPath();
					Properties pers = new Properties();
//					pers.setProperty("hibernate.current_session_context_class", "thread");
//					pers.setProperty("hibernate.connection.pool_size", "10");
					tmp = createEntityManager(punit, cl, pers);
				}
			});
			thread.start();
			try {
				thread.join();
			} catch (Exception e) {
				log("ERROR on creating entitymanager: ", e);
				throw new IllegalStateException(e);
			}
		} else {
			createEntityManager(punit, null, new Properties());
		}
		ULog.log((System.currentTimeMillis() - start) + " ms");
		return tmp.get();
	}
	
	private AtomicReference<EntityManager> createEntityManager(String punit, ClassLoader cl, Properties pers) {
		ULog.log("creating EntityManager for '" + punit + "...", false);
		EntityManagerFactory entityManagerFactory = null;
		try {
			entityManagerFactory = Persistence.createEntityManagerFactory(punit);
			tmp.set(entityManagerFactory.createEntityManager(pers));
			if (cl != null)
				Thread.currentThread().setContextClassLoader(cl);
			return tmp;
		} finally {
			//seams to close the entitymanagers, too
//			if (entityManagerFactory != null)
//				entityManagerFactory.close();
		}
	}
	public EntityReplication(EntityManager src, EntityManager dest) {
		this.src = src;
		this.dest = dest;
	}

	public EntityReplication() {
	}

	protected boolean isKeywordOrNull(String name) {
		return name == null || Serializer.getByKey(name) != null || name.equals(PERS_JNDI);
	}
	
    public <T> void replicateFromIDs(Class<T> entityClass, Serializer ser, Object...ids) {
        T[] entities = fromIDs(entityClass, ids);
        replicate((Consumer<T>)null, s -> strategySerialize(s, ser), entities);
    }
    
	protected <T> T[] fromIDs(Class<T> entityClass, Object... ids) {
		EntityManager em = src != null ? src : dest;
		Util.assert_(em != null, "to find entities through their ids, source or destination entitymanager must not be null!");
		long start = System.currentTimeMillis();
		ULog.log("loading entities of type " + entityClass.getName() + " for " + ids.length + " ids");
		T[] entities = Arrays.stream(ids).
				map(id -> src.find(entityClass, id, readOnlyHints())).filter(e -> e != null).
				toArray(s -> (T[])java.lang.reflect.Array.newInstance(entityClass, s));
        if (ids.length > entities.length) {
        	if (entities.length == 0)
        		throw new IllegalStateException(em + " could not find any entity for ids: " + Arrays.toString(ids));
        	else
        		ULog.log("WARNING: entitymanager " + em +  " couldn't find all ids (" + ids.length + ")! found only: " + entities.length);
        }
		ULog.log("loading entities finshed " + (System.currentTimeMillis() - start) + " ms");
		return entities;
	}
	
	protected <T> T[] fromIDs(JndiLookup.FindByIdAccess ejbSession, Class<T> entityClass, Object... ids) {
		long start = System.currentTimeMillis();
		ULog.log("loading entities of type " + entityClass.getName() + " for " + ids.length + " ids");
		T[] entities = Arrays.stream(ids).
				map(id -> ejbSession.call(entityClass, (Serializable)id)).filter(o -> o != null).
				toArray(s -> (T[])java.lang.reflect.Array.newInstance(entityClass, s));
        if (ids.length > entities.length)
            ULog.log("WARNING: not all ids (" + ids.length + ") were found: " + entities.length);
		ULog.log("loading entities finshed " + (System.currentTimeMillis() - start) + " ms");
		return entities;
	}
	
	public <T> void replicate(T...entities) {
		replicate(EntityReplication::strategySerializeXML, entities);
	}
	
	public <T> void replicate(Consumer<T> strategy, T...entities) {
		replicate((Consumer<T>)null, strategy, entities);
	}
	public <T> void replicate(Consumer<T> transformer, Consumer<T> strategy, T...entities) {
		long start = System.currentTimeMillis();
		Integer transactionBlock = Util.getProperty("transaction.block", Integer.class);
		log("replicating with transformer " + transformer + ", strategy " + strategy + " on " + entities.length + " entities");
		if (transactionBlock == null)
			beginTransation();
		int i[] = {0}, f[] = {0}; 
		Arrays.stream(entities).forEach(e -> {
			try {
			if (transformer != null)
				transformer.accept(e);
			if (strategy != null) {
				boolean doTransaction = transactionBlock != null && (i[0] %transactionBlock) == 0;
				if (doTransaction)
					beginTransation();
				ULog.log("\b" + i[0], false);
				strategy.accept(e);
				if (doTransaction)
					commitTransaction();
				i[0]++;
			} else {
				ULog.log("WARN: no strategy defined --> nothing to do");
			}
			} catch (Exception ex) {
				f[0]++;
				if (dest != null && dest.getTransaction().isActive())
					dest.getTransaction().rollback();
				Util.handleException(ex);
			} 
		});
		if (transactionBlock == null) {
			commitTransaction();
			ULog.log("");
		}
		log("replication finished " + (f[0] == 0 ? "successfull" : "with " + f[0] + " errors") + "! " + "(Entities: " + i[0] + ", "+ (System.currentTimeMillis() - start) + " ms)");
	}

	private void beginTransation() {
		if (dest != null) {
			ULog.log("--> begin transaction...", false);
			dest.getTransaction().begin();
		}
	}
	
	private void commitTransaction() {
		if (dest != null) {
			ULog.log("--> commit transaction...", false);
			H2Util.disableReferentialIntegrity(dest);
			dest.flush();
			dest.getTransaction().commit();
		}
	}

    public static void strategySerializeXML(Object entity) {
        strategySerialize(entity, Serializer.getByKey(SerializeXML.KEY));
    }
    
    public static void strategySerialize(Object entity, Serializer serializer) {
        File file = getFile(entity.getClass(), getID(entity), serializer.getExtension());
        log("serializing (" + serializer.getKey() + ") " + entity.getClass() + " to " + file);
        try {
            Files.write(Paths.get(file.getPath()), serializer.serialize(entity).toByteArray());
        } catch (IOException e) {
            Util.handleException(e);
        }
    }
    
	public void strategyPersist(Object entity) {
		log("persisting " + entity.getClass() + " to " + dest);
		dest.merge(entity);
	}
	
	
	public static void setPersistenceXmlPath(String persistenceXmlPath) {
		EntityReplication.persistenceXmlPath = persistenceXmlPath;
	}

	public static void setPersistableID(Function persistableID) {
		log("setting persistableID=" + persistableID);
		EntityReplication.persistableID = persistableID;
	}

	private static Object getID(Object entity) {
		Object id;
		if (persistableID != null) {
			id = persistableID.apply(entity);
		} else {
			String idMethod = Util.getProperty("peristableid.access", "getId", "");
			try {
				id = entity.getClass().getMethod(idMethod, new Class[0]).invoke(entity);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalArgumentException("ERROR: Please define a peristableid either through calling setPersistableID() or system-property 'peristableid.access' (default: getId <- reflaction!)", e);
			}
		}
		log("handling " + entity.getClass() + ":" + id);
		return id;
	}

    public static <T> List<T> load(Class<T> entityClass, Serializer serializer) {
        ArrayList<T> entities = new ArrayList<T>();
        String type = entityClass.getSimpleName().toLowerCase();
        Arrays.stream(new File(".").listFiles()).forEach(f -> {
        	if (f.isFile() && f.getName().startsWith(type) && f.getName().endsWith(serializer.getExtension())) {
        		entities.add(load(f, entityClass, serializer));
        	}
        });
        return entities;
    }
    
    public static <T> T load(File file, Class<T> entityClass, Serializer serializer) {
        try (InputStream in = Files.newInputStream(Paths.get(file.getPath()))) {
			return serializer.deserialize(in, entityClass);
        } catch (IOException | ClassNotFoundException e) {
            Util.handleException(e);
            return null;
        }
    }
    private static File getFile(Class<?> entityClass, Object id, String extension) {
        return new File(entityClass.getSimpleName().toLowerCase() + (id != null ? "-" + id : "") + "." + extension);
    }

	private Map<String, Object> readOnlyHints() {
		HashMap<String, Object> hints = new HashMap<>();
		hints.put("org.hibernate.readOnly", true);
		log("setting entity-manager hints: " + hints);
		return hints;
	}

    public static void checkContent(String srcPersistenceUnit, String destPersistenceUnit, Class<?> entityClass, Object...ids) throws IOException {
        EntityReplication repl = new EntityReplication(srcPersistenceUnit, destPersistenceUnit);
        SerializeBytes s = (SerializeBytes) Serializer.getByKey(SerializeBytes.KEY);
        Object srcObj, dstObj;
        for (int i = 0; i < ids.length; i++) {
            srcObj = repl.src.find(entityClass, ids[i]);
            dstObj = repl.dest.find(entityClass, ids[i]);
            Util.assert_(srcObj != null, ids[i] + " not found in " + repl.src);
            Util.assert_(dstObj != null, ids[i] + " not found in " + repl.dest);
            Util.assert_(Arrays.equals(s.serialize(srcObj).toByteArray(), s.serialize(dstObj).toByteArray()), "objects for " + ids[i] + " differ between both persistences!");
        }
    }
    
	private static void log(String txt, Object...args) {
		ULog.log(EntityReplication.class.getSimpleName() + ": " + txt, true, args);
	}

	public static void main(String[] args) throws ClassNotFoundException {
		log("===============================================================================");
		Util.printLogo("repl-logo.txt");
		final int MINARGS = 3;
		Properties props = Util.loadPropertiesToSystem(CONFIG_DIR + EntityReplication.class.getSimpleName().toLowerCase() + ".properties");
		args = Util.mergeArgsAndProps(args, args.length, props);
		if (args.length < MINARGS || args[0].matches("[-/]+[?h].*")) {
			log("usage  : {<persunit1>|XML|JAXB|BYTES|YAML|JSON|SIMPLE_XML} {<persunit2>||XML|JAXB|BYTES|YAML|JSON|SIMPLE_XML} {<classname>} {<object-id1>, ...}" );
			log("  e.g. : mypersistentunit1 XML my.pack.MyClass 1 2 3");
			log("  e.g. : XML mypersistentunit2 my.pack.MyClass 1");
			log("  e.g. : mypersistentunit1 mypersistentunit2 my.pack.MyClass 1 2 3");
			log("  e.g. : -Dperistableid.access=getMyID mypersistentunit1 XML my.pack.MyClass 1 2 3");
			log("  e.g. : -Dpersistencexml.path=REPL-INF/persistence.xml mypersistentunit1 xml my.pack.MyClass 1 2 3");
			log("  e.g. : -Duse.hibernate.replication=true -Djndi.prefix=ejb:/myapp/ -Djndi.sessionbean=MySessionBean -Djndi.sessioninterface=MySessionInterface -Djndi.find.method=myFindByID JNDI mypersistentunit2 my.pack.MyClass 1 2 3");
			log("  e.g. : -Dreplication.transformer=de.tsl2.nano.replication.util.SimpleTransformer -Dreplication.transform.regex=my-class+field-find-regex ...");
			log("you can provide properties through system call or through file 'REPL-INF/entityreplication.properties");
			log("the properties may contain the main args instead: syntax: args0={<persunit1>||XML|JAXB|BYTES}, args1={<persunit2>||XML|JAXB|BYTES}, args2={<classname>}, args3={<object-id1>, ...}");
			return;
		}
		String pers1 = args[0];
		String pers2 = args[1];
        String call;
        EntityReplication repl = null;
		try {
	        if ((call = Util.getProperty("on.start.call", null, null)) != null)
	                Util.invoke(call);
			repl = new EntityReplication(pers1, pers2);
			Class cls = Thread.currentThread().getContextClassLoader().loadClass(args[2]);
			boolean useHibernateReplication = Util.getProperty("use.hibernate.replication", Boolean.class);
			Util.assert_(pers2 != PERS_JNDI, "persistence-unit-2 must not be " + PERS_JNDI);
			
			Object[] ids = Arrays.copyOfRange(args, 3, args.length);
			if (ids.length == 1 && ids[0].getClass().equals(String.class))
				ids = ((String)ids[0]).split("[,;| ]");
			log("starting replication with:" 
					+ "\n\tpersistence-unit-1: " + pers1
					+ "\n\tpersistence-unit-2: " + pers2
					+ "\n\tentity-class      : " + cls
					+ "\n\tentity-ids        : " + Arrays.toString(ids));
		
			Consumer transformer = evalTransformer();
	        Serializer ser;
			if ((ser = Serializer.getByKey(pers1)) != null) {
					repl.replicate(transformer, repl::strategyPersist, load(cls, ser).toArray());
			} else {
				if ((ser = Serializer.getByKey(pers2)) != null) {
					repl.replicateFromIDs(cls, ser, ids);
				} else if (pers1.equals(PERS_JNDI)){
					if (useHibernateReplication)
						repl.replicate(transformer, (Consumer)new HibReplication<>(repl.dest)::strategyHibReplicate, repl.fromIDs(repl.jndiEJBSession, cls, ids));
					else
						repl.replicate(transformer, (Consumer)repl::strategyPersist, repl.fromIDs(repl.jndiEJBSession, cls, ids));
				} else {
					repl.replicate((Consumer)transformer, (Consumer)repl::strategyPersist, repl.fromIDs(cls, ids));
				}
			}
		} catch (Throwable e) {
			ULog.log("STOPPED WITH ERROR: " + Util.toString(e));
			throw new RuntimeException(e);
		} finally {
			if (repl != null) {
				if (repl.src != null && repl.src.isOpen())
					repl.src.close();
				if (repl.dest != null && repl.dest.isOpen())
					repl.dest.close();
			}
			ULog.log("\nconsumed properties: " + Util.getConsumedProperties());
			//TODO: store consumed properties
			log("===============================================================================");
            if (Util.getProperty("wait.on.finish", Boolean.class)) {
                if (System.console() != null)
                        System.console().readLine("Please press ENTER to shutdown Java VM: ");
        }
		}
	}

	private static Consumer<?> evalTransformer() {
		String cls = System.getProperty("replication.transformer");
		if (cls != null) {
			try {
				Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(cls);
				log("using transformer: " + clazz);
				return (Consumer<?>) clazz.newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException();
			}
		}
		return null;
	}

	static final void log(Object o) {
		System.out.println(o);
	}
}
