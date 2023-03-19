package de.tsl2.nano.modelkit;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * @author scne522
 */
public class ExceptionHandler {
    private ExceptionHandler() {
    }

    @SuppressWarnings("unchecked")
    public static final <T> T trY(final SupplierEx<T> s, final Class<? extends Exception>... handleExceptions) {
        try {
            return s.get();
        } catch (final Exception e) {
            if (!Arrays.asList(handleExceptions).contains(cause(e).getClass())) {
                return (T) forward(e);
            } else {
                return null;
            }
        }
    }

    public static Object forward(final Exception e) {
        throw (RuntimeException) (e instanceof RuntimeException ? e : new RuntimeException(e));
    }

    private static Throwable cause(final Throwable e) {
        if (e.getCause() != null) {
            return cause(e.getCause());
        }
        return e;
    }

    @FunctionalInterface
    public interface SupplierEx<T> extends Supplier<T> {
        T getEx() throws Exception;

        @Override
        default T get() {
            try {
                return getEx();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
