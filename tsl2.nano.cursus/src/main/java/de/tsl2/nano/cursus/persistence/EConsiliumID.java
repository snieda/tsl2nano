package de.tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{name}")
@Attributes(names = { "name" })
@Presentable(label = "ΔConsilium-ID", icon = "icons/point-green.png")
public class EConsiliumID implements IPersistable<Long> {
	private static final long serialVersionUID = 1L;

	Long id;
	String name;

	public EConsiliumID() {
	}

	public EConsiliumID(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue
	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
