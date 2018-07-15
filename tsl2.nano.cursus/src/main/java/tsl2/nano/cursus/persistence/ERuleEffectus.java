package tsl2.nano.cursus.persistence;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.effectus.RuleEffectus;

@Entity
public class ERuleEffectus extends RuleEffectus<Object, Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;
	
	public ERuleEffectus() {
	}

	public ERuleEffectus(ERes res, boolean fixed, String ruleName) {
		super(res, fixed, ruleName);
	}

	@Id @GeneratedValue
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}
	
	@Override
	protected Object evaluateNewValue() {
		return getRuleName() != null ? super.evaluateNewValue() : null;
	}
	@OneToOne(targetEntity=ERes.class, mappedBy="res", cascade=CascadeType.ALL, orphanRemoval=true)
	@JoinColumn(table="ERES")
	public ERes getRes() {
		return (ERes) res;
	}

	public void setRes(ERes res) {
		this.res = res;
	}

}
