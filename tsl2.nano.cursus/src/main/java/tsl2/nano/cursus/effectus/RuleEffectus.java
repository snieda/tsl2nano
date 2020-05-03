package tsl2.nano.cursus.effectus;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.incubation.specification.Pool;
import tsl2.nano.cursus.Res;

public class RuleEffectus<O, V> extends Effectus<O, V> {
	private static final long serialVersionUID = 1L;


	private String ruleName;
	
	public RuleEffectus() {
	}

	public RuleEffectus(Res<O, V> res, boolean fixed, String ruleName) {
		super(res, fixed);
		this.ruleName = ruleName;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected V evaluateNewValue() {
		return (V) ENV.get(Pool.class).get(ruleName).run(BeanUtil.toValueMap(getItem()));
	}

	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}
}
