package com.divinitor.discord.wahrbot.core.config.dyn;

public interface DoubleDynConfigHandle {

    double getDouble();

    default float getFloat() {
        return (float) getDouble();
    }

}
