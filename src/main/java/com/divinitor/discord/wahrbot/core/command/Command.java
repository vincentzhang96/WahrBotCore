package com.divinitor.discord.wahrbot.core.command;

public interface Command {

    CommandResult invoke(CommandContext context);

    default CommandConstraint<CommandContext> getUserPermissionConstraints() {
        return CommandConstraints.allow();
    }

    default CommandConstraint<CommandContext> getBotPermissionConstraints() {
        return CommandConstraints.allow();
    }

    default CommandConstraint<CommandContext> getOtherConstraints() {
        return CommandConstraints.allow();
    }
}
