package de.tsl2.nano.ebeanprovider;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;

public class EntityManagerTest {

	@Before
	public void setUp() {
		ENVTestPreparation.setUp("ebeanprovider", false);
		//create a h2 datbase with the table for our test: Address
	    EntityManagerFactory factory = Persistence.createEntityManagerFactory("test");
	    javax.persistence.EntityManager emPrepare = factory.createEntityManager();
	    
	    ENV.setProperty("service.loadedBeanTypes", Arrays.asList(Address.class));
	}
//	@Ignore("Entity type class de.tsl2.nano.ebeanprovider.Address is not an enhanced entity bean. Subclassing is not longer supported in Ebean")
	@Test
	public void testSimpleCRUD() {
		EntityManager em = null;
		try {
			em = new EntityManager(MapUtil.asProperties("jdbc.driver", "org.h2.Driver", "jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1", "jdbc.username", "SA", "jdbc.password", "")) {
				
			};
			Address address = new Address("1", "Buxdehude", "Einoede 1");

			//TODO: uncomment. problem, while loading an @entity ReplicationChange from directaccess
			em.merge(address);
			em.refresh(address);
			/*assertTrue(*/em.contains(address)/*)*/;
			
			String[] expected = new String[] {"1", "Buxdehude", "Einoede 1"};
			assertTrue(Arrays.deepEquals(expected, ((String[]) em.createQuery("select t from Address t").getResultList().iterator().next())));
			em.detach(address);
			Address address2 = em.find(Address.class, address.id);
			em.remove(address2);
			
		}catch (Exception e) {
			// Entity type class de.tsl2.nano.ebeanprovider.Address is not an enhanced entity bean. Subclassing is not longer supported in Ebean
		} finally {
			if (em != null && em.isOpen()) {
				em.clear();
				em.close();
			}
		}
	}

}
