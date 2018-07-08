package tsl2.nano.cursus.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import de.tsl2.nano.service.util.IPersistable;
import tsl2.nano.cursus.Res;
import tsl2.nano.cursus.effectus.RuleEffectus;

@Entity
public class ERuleEffectus<O, V> extends RuleEffectus<O, V> implements IPersistable<String> {
	private static final long serialVersionUID = 1L;

	String id;
	
	public ERuleEffectus() {
	}

	public ERuleEffectus(Res<O, V> res, boolean fixed, String ruleName) {
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
}
