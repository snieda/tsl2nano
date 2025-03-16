package de.tsl2.nano.ormliteprovider;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;

public class EntityManagerTest implements ENVTestPreparation {

	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("ormliteprovider");
		
		//create a h2 datbase with the table for our test: Address
	    EntityManagerFactory factory = Persistence.createEntityManagerFactory("test");
	    javax.persistence.EntityManager emPrepare = factory.createEntityManager();
	    
	    ENV.setProperty("service.loadedBeanTypes", Arrays.asList(Address.class));
	}
	@Test
	public void testSimpleCRUD() {
		EntityManager em = new EntityManager(MapUtil.asProperties("jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1", "jdbc.username", "SA", "jdbc.password", "")) {
			
		};
		Address address = new Address("1", "Buxdehude", "Einoede 1");
		em.merge(address);
		em.refresh(address);
		/*assertTrue(*/em.contains(address)/*)*/;
		
		String[] expected = new String[] {"1", "Buxdehude", "Einoede 1"};
		assertTrue(Arrays.deepEquals(expected, ((String[]) em.createQuery("select t from Address t").getResultList().iterator().next())));
		em.detach(address);
		Address address2 = em.find(Address.class, address.id);
		em.remove(address2);
		
		em.clear();
		em.close();
	}

}
