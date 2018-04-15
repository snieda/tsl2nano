package de.tsl2.nano.persistence.replication.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;

/** <pre>
 * Replicates entities through hibernatesession.replicate(). EntityManagers will be load through their persistence unit names. 
 * To use it, 
 *   - create a META-INF/persistence.xml in your classpath
 *   - describe the two persistence-units (source and destination)
 *   - call: new EntityManager(mySourceUnit, myDestUnit).replicate(myBeans)
 *   
 * </pre>
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class EntityReplication {
	private EntityManager src;
	private EntityManager dest;
    public EntityReplication(String destPersistenceUnit) {
        this(null, destPersistenceUnit);
    }
    
	public EntityReplication(String srcPersistenceUnit, String destPersistenceUnit) {
		src = Persistence.createEntityManagerFactory(srcPersistenceUnit).createEntityManager();
		dest = Persistence.createEntityManagerFactory(destPersistenceUnit).createEntityManager();
	}
	
	public EntityReplication(EntityManager src, EntityManager dest) {
		this.src = src;
		this.dest = dest;
	}

	public void replicate(Class<?> entityClass, Object...ids) {
		Session hibernateSession = dest.unwrap(Session.class);
		for (int i = 0; i < ids.length; i++) {
			hibernateSession.replicate(src.find(entityClass, ids[i]), ReplicationMode.OVERWRITE);
		}
		dest.flush();
	}
	
	public void replicate(Object... entities) {
        Session hibernateSession = dest.unwrap(Session.class);
        for (int i = 0; i < entities.length; i++) {
            hibernateSession.replicate(entities[i], ReplicationMode.OVERWRITE);
        }
        dest.flush();
	}
}
