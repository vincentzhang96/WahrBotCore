package com.divinitor.discord.wahrbot.core.command;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public interface CommandDispatcher {

    @Subscribe
    void handlePrivateMessage(PrivateMessageReceivedEvent event);

    @Subscribe
    void handleServerMessage(GuildMessageReceivedEvent event);

    CommandRegistry getRootRegistry();
}
