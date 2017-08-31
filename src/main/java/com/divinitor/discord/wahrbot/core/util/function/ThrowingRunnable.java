package com.divinitor.discord.wahrbot.core.util.function;

import lombok.Lombok;

import static lombok.Lombok.sneakyThrow;

public interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;

    static Runnable runnable(ThrowingRunnable<?> r) {
        return () -> {
            try {
                r.run();
            } catch (Throwable e) {
                throw sneakyThrow(e);
            }
        };
    }
}
