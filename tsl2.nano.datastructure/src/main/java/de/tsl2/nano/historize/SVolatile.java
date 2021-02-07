package de.tsl2.nano.historize;

import java.util.function.Supplier;

/**
 * use {@link Volatile} only the {@link Supplier} to eval new value on expiring.
 * <p/>
 * NOTE: the use of {@link #setHardResetOnExpiring(boolean)} and
 * {@link #set(Object)} may result in unpredicted results!
 * 
 * @author ts
 */
public class SVolatile<T> extends Volatile<T> {
	private Supplier<T> callbackNewValue;

	public SVolatile(long period, Supplier<T> callbackNewValue) {
		super(period, null);
		this.callbackNewValue = callbackNewValue;
	}

	@Override
	public T get() {
		return super.get(callbackNewValue);
	}

	@Override
	public T get(Supplier<T> newValueOnExpired) {
		throw new UnsupportedOperationException("please call the get() method without parameter");
	}
}
