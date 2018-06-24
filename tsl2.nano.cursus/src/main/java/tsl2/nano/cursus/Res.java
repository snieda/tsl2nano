package tsl2.nano.cursus;

import java.io.Serializable;

import javax.persistence.Entity;

import de.tsl2.nano.bean.PReference;
import de.tsl2.nano.core.util.Util;

/**
 * References the real entity instance to change
 * @author Tom
 */
@Entity
class Res extends PReference<Contract, Object> implements Serializable {
	private static final long serialVersionUID = 1L;
	String type;
	String path;
	String objectid;
	public Res(String type, String objectid, String path) {
		this.type = type;
		this.objectid = objectid;
		this.path = path;
		setDescription(createDescription(type, objectid, path));
	}
	@Override
	protected Contract materialize(String description) {
		return CursusTest.origin;
	}
	@Override
	public String toString() {
		return Util.toString(getClass(), objectid, path);
	}
}