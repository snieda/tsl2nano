package de.tsl2.nano.persistence.replication.hibernate;

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.service.util.IPersistable;

public class EntityReplicationTest  implements ENVTestPreparation {

    @BeforeClass
    public static void setUp() {
        ENVTestPreparation.setUp("entityreplication", false);
    }

    @AfterClass
    public static void tearDown() {
        ENVTestPreparation.tearDown();
    }

    @Test
//    @Ignore("entitymanager and entity must be filled!")
    public void testReplication() {
        MyEntity e = new MyEntity("1");
        
        //TODO: entitymanager and entity must be filled!
        EntityManager destEM = null;
//        new EntityReplication().replicate(new HibReplication(destEM)::strategyHibReplicate, e);
        
        EntityReplication.setPersistableID(e1 -> ((IPersistable<String>)e1).getId());
        new EntityReplication().replicate(e);
        MyEntity loadedEntity = new EntityReplication().load(e.getId(), e.getClass());
        assertEquals(e.getId(), loadedEntity.getId());
    }

}

class MyEntity implements IPersistable<String> {
    private static final long serialVersionUID = 1L;
    String id;
    public MyEntity() {
    }
    public MyEntity(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}