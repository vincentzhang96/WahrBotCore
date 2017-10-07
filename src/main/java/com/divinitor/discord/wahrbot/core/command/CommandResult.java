package com.divinitor.discord.wahrbot.core.command;

public enum CommandResult {
    /**
     * Command execution was successful.
     */
    OK,

    /**
     * Command execution failed due to an unexpected error.
     */
    ERROR,

    /**
     * Command execution failed because the user does not have permission.
     */
    NO_PERM,

    /**
     * Command execution failed because the bot does not have permission.
     */
    NO_BOT_PERM,

    /**
     * Command execution failed because no such command exists to handle it.
     */
    NO_SUCH_COMMAND,

    /**
     * The response was handled. Used to prevent upstream propagation when necessary.
     */
    HANDLED
}
