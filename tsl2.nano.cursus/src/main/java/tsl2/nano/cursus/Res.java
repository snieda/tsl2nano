package tsl2.nano.cursus;

import java.io.Serializable;

import de.tsl2.nano.bean.PReference;
import de.tsl2.nano.core.util.Util;

/**
 * References the real entity instance to change
 * @author Tom
 */
public class Res<O, V> extends PReference<O, V> implements Serializable {
	private static final long serialVersionUID = 1L;
	protected String type;
	protected String path;
	protected Object objectid;
	
	
	public Res() {
	}
	public Res(O instance, String path) {
		super(instance, path);
	}
	public Res(Class type, Object objectid, String path) {
		this(type.getName(), objectid, path);
	}
	public Res(String type, Object objectid, String path) {
		this.type = type;
		this.objectid = objectid;
		this.path = path;
		setDescription(createDescription(type, objectid, path));
	}
	
//	@Override
//	public String toString() {
//		return Util.toString(getClass(), type, objectid, path);
//	}
}