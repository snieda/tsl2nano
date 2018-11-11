package de.tsl2.nano.persistence.replication.jpa;

import javax.persistence.EntityManager;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;

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
