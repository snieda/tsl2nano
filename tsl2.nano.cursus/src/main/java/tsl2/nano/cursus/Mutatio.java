package tsl2.nano.cursus;

import java.io.Serializable;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;

/**
 * describes the change of an item/res from an old to a new value
 * @author Tom
 */
public class Mutatio<O, V> implements IChange, Serializable {
	private static final long serialVersionUID = 1L;
	protected V previous;
	protected V next;
	protected Res<O, V> res;
	
	public Mutatio() {
	}
	
	public Mutatio(V next, Res<O, V> res) {
		this.next = next;
		this.res = res;
	}
	@Override
	public Object getItem() {
		return res;
	}
	@Override
	public V getOld() {
		return previous;
	}
	@Override
	public V getNew() {
		return next;
	}
	@Override
	public IChange revert() {
		return new Mutatio<O, V>(previous, res);
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