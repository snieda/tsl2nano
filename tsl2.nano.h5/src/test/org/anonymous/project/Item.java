package org.anonymous.project;
// Generated 14.11.2015 00:32:01 by Hibernate Tools 4.3.1.Final


import java.math.BigDecimal;
import java.sql.Blob;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Item generated by hbm2java
 */
@Entity
@Table(name="ITEM"
    ,catalog="PUBLIC"
)
public class Item  implements java.io.Serializable {


     private int id;
     private int orga;
     private int class_;
     private int type;
     private String name;
     private Date start;
     private Date end;
     private BigDecimal value;
     private String description;
     private Blob icon;

    public Item() {
    }

	
    public Item(int id, int orga, int class_, int type, String name, Date start, BigDecimal value) {
        this.id = id;
        this.orga = orga;
        this.class_ = class_;
        this.type = type;
        this.name = name;
        this.start = start;
        this.value = value;
    }
    public Item(int id, int orga, int class_, int type, String name, Date start, Date end, BigDecimal value, String description, Blob icon) {
       this.id = id;
       this.orga = orga;
       this.class_ = class_;
       this.type = type;
       this.name = name;
       this.start = start;
       this.end = end;
       this.value = value;
       this.description = description;
       this.icon = icon;
    }
   
     @Id 

    
    @Column(name="ID", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    
    @Column(name="ORGA", nullable=false)
    public int getOrga() {
        return this.orga;
    }
    
    public void setOrga(int orga) {
        this.orga = orga;
    }

    
    @Column(name="CLASS", nullable=false)
    public int getClass_() {
        return this.class_;
    }
    
    public void setClass_(int class_) {
        this.class_ = class_;
    }

    
    @Column(name="TYPE", nullable=false)
    public int getType() {
        return this.type;
    }
    
    public void setType(int type) {
        this.type = type;
    }

    
    @Column(name="NAME", nullable=false, length=64)
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="START", nullable=false, length=19)
    public Date getStart() {
        return this.start;
    }
    
    public void setStart(Date start) {
        this.start = start;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="END", length=19)
    public Date getEnd() {
        return this.end;
    }
    
    public void setEnd(Date end) {
        this.end = end;
    }

    
    @Column(name="VALUE", nullable=false, precision=128, scale=0)
    public BigDecimal getValue() {
        return this.value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    
    @Column(name="DESCRIPTION", length=256)
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    
    @Column(name="ICON")
    public Blob getIcon() {
        return this.icon;
    }
    
    public void setIcon(Blob icon) {
        this.icon = icon;
    }




}

