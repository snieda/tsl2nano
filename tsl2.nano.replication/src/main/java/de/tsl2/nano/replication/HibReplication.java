package de.tsl2.nano.replication;

import javax.persistence.EntityManager;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;

/**
 * calls the replicat() method of a hibernate session. The given entity should be detached from source entitymanager/session<br/>
 * WARNING: having src + dest EntityManagers in one Java-VM does not work! tried src.clear(), src.close() and further more..
 * @param <T>
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class HibReplication<T> {
    EntityManager destEM;
    
    public HibReplication(EntityManager destEM) {
        this.destEM = destEM;
    }

    public void strategyHibReplicate(T entity) {
        Session hibernateSession = destEM.unwrap(Session.class);
        hibernateSession.replicate(entity, ReplicationMode.OVERWRITE);
    }
    

}
