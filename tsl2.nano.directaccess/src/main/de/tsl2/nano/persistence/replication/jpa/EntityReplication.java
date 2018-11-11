package de.tsl2.nano.persistence.replication.jpa;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.persistence.replication.jpa.serializer.SerializeBytes;
import de.tsl2.nano.persistence.replication.jpa.serializer.SerializeJAXB;
import de.tsl2.nano.persistence.replication.jpa.serializer.SerializeXML;
import de.tsl2.nano.persistence.replication.jpa.serializer.Serializer;

/** <pre>
 * Replicates entities through hibernatesession.replicate(). EntityManagers will be load through their persistence unit names. 
 * To use it, 
 *   - create a META-INF/persistence.xml in your classpath
 *   - describe the two persistence-units (source and destination)
 *   - call: new EntityReplication(mySourceUnit, myDestUnit).replicate(new HibReplication(myDestUnit)::strategyHibReplicate, myBeans)
 * or through serliazition:
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
    
    static final List<Serializer> serializer = new LinkedList<>();
    static {
        serializer.add(new SerializeJAXB());
        serializer.add(new SerializeXML());
        serializer.add(new SerializeBytes());
    }

    private EntityManager src;
    private EntityManager dest;
    private static Function persistableID;
    
    public EntityReplication(String srcPersistenceUnit, String destPersistenceUnit) {
        if (!isSerializerOrNull(srcPersistenceUnit)) {
            log("creating EntityManager for " + srcPersistenceUnit);
            src = Persistence.createEntityManagerFactory(srcPersistenceUnit).createEntityManager();
        }
        if (!isSerializerOrNull(destPersistenceUnit)) {
            log("creating EntityManager for " + destPersistenceUnit);
            dest = Persistence.createEntityManagerFactory(destPersistenceUnit).createEntityManager();
        }
        assert src != null || dest != null : "at least one persistence-unit-name must be given!";
    }
    
    public EntityReplication(EntityManager src, EntityManager dest) {
        this.src = src;
        this.dest = dest;
    }

    public EntityReplication() {
    }

    protected static boolean isSerializerOrNull(String name) {
        return name == null || getSerializer(name) != null;
    }
    
    protected static Serializer getSerializer(String key) {
        return serializer.stream().filter(s -> s.getKey().equals(key)).findFirst().orElse(null);
    }
    
    public <T> void replicateFromIDs(Class<T> entityClass, Object...ids) {
        T[] entities = fromIDs(entityClass, ids);
        replicate((Consumer<T>)null, (Consumer<T>)EntityReplication::strategySerializeJAXB, entities);
    }

    protected <T> T[] fromIDs(Class<T> entityClass, Object... ids) {
        T[] entities = Arrays.stream(ids).
                map(id -> src.find(entityClass, id, readOnlyHints())).
                toArray(s -> (T[])java.lang.reflect.Array.newInstance(entityClass, s));
        return entities;
    }
    
    public <T> void replicate(T...entities) {
        replicate((Consumer<T>)EntityReplication::strategySerializeJAXB, entities);
    }
    
    public <T> void replicate(Consumer<T> strategy, T...entities) {
        replicate((Consumer<T>)null, strategy, entities);
    }
    public <T> void replicate(Consumer<T> transformer, Consumer<T> strategy, T...entities) {
        long start = System.currentTimeMillis();
        log("replicating with transformer " + transformer + ", strategy " + strategy + " on " + entities.length + " entities");
        if (dest != null) {
            log("--> begin transaction");
            dest.getTransaction().begin();
        }
        Arrays.stream(entities).forEach(e -> {
            if (transformer != null)
                transformer.accept(e);
            if (strategy != null)
                strategy.accept(e);
            });
        if (dest != null) {
            log("--> commit transaction");
            dest.flush();
            dest.getTransaction().commit();
        }
        log("replication finished successfull! " + "(" + (System.currentTimeMillis() - start) + " ms)");
    }
    
    public static void strategySerializeJAXB(Object entity) {
        strategySerialize(entity, getSerializer(SerializeJAXB.KEY));
    }
    
    public static void strategySerializeBytes(Object entity) {
        strategySerialize(entity, getSerializer(SerializeBytes.KEY));
    }
    
    public static void strategySerializeXML(Object entity) {
        strategySerialize(entity, getSerializer(SerializeXML.KEY));
    }
    
    public static void strategySerialize(Object entity, Serializer serializer) {
        File file = getFile(entity.getClass(), getID(entity), serializer.getExtension());
        log("serializing (" + serializer.getKey() + ") " + entity.getClass() + " to " + file);
        try {
            Files.write(Paths.get(file.getPath()), serializer.serialize(entity).toByteArray());
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }
    
    public void strategyPersist(Object entity) {
        log("persisting " + entity.getClass() + " to " + dest);
        dest.merge(entity);
    }
    
    public static void setPersistableID(Function persistableID) {
        log("setting persistableID=" + persistableID);
        EntityReplication.persistableID = persistableID;
    }

    private static Object getID(Object entity) {
        assert persistableID != null : "please call 'setPersistableID() first!";
        Object id;
        if (persistableID != null) {
            id = persistableID.apply(entity);
        } else {
            String idMethod = System.getProperty("peristableid.access", "getId");
            try {
                id = entity.getClass().getMethod(idMethod, new Class[0]).invoke(entity, new Object[0]);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalArgumentException("ERROR: Please define a peristableid either through calling setPersistableID() or system-property 'peristableid.access' (default: getId <- reflaction!)", e);
            }
        }
        log("handling " + entity.getClass() + ":" + id);
        return id;
    }

    public static <T> List<T> load(Class<T> entityClass, Serializer serializer, Object...ids) {
        ArrayList<T> entities = new ArrayList<T>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            entities.add(load(ids[i], entityClass, serializer));
        }
        return entities;
    }
    
    public static <T> T load(Object id, Class<T> entityClass, Serializer serializer) {
        File file = getFile(entityClass, id, serializer.getExtension());
        try {
            return serializer.deserialize(Files.newInputStream(Paths.get(file.getPath())), entityClass);
        } catch (IOException | ClassNotFoundException e) {
            ManagedException.forward(e);
            return null;
        }
    }
    private static File getFile(Class<?> entityClass, Object id, String extension) {
        return new File(entityClass.getSimpleName() + "-" + id + "." + extension);
    }

    private Map<String, Object> readOnlyHints() {
        HashMap<String, Object> hints = new HashMap<>();
        hints.put("org.hibernate.readOnly", true);
        log("setting entity-manager hints: " + hints);
        return hints;
    }

    public static void checkContent(String srcPersistenceUnit, String destPersistenceUnit, Class<?> entityClass, Object...ids) throws IOException {
        EntityReplication repl = new EntityReplication(srcPersistenceUnit, destPersistenceUnit);
        SerializeBytes s = (SerializeBytes) getSerializer(SerializeBytes.KEY);
        Object srcObj, dstObj;
        for (int i = 0; i < ids.length; i++) {
            srcObj = repl.src.find(entityClass, ids[i]);
            dstObj = repl.dest.find(entityClass, ids[i]);
            assert srcObj != null : ids[i] + " not found in " + repl.src;
            assert dstObj != null : ids[i] + " not found in " + repl.dest;
            assert Arrays.equals(s.serialize(srcObj).toByteArray(), s.serialize(dstObj).toByteArray()) : "objects for " + ids[i] + " differ between both persistences!";
        }
    }
    
    private static void log(String txt, Object...args) {
        System.out.println(EntityReplication.class.getSimpleName() + ": " + String.format(txt, args));
    }

    public static void main(String[] args) throws ClassNotFoundException {
        if (args == null || args.length < 4 || args[0].matches("[-/]+[?h].*")) {
            System.out.println("usage  : {<persunit-src>|ser|xml|jaxb} {<persunit-dest>|ser|xml|jaxb} {<classname>} {<object-id1>, ...}" );
            System.out.println("  exam : mypersistentunit1 xml my.pack.MyClass 1 2 3");
            System.out.println("  exam : xml mypersistentunit2 my.pack.MyClass 1");
            System.out.println("  exam : mypersistentunit1 mypersistentunit2 my.pack.MyClass 1 2 3");
            System.out.println("  exam : -Dperistableid.access=getMyID mypersistentunit1 jaxb my.pack.MyClass 1 2 3");
            return;
        }
            
        String pers1 = args[0];
        String pers2 = args[1];
        System.out.println("===============================================================================");
        EntityReplication repl = new EntityReplication(pers1, pers2);
        Class cls = Thread.currentThread().getContextClassLoader().loadClass(args[2]);
        
        Object[] ids = Arrays.copyOfRange(args, 3, args.length);
        log("starting replication with:" 
                + "\n\tpersistence-unit-1: " + pers1
                + "\n\tpersistence-unit-2: " + pers2
                + "\n\tentity-class      : " + cls
                + "\n\tentity-ids        : " + Arrays.toString(ids));
        Serializer ser;
        if ((ser = getSerializer(pers1)) != null) {
                repl.replicate(repl::strategyPersist, load(cls, ser, ids).toArray());
        } else {
            if ((ser = getSerializer(pers2)) != null) {
                repl.replicateFromIDs(cls, ids);
            } else {
                repl.replicate((Consumer)null, (Consumer)repl::strategyPersist, repl.fromIDs(cls, ids));
            }
        }
        System.out.println("===============================================================================");
    }

}

