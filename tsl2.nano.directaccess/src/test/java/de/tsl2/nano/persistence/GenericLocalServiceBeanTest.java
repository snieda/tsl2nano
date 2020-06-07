package de.tsl2.nano.persistence;

import static de.tsl2.nano.service.util.finder.Finder.and;
import static de.tsl2.nano.service.util.finder.Finder.between;
import static de.tsl2.nano.service.util.finder.Finder.example;
import static de.tsl2.nano.service.util.finder.Finder.inSelection;
import static de.tsl2.nano.service.util.finder.Finder.not;
import static de.tsl2.nano.service.util.finder.Finder.or;
import static de.tsl2.nano.service.util.finder.Finder.orderBy;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.junit.After;
import org.junit.Test;

import de.tsl2.nano.bean.BeanFindParameters;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.service.util.IGenericService;
import de.tsl2.nano.service.util.batch.CachingBatchloader;
import de.tsl2.nano.service.util.batch.Part;
import de.tsl2.nano.service.util.finder.Finder;

public class GenericLocalServiceBeanTest {

	@After
	public void tearDown() {
		CachingBatchloader.reset();
	}
	@Test
	public void testGenericLocalFinder() {
		GenericLocalBeanContainer.initLocalContainer();
		IGenericService genService = ENV.get(IGenericService.class);
		
		Person1 p1 = new Person1("1", "Sepp", Arrays.asList(new Address1("Buxdehude")));
		Person1 p2 = new Person1("2", "Trottel", Arrays.asList(new Address1("Berlin")));
		Person1 p3 = new Person1("3", "Depp", Arrays.asList(new Address1("Nirgendwo")));
		
		genService.persistCollection(Arrays.asList(p1 , p2, p3));
		
		assertEquals(3, genService.findAll(new BeanFindParameters(Person1.class, Address1.class)).size());
		
		Collection<Person1> between = genService.findBetween(new Person1(null, "Sepp", null), new Person1(null, "Trottel", null));
		Iterator<Person1> it = between.iterator();
		assertEquals("1", it.next().id);
		assertEquals("2", it.next().id);

		Collection<Person1> inSelection = genService.find(inSelection(Person1.class, "name", Arrays.asList("Depp", "Sepp")));
		Iterator<Person1> iterator = inSelection.iterator();
		assertEquals("1", iterator.next().id);
		assertEquals("3", iterator.next().id);

		Collection<Person1> andnot = genService.find(
				between(new Person1(null, "Depp", null), new Person1(null, "Sepp", null)),
				and(example(new Person1("3", null, null))),
				or(example(new Person1(null, "Depp", null))),
				not(example(new Person1("1", null, null))),
				orderBy(Person1.class, "id")
				);
		it = andnot.iterator();
		assertEquals(1, andnot.size());
		assertEquals("3", it.next().id);
		
		//TODO: test member and holder
//		Collection<Address1> member = genService.find(member(p1, Address1.class, "addresses"));
//		Iterator<Address1> it1 = member.iterator();
//		assertEquals("Buxdehude", it1.next().city);
//
//		Collection<Person1> holder = genService.find(holder(new Address1("Buxdehude"), Person1.class, "addresses"));
//		Iterator<Person1> it2 = holder.iterator();
//		assertEquals("3", it2.next().id);
		
		Part<Person1>[] part = genService.findBatch(new Part<Person1>("test").setFinders(Finder.example(p1)));
		assertEquals(p1, part[0].getResult().iterator().next());
		assertEquals(new Part("test", Person1.class, true), part[0]);

//		ServiceFactory.createInstance(null).setInitialServices(MapUtil.asMap(IGenericService.class.getName(), genService));
		
		CachingBatchloader.init(genService, true).add(new Part<Person1>("test1", Person1.class, true).setFinders(Finder.example(p1)));
		assertEquals(p1, CachingBatchloader.instance().getSingle(Person1.class, "test1"));
	}

}
//we use one persistence.xml for several tests. the mapping classes must different names (--> Person1, Address1)
@Entity
class Person1 {
	@Id
	String id;
	@Basic
	String name;
	@OneToMany(cascade = CascadeType.ALL)
	Collection<Address1> addresses;
	public Person1() {
	}
	public Person1(String id, String name, Collection<Address1> addresses) {
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
	public Collection<Address1> getAddresses() {
		return addresses;
	}
	public void setAddresses(Collection<Address1> addresses) {
		this.addresses = addresses;
	}
}

@Entity
class Address1 {
	@Id
	@GeneratedValue
    @Column(unique=true, nullable=false)
	Long id;
	@Basic
	String city;
	
	public Address1() {
	}
	public Address1(String city) {
		super();
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
}
