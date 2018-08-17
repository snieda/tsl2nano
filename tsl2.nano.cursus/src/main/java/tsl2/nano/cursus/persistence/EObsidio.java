package tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Grex;
import tsl2.nano.cursus.Obsidio;

@Entity
@ValueExpression(expression="{consiliumID} ({grex}: {timer})")
@Attributes(names= {"consiliumID", "grex", "timer"})
@Presentable(label="ΔObsidio", icon="icons/blocked.png")
public class EObsidio extends Obsidio implements IPersistable<String>{
	private static final long serialVersionUID = 1L;

	String id;
	
	public EObsidio() {
	}

	public EObsidio(String name, String consiliumID, ETimer timer, Grex grex) {
		super(name, consiliumID, timer, grex);
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


	@ManyToOne @JoinColumn
	public EConsiliumID getConsiliumID() {
		return (EConsiliumID) consiliumID;
	}

	public void setConsiliumID(EConsiliumID consiliumID) {
		this.consiliumID = consiliumID;
	}

	@ManyToOne @JoinColumn
	public EGrex getGrex() {
		return (EGrex) grex;
	}

	public void setGrex(EGrex grex) {
		this.grex = grex;
	}

	@OneToOne @JoinColumn
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
