package tsl2.nano.cursus;

import java.io.Serializable;

import javax.persistence.Entity;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;

@Entity
class Mutatio implements IChange, Serializable {
	private static final long serialVersionUID = 1L;
	String previous;
	String next;
	Res res;
	public Mutatio(String next, Res res) {
		this.next = next;
		this.res = res;
	}
	@Override
	public Object getItem() {
		return res;
	}
	@Override
	public Object getOld() {
		return previous;
	}
	@Override
	public Object getNew() {
		return next;
	}
	@Override
	public IChange revert() {
		return new Mutatio(previous, res);
	}
	@Override
	public int hashCode() {
		return Util.hashCode(res, previous, next);
	}
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Mutatio))
			return false;
		Mutatio m = (Mutatio) o;
		return Util.equals(this.res, m.res) && Util.equals(previous, m.previous) && Util.equals(next, m.next);
	}
	@Override
	public String toString() {
		return Util.toString(getClass(), res, previous, next);
	}
}