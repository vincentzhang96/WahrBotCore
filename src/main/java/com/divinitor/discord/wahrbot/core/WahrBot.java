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
import com.divinitor.discord.wahrbot.core.toggle.ToggleRegistry;
import com.google.common.eventbus.AsyncEventBus;
import com.google.inject.Injector;
import net.dv8tion.jda.api.JDA;
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main bot interface
 */
public interface WahrBot {

    /**
     * Run the bot
     */
    void run();

    /**
     * Gets the name of this application
     * @return The name of this application
     */
    String getApplicationName();

    /**
     * Get the primary injector
     * @return The primary dependency injector
     */
    Injector getInjector();

    /**
     * Get the bot config
     * @return The bot's config
     */
    BotConfig getConfig();

    /**
     * Get the working directory for the bot
     * @return The working directory
     */
    Path getBotDir();

    /**
     * Get the JDA API client instance. Interact with Discord through this instance.
     * @return The API client instance
     */
    JDA getApiClient();

    /**
     * Get the bot's module manager. All modules are managed through this instance.
     * @see ModuleManager
     * @return The module manager
     */
    ModuleManager getModuleManager();

    /**
     * Get the bot's data source (SQL database connection manager)
     * @return The data source
     */
    DataSource getDataSource();

    /**
     * Get the bot's dynamic config store
     * @see DynConfigStore
     * @return The bot's dynamic config store
     */
    DynConfigStore getDynConfigStore();

    /**
     * Get the bot's Redis connection pool
     * @return The Redis connection pool
     */
    JedisPool getJedisPool();

    /**
     * Get the bot's event bus. All events are broadcasted on this Event Bus
     * @return The event bus
     */
    AsyncEventBus getEventBus();

    /**
     * Get the bot's common executor service
     * @return The executor service
     */
    ScheduledExecutorService getExecutorService();

    /**
     * Get the bot's root metric registry
     * @return The root metric registry
     */
    MetricRegistry getMetrics();

    /**
     * Get the bot's service bus. All services should be registered and accessed via this bus.
     * @see ServiceBus
     * @return The service bus
     */
    ServiceBus getServiceBus();

    /**
     * Get the bot's localizer. All localization should occur through this instance, and all locale modules should be
     * registered and unregistered from this instance.
     * @see Localizer
     * @return The bot's localizer
     */
    Localizer getLocalizer();

    /**
     * The primary command dispatcher. Commands are registerd and Discord messages are processed through this dispatcher
     * @see CommandDispatcher
     * @return The primary command dispatcher
     */
    CommandDispatcher getCommandDispatcher();

    /**
     * Get the server information storage. Discord server/guild-specific information should be stored here, as well
     * as server-member specific information (information specific to a user within a guild, not for user data global
     * to Discord)
     * @return The server information storage
     */
    ServerStorage getServerStorage();

    /**
     * Get the user information storage. Discord user-specific information should be stored here. For user information
     * that's specific to a server/guild,
     * @return The user information storage
     */
    UserStorage getUserStorage();

    /**
     * Get the feature toggle registry. Features that can be turned on and off should check for a toggle from this
     * registry.
     * @return The toggle registry
     */
    ToggleRegistry getToggleRegistry();

    /**
     * Set the toggle registry instance to use
     * @param registry The registry to use
     */
    void setToggleRegistry(ToggleRegistry registry);

    /**
     * Stop the bot
     */
    void shutdown();

    /**
     * Restart the bot
     */
    void restart();

    /**
     * The instant the bot was started
     * @return The instant the bot was started
     */
    Instant getStartTime();
}
