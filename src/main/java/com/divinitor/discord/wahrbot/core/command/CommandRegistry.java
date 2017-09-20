package com.divinitor.discord.wahrbot.core.command;

public interface CommandRegistry {

    CommandResult execute(String commandLine, CommandContext context);



}
