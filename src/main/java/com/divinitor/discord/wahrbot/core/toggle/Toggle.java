package com.divinitor.discord.wahrbot.core.toggle;

public interface Toggle {

    Toggle getParent();

    ToggleState getState();

    default ToggleState getEffectiveState() {
        return this.getState().effective(this.getParent().getEffectiveState());
    }

    default boolean use() {
        return this.getEffectiveState() == ToggleState.ON;
    }

    void setState(ToggleState state);

    ToggleRegistry getRegistry();
}
