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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Party generated by hbm2java
 */
@Entity
@Table(name="PARTY"
    ,schema="PUBLIC"
    ,catalog="SKILLER"
)
public class Party  implements java.io.Serializable {


     private Integer id;
     private String name;
     private String description;
     private String birthday;
     private Set<Address> addresses = new HashSet<Address>(0);
     private Set<Project> projects = new HashSet<Project>(0);
     private Set<Rating> ratings = new HashSet<Rating>(0);

    public Party() {
    }

	
    public Party(String name, String description, String birthday) {
        this.name = name;
        this.description = description;
        this.birthday = birthday;
    }
    public Party(String name, String description, String birthday, Set<Address> addresses, Set<Project> projects, Set<Rating> ratings) {
       this.name = name;
       this.description = description;
       this.birthday = birthday;
       this.addresses = addresses;
       this.projects = projects;
       this.ratings = ratings;
    }
   
     @Id @GeneratedValue(strategy=IDENTITY)
    
    @Column(name="ID", unique=true, nullable=false)
    public Integer getId() {
        return this.id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    @Column(name="NAME", nullable=false, length=64)
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
    
    @Column(name="BIRTHDAY", nullable=false)
    public String getBirthday() {
        return this.birthday;
    }
    
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="party")
    public Set<Address> getAddresses() {
        return this.addresses;
    }
    
    public void setAddresses(Set<Address> addresses) {
        this.addresses = addresses;
    }
@ManyToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    @JoinTable(name="PARTY_PROJECT", schema="PUBLIC", catalog="SKILLER", joinColumns = { 
        @JoinColumn(name="PARTY", nullable=false, updatable=false) }, inverseJoinColumns = { 
        @JoinColumn(name="PROJECT", nullable=false, updatable=false) })
    public Set<Project> getProjects() {
        return this.projects;
    }
    
    public void setProjects(Set<Project> projects) {
        this.projects = projects;
    }
@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="party")
    public Set<Rating> getRatings() {
        return this.ratings;
    }
    
    public void setRatings(Set<Rating> ratings) {
        this.ratings = ratings;
    }




}

