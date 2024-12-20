package org.anonymous.project;
// Generated 04.10.2019 09:31:23 by Hibernate Tools 4.3.1.Final


import java.sql.Blob;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Area generated by hbm2java
 */
@Entity
@Table(name="AREA"
    ,catalog="PUBLIC"
)
public class Area  implements java.io.Serializable {


     private int id;
     private Category category;
     private String name;
     private Blob icon;
     private Set<Type> types = new HashSet<Type>(0);

    public Area() {
    }

	
    public Area(int id, Category category, String name) {
        this.id = id;
        this.category = category;
        this.name = name;
    }
    public Area(int id, Category category, String name, Blob icon, Set<Type> types) {
       this.id = id;
       this.category = category;
       this.name = name;
       this.icon = icon;
       this.types = types;
    }
   
     @Id 

    
    @Column(name="ID", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

@ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="CATEGORY", nullable=false)
    public Category getCategory() {
        return this.category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }

    
    @Column(name="NAME", nullable=false, length=64)
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    
    @Column(name="ICON")
    public Blob getIcon() {
        return this.icon;
    }
    
    public void setIcon(Blob icon) {
        this.icon = icon;
    }

@OneToMany(fetch=FetchType.LAZY, mappedBy="area")
    public Set<Type> getTypes() {
        return this.types;
    }
    
    public void setTypes(Set<Type> types) {
        this.types = types;
    }




}


