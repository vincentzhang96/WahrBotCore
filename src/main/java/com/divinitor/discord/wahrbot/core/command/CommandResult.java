package com.divinitor.discord.wahrbot.core.command;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommandResult {

    private ResultType type;



    public enum ResultType {
        /**
         * Command execution was successful.
         */
        OK,

        /**
         * Command execution was rejected (bad argument, etc)
         */
        REJECTED,

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

    public static CommandResult ok() {
        return new CommandResult(ResultType.OK);
    }

    public static CommandResult rejected() {
        return new CommandResult(ResultType.REJECTED);
    }

    public static CommandResult error() {
        return new CommandResult(ResultType.ERROR);
    }

    public static CommandResult noPerm() {
        return new CommandResult(ResultType.NO_PERM);
    }

    public static CommandResult noBotPerm() {
        return new CommandResult(ResultType.NO_BOT_PERM);
    }

    public static CommandResult noSuchCommand() {
        return new CommandResult(ResultType.NO_SUCH_COMMAND);
    }

    public static CommandResult handled() {
        return new CommandResult(ResultType.HANDLED);
    }
}
