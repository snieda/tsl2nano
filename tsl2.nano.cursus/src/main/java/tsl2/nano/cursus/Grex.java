package tsl2.nano.cursus;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanFindParameters;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Provides a generator for Res
 * @author Tom
 *
 * @param <O> instance type
 * @param <V> value type
 */
public class Grex<O, V> implements Serializable {
	private static final long serialVersionUID = 1L;

	protected Res<O, V> genRes;
	/** optional set of object ids */
	protected Set<Object> validObjectIDs;
	
	public Grex() {
		super();
	}
	public Grex(Class<O> type, String path, Object...objectIDs) {
		this(new Res<>(type, "*", path), objectIDs);
	}
	
	public Grex(Res<O, V> genericRes, Object...objectIDs) {
		this.genRes = genericRes;
		if (objectIDs.length > 0)
			this.validObjectIDs = MapUtil.asSet(objectIDs);
	}

	/**
	 * @return all res belonging to this group. Warning: will do calls to the database. if no find parameters were
	 * defined, all object-ids of the group type will be loaded!
	 */
	Set<Res<O, V>> findParts(BeanFindParameters<O> parameters) {
		Set<Res<O, V>> rei = new HashSet<>();
		if (parameters == null)
			parameters = new BeanFindParameters<O>(genRes.getType());
		Collection<O> beans = BeanContainer.instance().getBeans(parameters);
		for (Object obj : beans) {
			rei.add(createResForId(Bean.getBean(obj).getId()));
		}
		return rei;
	}
	
	public Set<Res<O, V>> createParts() {
		return validObjectIDs != null ? createNewParts(validObjectIDs.toArray()) : null;
	}
	public Set<Res<O, V>> createNewParts(Object... objectIds) {
		Set<Res<O, V>> parts = new HashSet<>(objectIds.length);
		for (int i = 0; i < objectIds.length; i++) {
			parts.add(createResForId(objectIds[i]));
		}
		return parts;
	}
	public Res<O, V> createResForId(Object objectId) {
		return new Res<>(genRes.getType(), objectId, genRes.getPath());
	}
	public boolean isPart(Res<O, V> res) {
		return res.type.equals(genRes.type) && res.getPath().equals(genRes.getPath()) 
				&& (validObjectIDs == null || validObjectIDs.contains(res.objectid));
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
