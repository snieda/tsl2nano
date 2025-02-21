package de.tsl2.nano.cursus.persistence;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.cursus.Consilium;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{name}: {timer} {status}")
@Attributes(names = { "name", "author", "priority", "timer", "exsecutios" })
@Presentable(label = "ΔConsilium", icon = "icons/blue_pin.png", description = "change event descriptor")
public class EConsilium extends Consilium implements IPersistable<Long> {
	private static final long serialVersionUID = 1L;

	Long id;
	EConsiliumID name;

	public EConsilium() {
	}

	public EConsilium(EConsiliumID consiliumID, String author, ETimer timer, Priority priority,
			EExsecutio... exsecutios) {
		super(author, timer, priority, exsecutios);
		name = consiliumID;
	}

	@Id
	@GeneratedValue
	@Presentable(visible = false)
	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAuthor() {
		if (author == null && Persistence.current() != null) {
			author = Persistence.current().getConnectionUserName();
		}
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	@Presentable(visible = false)
	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Presentable(enabled = false)
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

	@Presentable(enabled = false)
	public String getSeal() {
		return seal;
	}

	public void setSeal(String seal) {
		this.seal = seal;
	}

	@Override
	@OneToOne()
	@JoinColumn(nullable = false)
	public ETimer getTimer() {
		return super.getTimer() != null && !(super.getTimer() instanceof ETimer)
				? setTimer(ETimer.from(super.getTimer()))
				: (ETimer) super.getTimer();
	}

	public ETimer setTimer(ETimer timer) {
		return (ETimer) (this.timer = timer);
	}

	@SuppressWarnings("unchecked")
	@OneToMany(mappedBy = "consilium", cascade = CascadeType.ALL, orphanRemoval = true)
	public Set<EExsecutio> getExsecutios() {
		return (Set<EExsecutio>) exsecutios;
	}

	public void setExsecutios(Set<EExsecutio> exsecutios) {
		this.exsecutios = exsecutios;
	}

	@Override
	@Enumerated(EnumType.STRING)
	public Status getStatus() {
		return super.getStatus();
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Set<EConsilium> followers() {
		return getRes().getConsilii().stream().filter(c -> c.compareTo(this) >= 0).collect(Collectors.toSet());
	}

	@Transient
	public ERes getRes() {
		//TODO: check, if exsecutios have different rei
		return !Util.isEmpty(getExsecutios()) ? getExsecutios().iterator().next().getMutatio().getRes() : null;
	}

	@Override
	@JoinColumn(nullable = false)
	public EConsiliumID getName() {
		return name;
	}

	public void setName(EConsiliumID name) {
		this.name = name;
	}
}
