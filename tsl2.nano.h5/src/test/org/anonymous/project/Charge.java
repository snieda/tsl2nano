package org.anonymous.project;
// Generated 14.11.2015 00:32:01 by Hibernate Tools 4.3.1.Final


import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Charge generated by hbm2java
 */
@Entity
@Table(name="CHARGE"
    ,catalog="PUBLIC"
)
public class Charge  implements java.io.Serializable {


     private int id;
     private int party;
     private int chargeitem;
     private Date fromdate;
     private Date fromtime;
     private Date todate;
     private Date totime;
     private Date pause;
     private BigDecimal value;
     private String comment;

    public Charge() {
    }

	
    public Charge(int id, int party, int chargeitem, Date fromdate, Date fromtime, Date todate, Date totime, BigDecimal value) {
        this.id = id;
        this.party = party;
        this.chargeitem = chargeitem;
        this.fromdate = fromdate;
        this.fromtime = fromtime;
        this.todate = todate;
        this.totime = totime;
        this.value = value;
    }
    public Charge(int id, int party, int chargeitem, Date fromdate, Date fromtime, Date todate, Date totime, Date pause, BigDecimal value, String comment) {
       this.id = id;
       this.party = party;
       this.chargeitem = chargeitem;
       this.fromdate = fromdate;
       this.fromtime = fromtime;
       this.todate = todate;
       this.totime = totime;
       this.pause = pause;
       this.value = value;
       this.comment = comment;
    }
   
     @Id 

    
    @Column(name="ID", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    
    @Column(name="PARTY", nullable=false)
    public int getParty() {
        return this.party;
    }
    
    public void setParty(int party) {
        this.party = party;
    }

    
    @Column(name="CHARGEITEM", nullable=false)
    public int getChargeitem() {
        return this.chargeitem;
    }
    
    public void setChargeitem(int chargeitem) {
        this.chargeitem = chargeitem;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="FROMDATE", nullable=false, length=19)
    public Date getFromdate() {
        return this.fromdate;
    }
    
    public void setFromdate(Date fromdate) {
        this.fromdate = fromdate;
    }

    @Temporal(TemporalType.TIME)
    @Column(name="FROMTIME", nullable=false, length=8)
    public Date getFromtime() {
        return this.fromtime;
    }
    
    public void setFromtime(Date fromtime) {
        this.fromtime = fromtime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="TODATE", nullable=false, length=19)
    public Date getTodate() {
        return this.todate;
    }
    
    public void setTodate(Date todate) {
        this.todate = todate;
    }

    @Temporal(TemporalType.TIME)
    @Column(name="TOTIME", nullable=false, length=8)
    public Date getTotime() {
        return this.totime;
    }
    
    public void setTotime(Date totime) {
        this.totime = totime;
    }

    @Temporal(TemporalType.TIME)
    @Column(name="PAUSE", length=8)
    public Date getPause() {
        return this.pause;
    }
    
    public void setPause(Date pause) {
        this.pause = pause;
    }

    
    @Column(name="VALUE", nullable=false, precision=128, scale=0)
    public BigDecimal getValue() {
        return this.value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    
    @Column(name="COMMENT", length=512)
    public String getComment() {
        return this.comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }




}

