package de.tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{grex}: {effectus}")
@Attributes(names = { "grex", "effectus" })
@Presentable(label = "ΔGrex-Effectus", icon = "icons/links.png")
public class EGrexEffectus implements IPersistable<Long> {
	private static final long serialVersionUID = 1L;

	Long id;
	EGrex grex;
	ERuleEffectus effectus;

	public EGrexEffectus() {
	}

	public EGrexEffectus(EGrex grex, ERuleEffectus effectus) {
		super();
		this.grex = grex;
		this.effectus = effectus;
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

	@ManyToOne
	@JoinColumn
	public EGrex getGrex() {
		return grex;
	}

	public void setGrex(EGrex grex) {
		this.grex = grex;
	}

	@ManyToOne
	@JoinColumn
	public ERuleEffectus getEffectus() {
		return effectus;
	}

	public void setEffectus(ERuleEffectus effectus) {
		this.effectus = effectus;
	}

}
