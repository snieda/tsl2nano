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
import tsl2.nano.cursus.effectus.RuleEffectus;

@Entity
@ValueExpression(expression="{ruleName}: {res}")
@Attributes(names= {"ruleName", "res"})
@Presentable(label="ΔRule-Effectus", icon="icons/attach.png")
public class ERuleEffectus extends RuleEffectus<Object, Object> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;
	EExsecutio exsecutio;
	
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
	
	@Override
	public boolean isFixed() {
		return super.isFixed();
	}
	
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}
	
	@Override
	protected Object evaluateNewValue() {
		return getRuleName() != null ? super.evaluateNewValue() : null;
	}
	@OneToOne @JoinColumn
	public ERes getRes() {
		return (ERes) res;
	}

	public void setRes(ERes res) {
		this.res = res;
	}

	@ManyToOne
	@JoinColumn
	public EExsecutio getExsecutio() {
		return exsecutio;
	}

	public void setExsecutio(EExsecutio exsecutio) {
		this.exsecutio = exsecutio;
	}

}
