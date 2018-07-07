package tsl2.nano.cursus.effectus;

import tsl2.nano.cursus.Res;

public class IncEffectus<O> extends Effectus<O, Number> {
	private static final long serialVersionUID = 1L;

	Number incValue;
	
	public IncEffectus() {
	}

	public IncEffectus(Res<O, Number> res, Number increase) {
		super(res, false);
		this.incValue = increase;
	}

	@Override
	protected Number evaluateNewValue() {
		return getOld().doubleValue() + incValue.doubleValue();
	}

}
