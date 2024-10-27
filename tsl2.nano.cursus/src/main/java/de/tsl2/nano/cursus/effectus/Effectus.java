package de.tsl2.nano.cursus.effectus;

import de.tsl2.nano.cursus.Mutatio;
import de.tsl2.nano.cursus.Res;

/**
 * an effect can be created/processed after a change was made.
 * @author Tom
 * @param <CONTENT>
 */
public abstract class Effectus<O, V> extends Mutatio<O, V> {
	private static final long serialVersionUID = 1L;
	/** on activation, the changes will be stored into the content - to be reused and not recalculated */
	protected boolean fixed;

	public Effectus() {
	}

	public Effectus(Res<O, V> res, boolean fixed) {
		super(null, res);
		this.fixed = fixed;
	}

	/**
	 * @return new calculated value. to evaluate the new value, the object/res should given to the calculating function.
	 */
	protected abstract V evaluateNewValue();

	@Override
	public V getNew() {
		if (fixed && super.getNew() != null)
			return super.getNew();
		else
			return (next = evaluateNewValue());
	}

	public boolean isFixed() {
		return fixed;
	}
}
