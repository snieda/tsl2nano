package de.tsl2.nano.ormliteprovider;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Address implements Serializable {

	public Address() {
	}

	public Address(String id, String city, String street) {
		super();
		this.id = id;
		this.city = city;
		this.street = street;
	}
	@Id
	String id;
	@Basic
	String city;
	@Basic
	String street;
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}
}
