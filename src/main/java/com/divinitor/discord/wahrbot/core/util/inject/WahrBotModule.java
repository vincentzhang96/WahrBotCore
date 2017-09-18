package com.divinitor.discord.wahrbot.core.util.inject;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.module.ModuleManager;
import com.divinitor.discord.wahrbot.core.util.SQLConnectionProvider;
import com.google.inject.AbstractModule;
import net.dv8tion.jda.core.JDA;
import redis.clients.jedis.Jedis;

public class WahrBotModule extends AbstractModule {

    private final WahrBot bot;

    public WahrBotModule(WahrBot bot) {
        this.bot = bot;
    }

    @Override
    protected void configure() {
        bind(WahrBot.class).toInstance(this.bot);
        bind(BotConfig.class).toProvider(this.bot::getConfig);
        bind(JDA.class).toProvider(this.bot::getApiClient);
        bind(Jedis.class).toProvider(this.bot.getJedisPool()::getResource);
        bind(JedisProvider.class).toInstance(new JedisProvider(this.bot.getJedisPool()::getResource));
        //  A bit of a misrepresentation, since the provider is a singleton, but the Connections are not.
        bind(SQLConnectionProvider.class).toInstance(this.bot.getDataSource()::getConnection);

        bind(ModuleManager.class).toProvider(this.bot::getModuleManager);
        bind(DynConfigStore.class).toProvider(this.bot::getDynConfigStore);
    }


}
