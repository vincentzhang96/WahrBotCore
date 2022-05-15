package com.divinitor.discord.wahrbot.core.command;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface CommandDispatcher {

    @Subscribe
    default void handleMessage(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            this.handlePrivateMessage(event);
        } else if (event.isFromGuild()) {
            this.handleServerMessage(event);
        }
    }

    void handlePrivateMessage(MessageReceivedEvent event);

    void handleServerMessage(MessageReceivedEvent event);

    CommandRegistry getRootRegistry();
}
