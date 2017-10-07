package com.divinitor.discord.wahrbot.core.command;

public interface CommandConstraint<T> {

    boolean check(T context);

    default CommandConstraint<T> and(CommandConstraint<T> b) {
        return and(this, b);
    }

    default CommandConstraint<T> or(CommandConstraint<T> b) {
        return or(this, b);
    }

    default CommandConstraint<T> not() {
        return not(this);
    }

    static <T> CommandConstraint<T> not(CommandConstraint<T> a) {
        return t -> !a.check(t);
    }

    static <T> CommandConstraint<T> and(CommandConstraint<T> a, CommandConstraint<T> b) {
        return t -> a.check(t) && b.check(t);
    }

    static <T> CommandConstraint<T> or(CommandConstraint<T> a, CommandConstraint<T> b) {
        return t -> a.check(t) || b.check(t);
    }
}
