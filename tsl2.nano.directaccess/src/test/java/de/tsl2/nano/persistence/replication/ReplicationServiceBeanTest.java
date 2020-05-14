package de.tsl2.nano.persistence.replication;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.util.FileUtil;

public class ReplicationServiceBeanTest {

	@Before
	public void setUp() {
		FileUtil.copy("META-INF/persistence.tml", "META-INF/persistence.xml");
	}
	@Test
	public void testReplication() {
		ReplicationServiceBean repl = new ReplicationServiceBean();
		repl.persistCollection(Arrays.asList(
				new Person("1", "Muster", Arrays.asList(new Address("2", "Bangkok")))));
	}

}

@Entity
class Person {
	@Id
	String id;
	@Basic
	String name;
	@OneToMany
	Collection<Address> addresses;
	public Person() {
	}
	public Person(String id, String name, Collection<Address> addresses) {
		super();
		this.id = id;
		this.name = name;
		this.addresses = addresses;
	}
}

@Entity
class Address {
	@Id
	String id;
	@Basic
	String city;
	public Address() {
	}
	public Address(String id, String city) {
		super();
		this.id = id;
		this.city = city;
	}
}
