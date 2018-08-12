package tsl2.nano.cursus.effectus;

import java.util.LinkedList;
import java.util.List;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.repeat.IChange;
import de.tsl2.nano.incubation.tree.STree;
import de.tsl2.nano.incubation.tree.Tree;
import tsl2.nano.cursus.Grex;
import tsl2.nano.cursus.Res;

/**
 * collects all change dependencies in a tree
 * @author Tom
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Effectree extends STree<Effectree.Entry>{
	private static final long serialVersionUID = 1L;
	private static Effectree self;

	private Effectree() {
	}

	public static Effectree instance() {
		if (self == null) {
			self = new Effectree();
			self.node = self.new Entry(null, null);
		}
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
		Entry node = change(res.getType(), res.getPath());
		this.add(node);
		STree<Entry> tree = getNode(node);
		tree.add(entries);
		return tree;
	}
	public static Entry change(Res res) {
		//TODO: cast to Class possible?
		return change((Class<?>) res.getType(), res.getPath());
	}
	public static Entry change(Class<?> type, String path) {
		return Effectree.instance().new Entry(new Grex<>(type, path), null);
	}
	public static Entry effect(Class<?> type, String path, Class<? extends IChange> effectType, Object effectParameter) {
		return Effectree.instance().new Entry(new Grex<>(type, path), effectType, effectParameter);
	}
	public static List<Effectus> generateEffects(Res res) {
		List<Effectus> effectus = new LinkedList<>();
		Entry entry = Effectree.change(res);
		Tree<Integer, Entry> node = Effectree.instance().getNode(entry);
		if (node != null) {
			List<Tree<?, Effectree.Entry>> tree = node.collectTree(IPredicate.ANY);
			if (tree != null) {
				tree.stream().forEach(t -> {
					Entry n = t.getNode();
					if (n != null && n.getGrex() != null && n.getChangeType() != null) //TODO: wrong tree items?
						effectus.add((Effectus) BeanClass.createInstance(n.getChangeType(), n.getParameter(res.getObjectid())));
				});
			}
		}
		return effectus;
	}
	public class Entry {
		Grex<?, ?> grex;
		Class<? extends IChange> changeType;
		Object[] changeConstructorParameter;
		
		public Entry(Grex<?, ?> grex, Class<? extends IChange> changeType, Object...changeConstructorParameter) {
			super();
			this.grex = grex;
			this.changeType = changeType;
			this.changeConstructorParameter = changeConstructorParameter;
		}
		public Grex<?, ?> getGrex() {
			return grex;
		}
		public Class<? extends IChange> getChangeType() {
			return changeType;
		}
		
		public Object[] getParameter(Object objectid) {
			return CollectionUtil.concat(new Object[] {grex.createResForId(objectid)}, changeConstructorParameter);
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
			return Util.toString(getClass(), grex, changeType);
		}
	}
}
