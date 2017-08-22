package com.divinitor.discord.wahrbot.core.util;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.google.inject.AbstractModule;
import net.dv8tion.jda.core.JDA;

public class WahrBotModule extends AbstractModule {

    private final WahrBot bot;

    public WahrBotModule(WahrBot bot) {
        this.bot = bot;
    }

    @Override
    protected void configure() {
        bind(WahrBot.class).toInstance(bot);
        bind(BotConfig.class).toProvider(bot::getConfig);
        bind(JDA.class).toProvider(bot::getApiClient);
    }
}
