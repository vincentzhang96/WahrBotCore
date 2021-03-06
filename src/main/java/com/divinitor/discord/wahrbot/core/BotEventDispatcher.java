package com.divinitor.discord.wahrbot.core;

import com.google.inject.Inject;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Core event dispatcher for the bot. This listens for all events from JDA and then dispatches them accordingly
 * to the proper event queues.
 */
public class BotEventDispatcher extends ListenerAdapter {

    /**
     * Bot instance
     */
    private final WahrBot bot;

    /**
     * Injected constructor
     * @param bot The bot instance
     */
    @Inject
    public BotEventDispatcher(WahrBot bot) {
        this.bot = bot;
    }

    /**
     * We register for the onGenericEvent listener because JDA will send us ALL Discord events, and we'll
     * dispatch events accordingly using the event bus.
     * @param event The Discord event
     */
    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        super.onGenericEvent(event);

        //  Dispatch
        bot.getEventBus().post(event);
    }
}
