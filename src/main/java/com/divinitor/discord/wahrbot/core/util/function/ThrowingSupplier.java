package com.divinitor.discord.wahrbot.core.util.function;

import java.util.function.Supplier;

import static lombok.Lombok.sneakyThrow;

/**
 * A Supplier that can throw an exception.
 * @param <T> The return type
 * @param <E> The exception type
 */
public interface ThrowingSupplier<T, E extends Throwable> {

    /**
     * Get a value, possibly throwing an exception
     * @return A value
     * @throws E if an error occurred
     */
    T get() throws E;

    /**
     * Wrap a ThrowingSupplier into a regular Supplier that can {@link lombok.Lombok#sneakyThrow(Throwable)}
     * @param supplier The throwing supplier to wrap
     * @return A Supplier that can SneakyThrow
     */
    static <T> Supplier<T> supplier(ThrowingSupplier<T, ?> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                //noinspection ConstantConditions
                throw sneakyThrow(t);
            }
        };
    }
}
