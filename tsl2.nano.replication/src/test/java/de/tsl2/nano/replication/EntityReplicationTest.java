package de.tsl2.nano.replication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.junit.Ignore;
import org.junit.Test;
//import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.SupplierExVoid;
import de.tsl2.nano.replication.EntityReplicationTest.MyEntity;
import de.tsl2.nano.replication.serializer.SerializeBytes;
import de.tsl2.nano.replication.serializer.SerializeJAXB;
import de.tsl2.nano.replication.serializer.SerializeJSON;
import de.tsl2.nano.replication.serializer.SerializeSimpleXml;
import de.tsl2.nano.replication.serializer.SerializeYAML;
import de.tsl2.nano.replication.util.H2Util;
import de.tsl2.nano.replication.util.SimpleTransformer;
// import mockit.Expectations;
// import mockit.Injectable;
// import mockit.Mock;
// import mockit.MockUp;
// import mockit.Tested;
// import mockit.Verifications;
// import mockit.integration.junit4.JMockit;

// @RunWith(MockitoJUnitRunner.class)
public class EntityReplicationTest implements ENVTestPreparation {
	@Mock
	EntityManagerFactory entityManagerFactory;
	@Spy
	EntityManager em;
	@Mock
	EntityTransaction transaction;
	@Mock
	InitialContext initialContext;

	// @Mock
	// SerializeXML serializeXML;
	@Mock
	SerializeBytes serializeBytes;
	@Mock
	SerializeJAXB serializeJAXB;
	@Mock
	SerializeYAML serializeYAML;
	@Mock
	SerializeSimpleXml serializeSimpleXml;
	@Mock
	SerializeJSON serializeJSON;

	@Spy
	JndiLookup jndiLookup;
	@Spy
	EntityReplication entityReplication;

