package tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Mutatio;
import tsl2.nano.cursus.Res;

@Entity
public class EMutatio<O> extends Mutatio<O, String> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;
	public EMutatio() {
	}
	public EMutatio(String next, Res<O, String> res) {
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

	public Res<O, String> getRes() {
		return res;
	}

	public void setRes(Res<O, String> res) {
		this.res = res;
	}

}
