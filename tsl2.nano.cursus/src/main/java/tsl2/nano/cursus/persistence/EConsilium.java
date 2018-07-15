package tsl2.nano.cursus.persistence;

import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Consilium;

@Entity
public class EConsilium extends Consilium implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	
	String id;
	
	public EConsilium() {
	}
	public EConsilium(String author, ETimer timer, Priority priority, EExsecutio... consecutios) {
		super(author, timer, priority, consecutios);
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
	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getChanged() {
		return changed;
	}

	public void setChanged(Date changed) {
		this.changed = changed;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public String getSeal() {
		return seal;
	}

	public void setSeal(String seal) {
		this.seal = seal;
	}

	@Override
	@OneToOne(targetEntity=ETimer.class, mappedBy="timer", cascade=CascadeType.ALL, orphanRemoval=true)
	@JoinColumn(table="ETIMER")
	public ETimer getTimer() {
		return (ETimer) super.getTimer();
	}
	
	public void setTimer(ETimer timer) {
		this.timer = timer;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@OneToMany(targetEntity=EExsecutio.class, mappedBy="exsecutios", cascade=CascadeType.ALL, orphanRemoval=true)
	@JoinColumn(table="EEXSECUTIO")
	public Set<EExsecutio<?>> getExsecutios() {
		return (Set<EExsecutio<?>>) exsecutios;
	}

	public void setExsecutios(Set<EExsecutio<?>> exsecutios) {
		this.exsecutios = exsecutios;
	}

	// public void setName(String name) {
	// 	this.name = name;
	// }
}
 