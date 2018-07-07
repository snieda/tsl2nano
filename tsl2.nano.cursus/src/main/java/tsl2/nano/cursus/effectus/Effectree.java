package tsl2.nano.cursus.effectus;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;
import de.tsl2.nano.incubation.tree.STree;
import tsl2.nano.cursus.Grex;
import tsl2.nano.cursus.Res;

/**
 * collects all change dependencies in a tree
 * @author Tom
 *
 */
public class Effectree extends STree<Effectree.Entry>{
	private static final long serialVersionUID = 1L;
	private static Effectree self;

	private Effectree() {
	}

	public static Effectree instance() {
		if (self == null)
			self = new Effectree();
		return self;
	}

	/**
	 * creates dependencies between any change and the following effects.<p/>
	 * Tipp: use convenience method effect(..) to build your entries. 
	 * @param mutatio
	 * @param entries
	 * @return 
	 */
	public STree<Entry> addEffects(Class<?> type, String path, Entry...entries) {
		return addEffects(new Res(type, null, path), entries);
	}
	public STree<Entry> addEffects(Res<?, ?> res, Entry...entries) {
		STree<Entry> tree = new STree<>(change(res.getType(), res.getPath()), this);
		tree.add(entries);
		return tree;
	}
	public static Entry change(Res res) {
		//TODO: cast to Class possible?
		return change((Class<?>) res.getType(), res.getPath());
	}
	public static Entry change(Class<?> type, String path) {
		return Effectree.self.new Entry(new Grex<>(type, path), null);
	}
	public static Entry effect(Class<?> type, String path, Class<? extends IChange> effectType) {
		return Effectree.self.new Entry(new Grex<>(type, path), effectType);
	}
	public class Entry {
		Grex<?, ?> grex;
		Class<? extends IChange> change;
		public Entry(Grex<?, ?> grex, Class<? extends IChange> change) {
			super();
			this.grex = grex;
			this.change = change;
		}
		public Grex<?, ?> getGrex() {
			return grex;
		}
		public Class<? extends IChange> getChange() {
			return change;
		}
		@Override
		public int hashCode() {
			return Util.hashCode(grex);
		}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Entry))
				return false;
			Entry m = (Entry) o;
			return Util.equals(this.grex, m.grex);
		}
		@Override
		public String toString() {
			return Util.toString(getClass(), grex, change);
		}
	}
}
