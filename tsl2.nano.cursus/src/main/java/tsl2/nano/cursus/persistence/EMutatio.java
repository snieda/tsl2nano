package tsl2.nano.cursus.persistence;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Mutatio;

@Entity
public class EMutatio extends Mutatio<Object, Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;
	public EMutatio() {
	}
	public EMutatio(String next, ERes res) {
		super(next, res);
	}
	@Id
	@GeneratedValue
	@Override
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getPrevious() {
		return Util.asString(getOld());
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}

	public String getNext() {
		return Util.asString(getNew());
	}

	public void setNext(String next) {
		this.next = next;
	}

	@OneToOne(targetEntity=ERes.class, mappedBy="res", cascade=CascadeType.ALL, orphanRemoval=true)
	@JoinColumn(table="ERES")
	public ERes getRes() {
		return (ERes) res;
	}

	public void setRes(ERes res) {
		this.res = res;
	}

}
