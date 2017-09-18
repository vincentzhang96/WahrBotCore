package com.divinitor.discord.wahrbot.core.config.dyn;

public interface LongDynConfigHandle {

    long getLong();

    default int getInt() {
        return (int) getLong();
    }

}
