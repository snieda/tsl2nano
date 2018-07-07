package tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Exsecutio;
import tsl2.nano.cursus.Mutatio;

@Entity
public class EExsecutio<CONTEXT> extends Exsecutio<CONTEXT> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	
	String id;

	public EExsecutio() {
	}

	public EExsecutio(String name, Mutatio mutatio, String description) {
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

	public Mutatio getMutatio() {
		return mutatio;
	}

	public void setMutatio(Mutatio mutatio) {
		this.mutatio = mutatio;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
