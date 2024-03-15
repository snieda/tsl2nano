package de.tsl2.nano.cursus;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.bean.def.SStatus;
import de.tsl2.nano.cursus.persistence.EConsilium;
import de.tsl2.nano.cursus.persistence.EProcess;
import de.tsl2.nano.service.util.IPersistable;

@Entity
@ValueExpression("{process}: {started} {consilium} ({status})")
@Attributes(names = { "process", "started", "consilium", "status" })
@Presentable(label = "ΔProcessLog", icon = "icons/about.png", enabled = false)
public class EProcessLog implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;
	Timestamp started;
	EConsilium consilium;
	SStatus status;
	EProcess process;

	public EProcessLog() {
	}

	public EProcessLog(EProcess process, Timestamp started, EConsilium consilium) {
		this.process = process;
		this.started = started;
		this.consilium = consilium;
	}

	@Id
	@GeneratedValue
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Timestamp getStarted() {
		return started;
	}

	public void setStarted(Timestamp started) {
		this.started = started;
	}

	@ManyToOne
	@JoinColumn
	public EConsilium getConsilium() {
		return consilium;
	}

	public void setConsilium(EConsilium consilium) {
		this.consilium = consilium;
	}

	public SStatus getStatus() {
		return status;
	}

	public void setStatus(SStatus status) {
		this.status = status;
	}

	@ManyToOne
	@JoinColumn
	public EProcess getProcess() {
		return process;
	}

	public void setProcess(EProcess process) {
		this.process = process;
	}
}
