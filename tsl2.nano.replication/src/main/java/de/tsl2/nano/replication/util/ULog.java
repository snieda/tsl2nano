package de.tsl2.nano.replication.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public class ULog {
	public static <T> T call(String txt, LSupplier<T>...callbacks) throws Exception {
		long start = System.currentTimeMillis();
		log(txt + "...", false, null);
		Optional<T> result = Arrays.stream(callbacks).map(c -> c.get()).findFirst();
		System.out.println((System.currentTimeMillis() - start) + " ms");
		return result.get();
	}
	public static void log(String txt) {
		log(txt, true);
	}
	public static void log(String txt, boolean newline, Object...args) {
		System.out.print(String.format(txt, args) + (newline ? "\n" : ""));
	}

	@FunctionalInterface
	public interface LSupplier<T> extends Supplier<T> {
		@Override
		default T get() {
			try {
				return getAndThrow();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		T getAndThrow() throws Exception;
	}

}
