package tsl2.nano.cursus.persistence;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Constraint;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.core.cls.PrivateAccessor;
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
	
	public ERes() {
		super();
	}
	public ERes(Class<Object> type, Object objectid, String path) {
		super(type, objectid, path);
	}
	public ERes(Object instance, String path) {
		super(instance, path);
	}
	public ERes(String type, Object objectid, String path) {
		super(type, objectid, path);
	}
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
			setDescription(createDescription(type, objectid, path != null ? path : path(getDescription())));
	}
	@Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS)
	public String getPath() {
		return super.getPath();
	}
	public void setPath(String path) {
		if (type != null && objectid != null) {
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
			setDescription(createDescription(type, objectid, path != null ? path : path(getDescription())));
	}
	@ManyToOne @JoinColumn
	public EMutatio getMutatio() {
		return mutatio;
	}
	public void setMutatio(EMutatio mutatio) {
		this.mutatio = mutatio;
	}

	@Transient
	public Collection<EConsilium> getConsilii() {
//		String sql = "select c from EConsilium c where c.exsecutios.mutatio.res.objectid = %1";
		EConsilium exCons = new EConsilium(null, null, null, new EExsecutio<>(null, new EMutatio(null, this), null));
		PrivateAccessor<EConsilium> pa = new PrivateAccessor<>(exCons);
		pa.set("status", null);
		pa.set("seal", null);
		pa.set("created", null);
		return BeanContainer.instance().getBeansByExample(exCons);
	}
	public Object actionTest() {
		if (getDescription() == null)
			throw new IllegalStateException("Please provide the type and an object id!");
		return bean();
	}
}
