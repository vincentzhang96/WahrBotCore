package com.divinitor.discord.wahrbot.core.util.progress;

@FunctionalInterface
public interface ProgressListener {

    void update(int percent, String status);

}