	@Before
	public void setUp() throws Exception {
		ENVTestPreparation.super.setUp("replication");
		System.setProperty("entityreplication.load.src.extrathread", "false");
		H2Util.startH2Datbase();
		MockitoAnnotations.openMocks(this);
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

	void mockupEMCreationAndRun(SupplierExVoid<?> testRunner)
			throws NamingException, ClassNotFoundException, IOException {
		try (MockedStatic<Persistence> msPersistence = Mockito.mockStatic(Persistence.class,
				Mockito.CALLS_REAL_METHODS);
				MockedStatic<InitialContext> msInitialContext = Mockito.mockStatic(InitialContext.class,
						Mockito.CALLS_REAL_METHODS)) {
			msPersistence.when(() -> Persistence.createEntityManagerFactory(any())).thenReturn(entityManagerFactory);
			msInitialContext.when((() -> InitialContext.doLookup(BASE_DIR))).thenReturn(new MyService());
			when(entityManagerFactory.createEntityManager(anyMap())).thenReturn(em);
			when(initialContext.lookup(anyString())).thenReturn(new MyService());
			MyEntity myEntity1 = new MyEntity("1");
			// when(serializeXML.deserialize(any(), any())).thenReturn(myEntity1);
			when(serializeBytes.deserialize(any(), any())).thenReturn(myEntity1);
			when(serializeJAXB.deserialize(any(), any())).thenReturn(myEntity1);
			when(serializeYAML.deserialize(any(), any())).thenReturn(myEntity1);
			when(serializeJSON.deserialize(any(), any())).thenReturn(myEntity1);

			when(serializeSimpleXml.deserialize(any(), any())).thenReturn(myEntity1);

			//Mockito static methods only work on main thread
			// when(entityReplication.runAndJoinInThread(any())).thenAnswer(args -> {
			// 	Runnable a = (Runnable) args.getArgument(0);
			// 	if (a != null) {
			// 		a.run();
			// 	}
			// 	return Thread.currentThread();
			// });

			// while no DI but direct call to constructor is done, we have to use the mockConstruction to mock the answer into getWellTypedIds
			try (MockedConstruction<EntityReplication> mcEntityReplication = Mockito.mockConstruction(
					EntityReplication.class,
					(mock, context) -> {
						when(mock.getWellTypedIds(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
					})) {
				testRunner.get();
			}
		}

		// new MockUp<Persistence>() {
		// 	@Mock
		// 	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {
		// 		return entityManagerFactory;
		// 	}
		// };
		// new MockUp<InitialContext>() {
		//     @Mock
		// 	public Object lookup(String name) throws NamingException {
		//     	return new MyService();
		//     }
		// };
		// new MockUp<SerializeXML>() {
		//     @Mock
		// 	public <T> T deserialize(InputStream stream, Class<T> type) {
		//     	return (T) new MyEntity("1");
		//     }
		// };
		// new MockUp<SerializeBytes>() {
		//     @Mock
		// 	public <T> T deserialize(InputStream stream, Class<T> type) {
		//     	return (T) new MyEntity("1");
		//     }
		// };
		// new MockUp<SerializeYAML>() {
		//     @Mock
		// 	public <T> T deserialize(InputStream stream, Class<T> type) {
		//     	return (T) new MyEntity("1");
		//     }
		// };
		// new MockUp<SerializeJAXB>() {
		//     @Mock
		// 	public <T> T deserialize(InputStream stream, Class<T> type) {
		//     	return (T) new MyEntity("1");
		//     }
		// };
		// new MockUp<SerializeJSON>() {
		//     @Mock
		// 	public <T> T deserialize(InputStream stream, Class<T> type) {
		//     	return (T) new MyEntity("1");
		//     }
		// };
		// new MockUp<SerializeSimpleXml>() {
		//     @Mock
		// 	public <T> T deserialize(InputStream stream, Class<T> type) {
		//     	return (T) new MyEntity("1");
		//     }
		// };
		// 		new Expectations() {{
		// //			entityManagerFactory.createEntityManager(); returns(em, em);
		// //			em.getTransaction(); returns(transaction, transaction);
		// 			em.find((Class)any, any, (Map)any); returns(new MyEntity("1"), new MyEntity("1"));
		// 		}};

		// verify(em.find((Class) any(), any(), (Map) any()), times(1));
	}

	@Test
	public void testReplicatonWithPUnit2() throws ClassNotFoundException, IOException, NamingException {
		//simple help-text
		EntityReplication.main(new String[] { "/?" });
		EntityReplication.main(new String[] { "--help" });
		
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		//serialize to xml
		mockupEMCreationAndRun(() -> {
			//serialize to xml
			EntityReplication.main(new String[] { "mypersistence-origin", "nix", MyEntity.class.getName(), myId });
			//deserialize to dest
			EntityReplication.main(new String[] { "nix", "mypersistence-h2", MyEntity.class.getName(), myId });
			// EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
		});
		// new Verifications() {{
		// 	transaction.commit(); times = 2;
		// }};
		// verify(transaction.commit(), times(1));
	}
	
	@Ignore("jmockit -> mockito (hard to resolve...so we ignore it)")
	@Test
	public void testReplicatonWithXml() throws ClassNotFoundException, IOException, NamingException {
		//simple help-text
		EntityReplication.main(new String[] {"/?"});
		EntityReplication.main(new String[] {"--help"});
		
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		System.setProperty("replication.transformer", SimpleTransformer.class.getName());
		mockupEMCreationAndRun(() -> {
			//serialize to xml
			EntityReplication.main(new String[] { "mypersistence-origin", "xml", MyEntity.class.getName(), myId });
			//deserialize to dest
			EntityReplication.main(new String[] { "xml", "mypersistence-h2", MyEntity.class.getName(), myId });
			// EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
		});
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithYaml() throws ClassNotFoundException, IOException, NamingException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		mockupEMCreationAndRun(() -> {
			//serialize to yaml
			EntityReplication.main(new String[] { "mypersistence-origin", "yaml", MyEntity.class.getName(), myId });
			//deserialize to dest
			EntityReplication.main(new String[] { "yaml", "mypersistence-h2", MyEntity.class.getName(), myId });
			// EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
		});
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithJson() throws ClassNotFoundException, IOException, NamingException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		mockupEMCreationAndRun(() -> {
			//serialize to json
			EntityReplication.main(new String[] { "mypersistence-origin", "json", MyEntity.class.getName(), myId });
			//deserialize to dest
			EntityReplication.main(new String[] { "json", "mypersistence-h2", MyEntity.class.getName(), myId });
			// EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
		});
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithJAXB() throws ClassNotFoundException, IOException, NamingException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		mockupEMCreationAndRun(() -> {
			//serialize to jaxb
			EntityReplication.main(new String[] { "mypersistence-origin", "jaxb", MyEntity.class.getName(), myId });
			//deserialize to dest
			EntityReplication.main(new String[] { "jaxb", "mypersistence-h2", MyEntity.class.getName(), myId });
			// EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
		});
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithSimpleXml() throws ClassNotFoundException, IOException, NamingException {
		String myId = "1";
		EntityReplication.setPersistenceXmlPath("REPL-INF/persistence.xml");
		EntityReplication.setPersistableID(e -> ((MyEntity)e).getId());
		mockupEMCreationAndRun(() -> {
			//serialize to xml
			EntityReplication
					.main(new String[] { "mypersistence-origin", "simple_xml", MyEntity.class.getName(), myId });
			//deserialize to dest
			EntityReplication.main(new String[] { "simple_xml", "mypersistence-h2", MyEntity.class.getName(), myId });
			// EntityReplication.checkContent("mypersistence-origin", "mypersistence-h2", MyEntity.class, myId);
		});
//		new Verifications() {{
//			transaction.commit(); times = 1;
//		}};
	}
	
	@Test
	public void testReplicatonWithHibernate() throws ClassNotFoundException, IOException, NamingException {
		System.setProperty("persistencexml.path", "REPL-INF/persistence.xml");
		System.setProperty("peristableid.access", "getId");
		System.setProperty("use.hibernate.replication", "true");
		System.setProperty("jndi.prefix", "ejb:/mypersistence/");
		System.setProperty("jndi.sessionbean", MyEntity.class.getName());
		System.setProperty("jndi.sessioninterface", MyEntity.class.getName());
		System.setProperty("jndi.find.method", "findById");
		System.setProperty("transaction.block", "1");
		String myIds = "1,2,3,4";
		mockupEMCreationAndRun(() -> {
			EntityReplication.main(new String[] { "jndi", "mypersistence-h2", MyEntity.class.getName(), myIds });
		});
		// new Verifications() {{
		// 	transaction.commit(); times = 4;
		// }};
	}

	public static class MyEntity implements Serializable {
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
}

class MyService implements Serializable {
	public MyEntity find(Class cls, String id, Map m) {
		return new MyEntity(id);
	}
}