package de.tsl2.nano.replication;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.replication.serializer.SerializeBytes;
import de.tsl2.nano.replication.serializer.SerializeJAXB;
import de.tsl2.nano.replication.serializer.SerializeJSON;
import de.tsl2.nano.replication.serializer.SerializeSimpleXml;
import de.tsl2.nano.replication.serializer.SerializeXML;
import de.tsl2.nano.replication.serializer.SerializeYAML;
import de.tsl2.nano.replication.serializer.Serializer;
import de.tsl2.nano.replication.util.H2Util;
import de.tsl2.nano.replication.util.SimpleTransformer;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class EntityReplicationTest implements ENVTestPreparation {
	@Injectable EntityManagerFactory entityManagerFactory;
	@Injectable EntityManager em;
	@Injectable EntityTransaction transaction;
	
	@Tested JndiLookup jndiLookup;
	@Tested EntityReplication entityReplication;
	
	@Before
	public void setUp() throws Exception {
		setUp("replication");
		H2Util.startH2Datbase();
        mockupEMCreation();
	}

	@After
	public void tearDown() throws Exception {
		try {
			H2Util.stopH2Datbase();
		} catch (Exception e) {
			e.printStackTrace(); //keinen neuen Fehler im tearDown erzeugen...
		}
	}

    @BeforeClass
    public static void setUpClass() {
        ENVTestPreparation.setUp("replication", false);
    }

	@AfterClass
    public static void tearDownClass() {
        ENVTestPreparation.tearDown();
    }

    void mockupEMCreation() {
    	new MockUp<Persistence>() {
    		@Mock
    		public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {
    			return entityManagerFactory;
    		}
		};
		new MockUp<InitialContext>() {
		    @Mock
			public Object lookup(String name) throws NamingException {
		    	return new MyService();
		    }
		};
		new MockUp<SerializeXML>() {
		    @Mock
			public <T> T deserialize(InputStream stream, Class<T> type) {
		    	return (T) new MyEntity("1");
		    }
		};
		new MockUp<SerializeBytes>() {
		    @Mock
			public <T> T deserialize(InputStream stream, Class<T> type) {
		    	return (T) new MyEntity("1");
		    }
		};
		new MockUp<SerializeYAML>() {
		    @Mock
			public <T> T deserialize(InputStream stream, Class<T> type) {
		    	return (T) new MyEntity("1");
		    }
		};
		new MockUp<SerializeJAXB>() {
		    @Mock
			public <T> T deserialize(InputStream stream, Class<T> type) {
		    	return (T) new MyEntity("1");
		    }
		};
		new MockUp<SerializeJSON>() {
		    @Mock
			public <T> T deserialize(InputStream stream, Class<T> type) {
		    	return (T) new MyEntity("1");
		    }
		};
		new MockUp<SerializeSimpleXml>() {
		    @Mock
			public <T> T deserialize(InputStream stream, Class<T> type) {
		    	return (T) new MyEntity("1");
		    }
		};
		new Expectations() {{
//			entityManagerFactory.createEntityManager(); returns(em, em);
//			em.getTransaction(); returns(transaction, transaction);
			em.find((Class)any, any, (Map)any); returns(new MyEntity("1"), new MyEntity("1"));
		}};
	}

	@Test
	public void testReplicatonWithPUnit2() throws ClassNotFoundException, IOException {
		//simple help-text
		EntityReplication.main(new String[] {"/?"});
		EntityReplication.main(new String[] {"--help"});
		
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		//serialize to xml
		EntityReplication.main(new String[] {"mypersistence-origin", "nix", MyEntity.class.getName(), myId});
		//deserialize to dest
		EntityReplication.main(new String[] {"nix", "mypersistence-h2", MyEntity.class.getName(), myId});
		
		EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
		new Verifications() {{
			transaction.commit(); times = 2;
		}};
	}
	
	@Test
	public void testReplicatonWithXml() throws ClassNotFoundException, IOException {
		//simple help-text
		EntityReplication.main(new String[] {"/?"});
		EntityReplication.main(new String[] {"--help"});
		
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		System.setProperty("replication.transformer", SimpleTransformer.class.getName());
		//serialize to xml
		EntityReplication.main(new String[] {"mypersistence-origin", "xml", MyEntity.class.getName(), myId});
		//deserialize to dest
		EntityReplication.main(new String[] {"xml", "mypersistence-h2", MyEntity.class.getName(), myId});
		
		EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithYaml() throws ClassNotFoundException, IOException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		//serialize to yaml
		EntityReplication.main(new String[] {"mypersistence-origin", "yaml", MyEntity.class.getName(), myId});
		//deserialize to dest
		EntityReplication.main(new String[] {"yaml", "mypersistence-h2", MyEntity.class.getName(), myId});
		
		EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithJson() throws ClassNotFoundException, IOException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		//serialize to json
		EntityReplication.main(new String[] {"mypersistence-origin", "json", MyEntity.class.getName(), myId});
		//deserialize to dest
		EntityReplication.main(new String[] {"json", "mypersistence-h2", MyEntity.class.getName(), myId});
		
		EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithJAXB() throws ClassNotFoundException, IOException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		//serialize to jaxb
		EntityReplication.main(new String[] {"mypersistence-origin", "jaxb", MyEntity.class.getName(), myId});
		//deserialize to dest
		EntityReplication.main(new String[] {"jaxb", "mypersistence-h2", MyEntity.class.getName(), myId});
		
		EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithSimpleXml() throws ClassNotFoundException, IOException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		//serialize to xml
		EntityReplication.main(new String[] {"mypersistence-origin", "simple_xml", MyEntity.class.getName(), myId});
		//deserialize to dest
		EntityReplication.main(new String[] {"simple_xml", "mypersistence-h2", MyEntity.class.getName(), myId});
		
		EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithHibernate() throws ClassNotFoundException, IOException {
		System.setProperty("persistencexml.path", "REPL-INF/persistence.xml");
		System.setProperty("peristableid.access", "getId");
		System.setProperty("use.hibernate.replication", "true");
		System.setProperty("jndi.prefix", "ejb:/mypersistence/");
		System.setProperty("jndi.sessionbean", MyEntity.class.getName());
		System.setProperty("jndi.sessioninterface", MyEntity.class.getName());
		System.setProperty("jndi.find.method", "findById");
		System.setProperty("transaction.block", "1");
		String myIds = "1,2,3,4";
		
		EntityReplication.main(new String[] {"jndi", "mypersistence-h2", MyEntity.class.getName(), myIds});
		
		new Verifications() {{
			transaction.commit(); times = 4;
		}};
	}
}

class MyEntity implements Serializable {
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

class MyService implements Serializable {
	public MyEntity find(Class cls, String id, Map m) {
		return new MyEntity(id);
	}
}