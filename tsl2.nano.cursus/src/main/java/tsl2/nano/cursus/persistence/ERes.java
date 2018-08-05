package tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Constraint;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Res;

@Entity
@ValueExpression(expression="@{typeName}:{objectid}->{path}")
@Attributes(names= {"typeName", "path", "objectid"})
@Presentable(label="ΔRes", icon="icons/images_all.png")
public class ERes extends Res<Object, Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;
	EMutatio mutatio;
	
	@Id @GeneratedValue
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@Constraint(allowed=ConstraintValueSet.ALLOWED_APPCLASSES)
	public String getTypeName() {
		return type;
	}
	public void setTypeName(String type) {
		this.type = type;
		if (objectid != null)
			setDescription(createDescription(type, objectid, path));
	}
	@Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS)
	public String getPath() {
		return super.getPath();
	}
	public void setPath(String path) {
		if (type != null && objectid != null) {
			this.path = null; //path will be evaluated from description 
			setDescription(createDescription(type, objectid, path));
		} else {
			this.path = path;
		}
	}
	public String getObjectid() {
		return Util.asString(objectid);
	}
	public void setObjectid(String objectid) {
		this.objectid = objectid;
		if (type != null && objectid != null)
			setDescription(createDescription(type, objectid, path));
	}
	@ManyToOne @JoinColumn
	public EMutatio getMutatio() {
		return mutatio;
	}
	public void setMutatio(EMutatio mutatio) {
		this.mutatio = mutatio;
	}

	public Object actionTest() {
		if (getDescription() == null)
			throw new IllegalStateException("Please the type and an object id!");
		return bean();
	}
}
