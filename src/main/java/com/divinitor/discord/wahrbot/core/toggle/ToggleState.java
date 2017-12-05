package com.divinitor.discord.wahrbot.core.toggle;

public enum ToggleState {
    ON,
    OFF,
    UNSET;

    public ToggleState effective(ToggleState parent) {
        if (this == UNSET) {
            return parent;
        }

        return this;
    }
}
