package de.tsl2.nano.cursus.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.cursus.Grex;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{genRes}")
@Attributes(names = { "genRes", "validObjectIDs" })
@Presentable(label = "ΔGrex", icon = "icons/cascade.png")
public class EGrex extends Grex<Object, Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;

	public EGrex() {
	}

	public EGrex(Class<Object> type, String path, Object... objectIDs) {
		super(type, path, objectIDs);
	}

	public EGrex(ERes genericRes, Object... objectIDs) {
		super(genericRes, objectIDs);
	}

	@Id
	@GeneratedValue
	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@OneToOne
	@JoinColumn
	public ERes getGenRes() {
		return (ERes) genRes;
	}

	public void setGenRes(ERes genRes) {
		this.genRes = genRes;
	}

	public HashSet<String> getValidObjectIDs() {
		return (HashSet<String>) (HashSet) validObjectIDs;
	}

	public void setValidObjectIDs(HashSet<String> validObjectIDs) {
		this.validObjectIDs = (Set<Object>) (Object) validObjectIDs;
	}

	public ERes createResForId(Object objectId) {
		return new ERes(genRes.getType(), objectId, genRes.getPath());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Set<ERes> createParts() {
		Set<ERes> parts = (Set<ERes>) super.createParts();
		if (parts == null) {
			Object wildcard = genRes.getObjectid();
			Bean bean = Bean.getBean(BeanClass.createInstance(genRes.getType()));
			bean.setId(wildcard);
			Collection<Object> entities = BeanContainer.instance().getBeansByExample(bean.getInstance());
			parts = new HashSet<>(entities.size());
			for (Object e : entities) {
				parts.add(createResForId(new Bean(e).getId()));
			}
		}
		return parts;
	}

	public Object actionTest(Object objectId) {
		return createResForId(objectId).bean();
	}
}
