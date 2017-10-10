package com.divinitor.discord.wahrbot.core.i18n;

import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface LocalizerPluralRule extends Predicate<Number> {

    default LocalizerPluralRule and(LocalizerPluralRule other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    default LocalizerPluralRule negate() {
        return t -> !test(t);
    }

    static LocalizerPluralRule TRUE() {
        return number -> true;
    }
}
