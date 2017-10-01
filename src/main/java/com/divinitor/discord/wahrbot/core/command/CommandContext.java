package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import java.util.UUID;

@Getter
public class CommandContext {

    private WahrBot bot;
    private Channel channel;
    private CommandContext parent;
    private String fullCmdline;


    private MessageReceivedEvent event;


    public static CommandContext from(PrivateMessageReceivedEvent event) {
        //  TODO
        return new CommandContext();
    }

    public static CommandContext from(GuildMessageReceivedEvent event) {
        //  TODO
        return new CommandContext();
    }

}
