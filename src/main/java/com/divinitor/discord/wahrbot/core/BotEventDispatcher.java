package com.divinitor.discord.wahrbot.core;

import com.google.inject.Inject;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Core event dispatcher for the bot. This listens for all events from JDA and then dispatches them accordingly
 * to the proper event queues.
 */
public class BotEventDispatcher extends ListenerAdapter {

    private final WahrBot bot;

    @Inject
    public BotEventDispatcher(WahrBot bot) {
        this.bot = bot;
    }

    @Override
    public void onGenericEvent(Event event) {
        super.onGenericEvent(event);

        //  Dispatch
        bot.getEventBus().post(event);
    }
}
