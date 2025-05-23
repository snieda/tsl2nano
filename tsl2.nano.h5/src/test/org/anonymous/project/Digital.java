package org.anonymous.project;
// Generated 04.10.2019 09:31:23 by Hibernate Tools 4.3.1.Final


import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Digital generated by hbm2java
 */
@Entity
@Table(name="DIGITAL"
    ,catalog="PUBLIC"
)
public class Digital  implements java.io.Serializable {


     private int id;
     private int location;
     private String name;
     private String value;
     private Set<Location> locations = new HashSet<Location>(0);

    public Digital() {
    }

	
    public Digital(int id, int location, String name, String value) {
        this.id = id;
        this.location = location;
        this.name = name;
        this.value = value;
    }
    public Digital(int id, int location, String name, String value, Set<Location> locations) {
       this.id = id;
       this.location = location;
       this.name = name;
       this.value = value;
       this.locations = locations;
    }
   
     @Id 

    
    @Column(name="ID", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    
    @Column(name="LOCATION", nullable=false)
    public int getLocation() {
        return this.location;
    }
    
    public void setLocation(int location) {
        this.location = location;
    }

    
    @Column(name="NAME", nullable=false, length=64)
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    
    @Column(name="VALUE", nullable=false, length=64)
    public String getValue() {
        return this.value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }

@OneToMany(fetch=FetchType.LAZY, mappedBy="digital")
    public Set<Location> getLocations() {
        return this.locations;
    }
    
    public void setLocations(Set<Location> locations) {
        this.locations = locations;
    }




}


