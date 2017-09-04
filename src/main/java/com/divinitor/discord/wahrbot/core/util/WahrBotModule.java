package com.divinitor.discord.wahrbot.core.util;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.WahrBotImpl;
import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.google.inject.AbstractModule;
import net.dv8tion.jda.core.JDA;
import redis.clients.jedis.Jedis;

public class WahrBotModule extends AbstractModule {

    private final WahrBotImpl bot;

    public WahrBotModule(WahrBotImpl bot) {
        this.bot = bot;
    }

    @Override
    protected void configure() {
        bind(WahrBot.class).toInstance(bot);
        bind(BotConfig.class).toProvider(bot::getConfig);
        bind(JDA.class).toProvider(bot::getApiClient);
        bind(Jedis.class).toProvider(bot.getJedisPool()::getResource);
        //  A bit of a misrepresentation, since the provider is a singleton, but the Connections are not.
        bind(SQLConnectionProvider.class).toInstance(bot.getDataSource()::getConnection);
    }


}
