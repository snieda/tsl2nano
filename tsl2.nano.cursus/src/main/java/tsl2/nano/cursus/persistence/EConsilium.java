package tsl2.nano.cursus.persistence;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Exsecutio;
import tsl2.nano.cursus.Consilium;
import tsl2.nano.cursus.Timer;
import tsl2.nano.cursus.IConsilium.Priority;
import tsl2.nano.cursus.IConsilium.Status;

@Entity
public class EConsilium extends Consilium implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	
	String id;
	
	public EConsilium() {
	}
	public EConsilium(String author, Timer timer, Priority priority, Exsecutio... consecutios) {
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
	public ETimer getTimer() {
		return (ETimer) super.getTimer();
	}
	
	public void setTimer(ETimer timer) {
		this.timer = timer;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
	public Set<EExsecutio<?>> getExsecutios() {
		return (Set<EExsecutio<?>>) exsecutios;
	}

	public void setExsecutios(Set<EExsecutio<?>> exsecutios) {
		this.exsecutios = exsecutios;
	}

}
 