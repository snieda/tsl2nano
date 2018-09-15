package de.tsl2.nano.persistence.replication.hibernate;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.xml.bind.JAXB;

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

public class EntityReplication {
    private EntityManager src;
    private EntityManager dest;
    private static Function persistableID;

    public EntityReplication() {
    }
    
    public EntityReplication(String srcPersistenceUnit, String destPersistenceUnit) {
        src = Persistence.createEntityManagerFactory(srcPersistenceUnit).createEntityManager();
        dest = Persistence.createEntityManagerFactory(destPersistenceUnit).createEntityManager();
    }
    
    public EntityReplication(EntityManager src, EntityManager dest) {
        this.src = src;
        this.dest = dest;
    }

    public <T> void replicateFromIDs(Class<T> entityClass, Object...ids) {
        T[] entities = Arrays.stream(ids).
                map(id -> src.find(entityClass, id, readOnlyHints())).
                toArray(s -> (T[])java.lang.reflect.Array.newInstance(entityClass, s));
        replicate((Function<T, T>)null, (Consumer<T>)EntityReplication::strategySerialize, entities);
    }
    
    public <T> void replicate(T...entities) {
        replicate((Consumer<T>)EntityReplication::strategySerialize, entities);
    }
    
    public <T> void replicate(Consumer<T> strategy, T...entities) {
        replicate((Function<T, T>)null, strategy, entities);
    }
    
    public <T> void replicate(Function<T, T> transformer, Consumer<T> strategy, T...entities) {
        if (dest != null)
            dest.getTransaction().begin();
        Arrays.stream(entities).forEach(e -> {
            //entity must be detached from src session
            if (src != null)
                src.detach(e);
            if (transformer != null)
                e = transformer.apply(e);
            if (strategy != null)
                strategy.accept(e);
            });
        if (dest != null)
            dest.getTransaction().commit();
    }
    
    public static void strategySerialize(Object entity) {
        JAXB.marshal(entity, getFile(entity.getClass(), getID(entity)));
    }
    
    public static void setPersistableID(Function persistableID) {
        EntityReplication.persistableID = persistableID;
    }

    private static Object getID(Object entity) {
        assert persistableID != null : "please call 'setPersistableID() first!";
        return persistableID.apply(entity);
    }

    public static <T> T load(String id, Class<T> entityClass) {
        return load(getFile(entityClass, id), entityClass);
    }
    private static File getFile(Class<?> entityClass, Object id) {
        return new File(entityClass.getSimpleName() + "-" + id + ".xml");
    }

    public static <T> T load(File file, Class<T> entityClass) {
        return JAXB.unmarshal(file, entityClass);
    }
    private Map<String, Object> readOnlyHints() {
        HashMap<String, Object> hints = new HashMap<>();
        hints.put("org.hibernate.readOnly", true);
        return hints;
    }

    /** <pre>
     * Example parameters:
     *  - myPersistenceUnit1
     *  - myPersistenceUnit1
     *  - de.myproject.MyEntity
     *  - 10000000
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 4) {
            EntityReplication repl = new EntityReplication(args[0], args[1]);
            Class<?> entityClass = Class.forName(args[2]);
            Object ids[] = new Object[args.length - 2];
            System.arraycopy(args, 2, ids, 0, args.length - 2);
            repl.replicateFromIDs(entityClass, ids);
        } else {
            System.out.println(EntityReplication.class.getSimpleName() + "\n\tusage: <persistenceunit-src> <persistenceunit-dest> <entity-class> <object-ids...>");
        }
    }
}