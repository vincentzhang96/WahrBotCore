package com.divinitor.discord.wahrbot.core.util.function;

import static lombok.Lombok.sneakyThrow;

/**
 * A Runnable that can throw an exception.
 * @param <E> The exception type
 */
public interface ThrowingRunnable<E extends Throwable> {
    /**
     * Executes something, possibly throwing an exception
     * @throws E If an error occurred
     */
    void run() throws E;

    /**
     * Wrap a ThrowingRunnable into a regular Runnable that can {@link lombok.Lombok#sneakyThrow(Throwable)}
     * @param r The throwing runnable to wrap
     * @return A Runnable that can SneakyThrow
     */
    static Runnable runnable(ThrowingRunnable<?> r) {
        return () -> {
            try {
                r.run();
            } catch (Throwable e) {
                //noinspection ConstantConditions
                throw sneakyThrow(e);
            }
        };
    }
}
