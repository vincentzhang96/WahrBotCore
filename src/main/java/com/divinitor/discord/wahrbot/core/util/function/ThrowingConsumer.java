package com.divinitor.discord.wahrbot.core.util.function;

import java.util.function.Consumer;

import static lombok.Lombok.sneakyThrow;

public interface ThrowingConsumer<C, E extends Throwable> {

    void accept(C c) throws E;

    static <C> Consumer<C> consumer(ThrowingConsumer<C, ?> tc) {
        return (c) -> {
            try {
                tc.accept(c);
            } catch (Throwable throwable) {
                throw sneakyThrow(throwable);
            }
        };
    }
}
