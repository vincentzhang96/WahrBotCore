package com.divinitor.discord.wahrbot.core.util.inject;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.module.ModuleManager;
import com.divinitor.discord.wahrbot.core.service.ServiceBus;
import com.divinitor.discord.wahrbot.core.util.SQLConnectionProvider;
import com.google.inject.AbstractModule;
import net.dv8tion.jda.core.JDA;
import redis.clients.jedis.Jedis;

/**
 * The dependency injection module for WahrBot
 */
public class WahrBotModule extends AbstractModule {

    /**
     * Bot instance
     */
    private final WahrBot bot;

    public WahrBotModule(WahrBot bot) {
        this.bot = bot;
    }

    @Override
    protected void configure() {
        //  Bot
        bind(WahrBot.class).toInstance(this.bot);
        //  Bot config -> bot.getConfig()
        bind(BotConfig.class).toProvider(this.bot::getConfig);
        //  JDA -> bot.getApiClient()
        bind(JDA.class).toProvider(this.bot::getApiClient);
        //  Jedis -> bot.getJedisPool().getResource()
        bind(Jedis.class).toProvider(this.bot.getJedisPool()::getResource);
        //  Jedis provider (deferred loading)
        bind(JedisProvider.class).toInstance(new JedisProvider(this.bot.getJedisPool()::getResource));
        //  A bit of a misrepresentation, since the provider is a singleton, but the Connections are not.
        if (this.bot.getDataSource() != null) {
            bind(SQLConnectionProvider.class).toInstance(this.bot.getDataSource()::getConnection);
        }

        //  Module manager
        bind(ModuleManager.class).toProvider(this.bot::getModuleManager);
        //  Dynamic config storage
        bind(DynConfigStore.class).toProvider(this.bot::getDynConfigStore);
        //  Localizer
        bind(Localizer.class).toProvider(this.bot::getLocalizer);
        //  Root command dispatcher
        bind(CommandDispatcher.class).toProvider(this.bot::getCommandDispatcher);
        //  Service bus
        bind(ServiceBus.class).toProvider(this.bot::getServiceBus);
    }


}
