package tsl2.nano.cursus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanFindParameters;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.util.Util;

/**
 * Provides a generator for Res
 * @author Tom
 *
 * @param <O> instance type
 * @param <V> value type
 */
public class Grex<O, V> {
	Res<O, V> genRes;
	private transient Set<Res<O, V>> rei;
	private BeanFindParameters<O> parameters;

	public Grex() {
		super();
	}
	public Grex(Class<O> type, String path) {
		this(new Res<>(type, "*", path));
	}
	
	public Grex(Res<O, V> genericRes) {
		this.genRes = genericRes;
	}
	public Grex(Res<O, V> genericRes, BeanFindParameters<O> parameters) {
		super();
		this.genRes = genericRes;
		this.parameters = parameters;
	}
	
	Set<Res<O, V>> getRei() {
		if (rei == null) {
			rei = new HashSet<>();
			if (parameters == null)
				parameters = new BeanFindParameters<O>(genRes.getType());
			Collection<O> beans = BeanContainer.instance().getBeans(parameters);
			for (Object obj : beans) {
				rei.add(createResForId(Bean.getBean(obj).getId()));
			}
		}
		return rei;
	}
	
	public Res<O, V> createResForId(Object objectId) {
		return new Res<>(genRes.type, objectId, genRes.path);
	}
	
	@Override
	public int hashCode() {
		return Util.hashCode(genRes);
	}
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Grex))
			return false;
		Grex m = (Grex) o;
		return Util.equals(this.genRes, m.genRes);
	}
	@Override
	public String toString() {
		return Util.toString(getClass(), genRes);
	}
	
}
