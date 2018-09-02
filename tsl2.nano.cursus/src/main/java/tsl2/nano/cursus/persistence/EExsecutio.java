package tsl2.nano.cursus.persistence;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Exsecutio;

@Entity
@ValueExpression("{name}: {mutatio} -> {effectus}")
@Attributes(names= {"name", "description", "consilium", "mutatio", "effectus"})
@Presentable(label="ΔExsecutio", icon="icons/wrench.png")
public class EExsecutio extends Exsecutio<Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	
	String id;
	EConsilium consilium;
	
	public EExsecutio() {
	}

	public EExsecutio(String name, EMutatio mutatio, String description) {
		super(name, mutatio, description);
	}

	@Id
	@GeneratedValue
	@Presentable(visible=false)
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToOne(mappedBy="exsecutio", cascade=CascadeType.ALL, orphanRemoval=true)
	public EMutatio getMutatio() {
		return (EMutatio) mutatio;
	}

	public void setMutatio(EMutatio mutatio) {
		if (mutatio != null)
			mutatio.setExsecutio(this);
		this.mutatio = mutatio;
	}

	@Override
	@OneToMany(mappedBy="exsecutio", cascade=CascadeType.ALL, orphanRemoval=true)
	public List<ERuleEffectus> getEffectus() {
		return (List<ERuleEffectus>) super.getEffectus();
	}
	
	public void setEffectus(List<ERuleEffectus> effectus) {
		this.effectus = effectus;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ManyToOne @JoinColumn
	public EConsilium getConsilium() {
		return consilium;
	}

	public void setConsilium(EConsilium consilium) {
		this.consilium = consilium;
	}
}
