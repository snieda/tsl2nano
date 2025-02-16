package de.tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.cursus.Grex;
import de.tsl2.nano.cursus.Obsidio;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{consiliumID} ({grex}: {timer})")
@Attributes(names = { "consiliumID", "grex", "timer" })
@Presentable(label = "ΔObsidio", icon = "icons/blocked.png", description = "change execution blocker")
public class EObsidio extends Obsidio implements IPersistable<Long> {
	private static final long serialVersionUID = 1L;

	Long id;

	public EObsidio() {
	}

	public EObsidio(String name, String consiliumID, ETimer timer, Grex grex) {
		super(name, consiliumID, timer, grex);
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
	public EConsiliumID getConsiliumID() {
		return (EConsiliumID) consiliumID;
	}

	public void setConsiliumID(EConsiliumID consiliumID) {
		this.consiliumID = consiliumID;
	}

	@ManyToOne
	@JoinColumn
	public EGrex getGrex() {
		return (EGrex) grex;
	}

	public void setGrex(EGrex grex) {
		this.grex = grex;
	}

	@OneToOne
	@JoinColumn
	public ETimer getTimer() {
		return (ETimer) timer;
	}

	public void setTimer(ETimer timer) {
		this.timer = timer;
	}

	public void setName(String name) {
		this.name = name;
	}
}
