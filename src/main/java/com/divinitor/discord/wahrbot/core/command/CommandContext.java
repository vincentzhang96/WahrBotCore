package com.divinitor.discord.wahrbot.core.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public class CommandContext {


    public static CommandContext from(PrivateMessageReceivedEvent event) {
        //  TODO
        return new CommandContext();
    }

    public static CommandContext from(GuildMessageReceivedEvent event) {
        //  TODO
        return new CommandContext();
    }

}
