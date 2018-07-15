package com.divinitor.discord.wahrbot.core.util.function;

import java.util.function.Consumer;

import static lombok.Lombok.sneakyThrow;

/**
 * A consumer that can throw an exception
 * @param <C> The input type
 * @param <E> The exception type
 */
public interface ThrowingConsumer<C, E extends Throwable> {

    /**
     * Accept a value, possibly throwing an exception
     * @param c The input
     * @throws E If an error occurred
     */
    void accept(C c) throws E;

    /**
     * Wrap a ThrowingConsumer into a regular Consumer that can {@link lombok.Lombok#sneakyThrow(Throwable)}
     * @param tc The throwing consumer to wrap
     * @param <C> The input type
     * @return A Consumer that can SneakyThrow
     */
    static <C> Consumer<C> consumer(ThrowingConsumer<C, ?> tc) {
        return (c) -> {
            try {
                tc.accept(c);
            } catch (Throwable throwable) {
                //noinspection ConstantConditions
                throw sneakyThrow(throwable);
            }
        };
    }
}
