package com.divinitor.discord.wahrbot.core;

import com.codahale.metrics.MetricRegistry;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.module.ModuleManager;
import com.divinitor.discord.wahrbot.core.service.ServiceBus;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.UserStorage;
import com.google.common.eventbus.AsyncEventBus;
import com.google.inject.Injector;
import net.dv8tion.jda.core.JDA;
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;

public interface WahrBot {
    void run();

    String getApplicationName();

    Injector getInjector();

    BotConfig getConfig();

    Path getBotDir();

    JDA getApiClient();

    ModuleManager getModuleManager();

    DataSource getDataSource();

    DynConfigStore getDynConfigStore();

    JedisPool getJedisPool();

    AsyncEventBus getEventBus();

    ScheduledExecutorService getExecutorService();

    MetricRegistry getMetrics();

    ServiceBus getServiceBus();

    Localizer getLocalizer();

    CommandDispatcher getCommandDispatcher();

    ServerStorage getServerStorage();

    UserStorage getUserStorage();

    void shutdown();

    void restart();
}
