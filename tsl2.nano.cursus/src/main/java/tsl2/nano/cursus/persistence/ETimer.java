package tsl2.nano.cursus.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import de.tsl2.nano.bean.annotation.Attributes;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.bean.annotation.ValueExpression;
import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Timer;

@Entity
@ValueExpression(expression="{from}-{until} ({stepType}:{stepLength})")
@Attributes(names= {"from", "until", "generator", "stepType", "stepLength"})
@Presentable(label="ΔTimer", icon="icons/clock.png")
public class ETimer extends Timer implements IPersistable<String> {
	private static final long serialVersionUID = 1L;
	
	String id;
	
	public ETimer() {
	}

	public ETimer(Date from, Date until, int stepType, int stepLength, Integer... stepExceptions) {
		super(from, until, stepType, stepLength, stepExceptions);
	}

	public ETimer(Date from, Date until) {
		super(from, until);
	}

	@Override
	@Id
	@GeneratedValue
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Temporal(TemporalType.DATE)
	@Column(name="dfrom", length=10)
	public Date getFrom() {
		return from;
	}

	public void setFrom(Date from) {
		this.from = from;
	}

	@Temporal(TemporalType.DATE)
	@Column(name="duntil", length=10)
	public Date getUntil() {
		return until;
	}

	public void setUntil(Date until) {
		this.until = until;
	}

	public int getStepType() {
		return stepType;
	}

	public void setStepType(int stepType) {
		this.stepType = stepType;
	}

	public int getStepLength() {
		return stepLength;
	}

	public void setStepLength(int stepLength) {
		this.stepLength = stepLength;
	}

	// public ArrayList<Integer> getStepExceptions() {
	// 	return stepExceptions;
	// }

	// public void setStepExceptions(ArrayList<Integer> stepExceptions) {
	// 	this.stepExceptions = stepExceptions;
	// }
	
	@Override
	//@Transient //its only a calculated value
	public boolean isGenerator() {
		return super.isGenerator();
	}
	
	public void setGenerator(boolean isGenerator) {
		stepLength = isGenerator ? 1 : 0;
	}

}