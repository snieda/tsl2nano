package de.tsl2.nano.persistence.replication.hibernate;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class EntityReplicationTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @Ignore("entitymanager and entity must be filled!")
    public void testReplication() {
        //TODO
        EntityManager destEM = null;
        Object e = null;
        new EntityReplication().replicate(new HibReplication(destEM)::strategyHibReplicate, e);
        new EntityReplication().replicate((Class)e.getClass(), e);
    }

}
