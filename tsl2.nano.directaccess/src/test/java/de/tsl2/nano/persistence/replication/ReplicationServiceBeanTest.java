package de.tsl2.nano.persistence.replication;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.junit.Test;

public class ReplicationServiceBeanTest {

	@Test
	public void testReplication() {
		Person p = new Person("1", "Muster", Arrays.asList(new Address("Bangkok")));
		ReplicationServiceBean repl = new ReplicationServiceBean();
		Collection replicated = repl.persistCollection(new ArrayList(Arrays.asList(p)));
		assertEquals(p, replicated.iterator().next());
	}

}

@Entity
class Person {
	@Id
	String id;
	@Basic
	String name;
	@OneToMany(cascade = CascadeType.ALL)
	Collection<Address> addresses;
	public Person() {
	}
	public Person(String id, String name, Collection<Address> addresses) {
		super();
		this.id = id;
		this.name = name;
		setAddresses(addresses);
		
	}
	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : super.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return hashCode() == (obj != null ? obj.hashCode() : 0);
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Collection<Address> getAddresses() {
		return addresses;
	}
	public void setAddresses(Collection<Address> addresses) {
		this.addresses = addresses;
//		for (Address address : addresses) {
//			address.setCyclePerson(this);
//		}
	}
}

@Entity
class Address {
	@Id
	@GeneratedValue
    @Column(unique=true, nullable=false)
	Long id;
	@Basic
	String city;
//	@Basic
//	Person cyclePerson;
	
	public Address() {
	}
	public Address(String city) {
		super();
		this.id = id;
		this.city = city;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
//	public Person getCyclePerson() {
//		return cyclePerson;
//	}
//	public void setCyclePerson(Person cyclePerson) {
//		this.cyclePerson = cyclePerson;
//	}
}
