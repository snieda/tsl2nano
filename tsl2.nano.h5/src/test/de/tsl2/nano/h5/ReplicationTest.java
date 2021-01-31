package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.util.test.TypeBean;

public class ReplicationTest {

	@Test
	public void testReplicationMethodAnnotation() {
		try {
			Replication rep = new Replication(Arrays.asList(new TypeBean()));
			rep.replicate("XML", "PUNIT1");
			fail("persistence should not be available!");
		} catch (Exception e) {
			assertEquals("No Persistence provider for EntityManager named genericPersistenceUnit", ManagedException.getRootCause(e).getMessage());
		}
	}

}
