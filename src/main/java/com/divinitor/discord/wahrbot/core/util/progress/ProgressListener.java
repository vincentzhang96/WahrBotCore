package com.divinitor.discord.wahrbot.core.util.progress;

/**
 * A functional interface for listening for progress percent and status.
 */
@FunctionalInterface
public interface ProgressListener {

    /**
     * Indicates that the current operation does not have a measurable completion percent
     */
    int INDETERMINATE = -1;

    /**
     * Notify of a progress update
     * @param percent The percent completed, or {@link ProgressListener#INDETERMINATE} for indeterminate status
     * @param status The current operation status text
     */
    void update(int percent, String status);

}
