package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public class CommandDispatch {

    private WahrBot bot;
    private final CommandRegistry rootRegistry;

    public CommandDispatch(WahrBot bot) {
        this.bot = bot;
        rootRegistry = null;
    }

    @Subscribe
    public void handlePrivateMessage(PrivateMessageReceivedEvent event) {
        CommandContext commandContext = CommandContext.from(event);


    }

    @Subscribe
    public void handleServerMessage(GuildMessageReceivedEvent event) {
        CommandContext commandContext = CommandContext.from(event);

    }


}
