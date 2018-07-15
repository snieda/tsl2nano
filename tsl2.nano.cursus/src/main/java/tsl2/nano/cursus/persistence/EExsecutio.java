package tsl2.nano.cursus.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Exsecutio;
import tsl2.nano.cursus.effectus.Effectus;

@Entity
public class EExsecutio<CONTEXT> extends Exsecutio<CONTEXT> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	
	String id;

	public EExsecutio() {
	}

	public EExsecutio(String name, EMutatio mutatio, String description) {
		super(name, mutatio, description);
	}

	@Id
	@GeneratedValue
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

	@OneToOne(targetEntity=EMutatio.class, mappedBy="mutatio", cascade=CascadeType.ALL, orphanRemoval=true)
	@JoinColumn(table="EMUTATIO")
	public EMutatio getMutatio() {
		return (EMutatio) mutatio;
	}

	public void setMutatio(EMutatio mutatio) {
		this.mutatio = mutatio;
	}

	@Override
	@OneToMany(targetEntity=ERuleEffectus.class, mappedBy="effectus", cascade=CascadeType.ALL, orphanRemoval=true)
	@JoinColumn(table="ERULEEFFECTUS")
	public List<ERuleEffectus> getEffectus() {
		return (ArrayList<ERuleEffectus>) super.getEffectus();
	}
	
	public void setEffectus(Collection<ERuleEffectus> effectus) {
		this.effectus = (List<? extends Effectus>) effectus;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
