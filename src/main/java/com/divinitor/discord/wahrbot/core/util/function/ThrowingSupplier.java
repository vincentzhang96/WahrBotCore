package com.divinitor.discord.wahrbot.core.util.function;

import lombok.Lombok;

import java.util.function.Supplier;

import static lombok.Lombok.*;

public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;

    static <T> Supplier<T> supplier(ThrowingSupplier<T, ?> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                throw sneakyThrow(t);
            }
        };
    }
}
