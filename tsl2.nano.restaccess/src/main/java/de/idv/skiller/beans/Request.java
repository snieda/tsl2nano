package de.idv.skiller.beans;
// Generated 06.06.2016 19:00:14 by Hibernate Tools 3.2.2.GA


import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Request generated by hbm2java
 */
@Entity
@Table(name="REQUEST"
    ,schema="PUBLIC"
    ,catalog="SKILLER"
    , uniqueConstraints = @UniqueConstraint(columnNames="NAME") 
)
public class Request  implements java.io.Serializable {


     private Integer id;
     private String name;
     private String description;
     private String location;
     private String start;
     private String end;
     private Set<Priority> priorities = new HashSet<Priority>(0);

    public Request() {
    }

	
    public Request(String name, String description, String location, String start, String end) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.start = start;
        this.end = end;
    }
    public Request(String name, String description, String location, String start, String end, Set<Priority> priorities) {
       this.name = name;
       this.description = description;
       this.location = location;
       this.start = start;
       this.end = end;
       this.priorities = priorities;
    }
   
     @Id @GeneratedValue(strategy=IDENTITY)
    
    @Column(name="ID", unique=true, nullable=false)
    public Integer getId() {
        return this.id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    @Column(name="NAME", unique=true, nullable=false)
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Column(name="DESCRIPTION", nullable=false)
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Column(name="LOCATION", nullable=false)
    public String getLocation() {
        return this.location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    @Column(name="START", nullable=false)
    public String getStart() {
        return this.start;
    }
    
    public void setStart(String start) {
        this.start = start;
    }
    
    @Column(name="END", nullable=false)
    public String getEnd() {
        return this.end;
    }
    
    public void setEnd(String end) {
        this.end = end;
    }
@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="request")
    public Set<Priority> getPriorities() {
        return this.priorities;
    }
    
    public void setPriorities(Set<Priority> priorities) {
        this.priorities = priorities;
    }




}

