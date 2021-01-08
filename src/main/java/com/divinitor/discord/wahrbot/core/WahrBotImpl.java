package com.divinitor.discord.wahrbot.core;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcherImpl;
import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.divinitor.discord.wahrbot.core.config.RedisCredentials;
import com.divinitor.discord.wahrbot.core.config.SQLCredentials;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.config.dyn.impl.CachingDynConfigStore;
import com.divinitor.discord.wahrbot.core.config.dyn.impl.RedisDynConfigStore;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.LocalizerImpl;
import com.divinitor.discord.wahrbot.core.module.ModuleManager;
import com.divinitor.discord.wahrbot.core.module.ModuleManagerImpl;
import com.divinitor.discord.wahrbot.core.service.ServiceBus;
import com.divinitor.discord.wahrbot.core.service.impl.ServiceBusImpl;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.UserStorage;
import com.divinitor.discord.wahrbot.core.store.impl.ServerStorageImpl;
import com.divinitor.discord.wahrbot.core.store.impl.UserStorageImpl;
import com.divinitor.discord.wahrbot.core.toggle.ToggleRegistry;
import com.divinitor.discord.wahrbot.core.toggle.impl.MemoryTransientToggleRegistry;
import com.divinitor.discord.wahrbot.core.toggle.impl.WeakToggleRegistryProxy;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.divinitor.discord.wahrbot.core.util.inject.WahrBotModule;
import com.divinitor.discord.wahrbot.core.util.metrics.EventBusMetricSet;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WahrBotImpl implements WahrBot {

    /**
     * Timeout before SQL connection fails
     */
    private static final int DATA_TIMEOUT_MS = 2000;

    /**
     * Main entry point
     * @param args Command line args
     */
    public static void main(String[] args) {
        //  Create bot and launch
        WahrBot bot = new WahrBotImpl();
        bot.run();
    }

    /**
     * The logger instance
     */
    private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The dependency injector
     */
    @Getter
    private Injector injector;

    /**
     * The bot config
     */
    @Getter
    private BotConfig config;

    /**
     * The bot's working directory
     */
    @Getter
    private final Path botDir;

    /**
     * The JDA API client
     */
    @Getter
    private JDA apiClient;

    /**
     * The SQL connection manager
     */
    @Getter
    private HikariDataSource dataSource;

    /**
     * The Redis connection pool
     */
    @Getter
    private JedisPool jedisPool;

    /**
     * The bot's JDA event listener
     */
    @Getter
    private BotEventDispatcher eventListener;

    /**
     * The event bus
     */
    @Getter
    private AsyncEventBus eventBus;

    /**
     * The executor service
     */
    @Getter
    private ScheduledExecutorService executorService;

    /**
     * The root metric registry
     */
    @Getter
    private MetricRegistry metrics;

    /**
     * The metric set for the event bus
     */
    private EventBusMetricSet eventBusMetricSet;

    /**
     * The metric reporter (currently unused)
     */
    private final Reporter reporter;

    /**
     * The module manager
     */
    @Getter
    private ModuleManager moduleManager;

    /**
     * The dynamic configuration store
     */
    @Getter
    private DynConfigStore dynConfigStore;

    /**
     * The service bus
     */
    @Getter
    private ServiceBus serviceBus;

    /**
     * The localizer
     */
    @Getter
    private Localizer localizer;

    /**
     * The root command dispatcher
     */
    @Getter
    private CommandDispatcher commandDispatcher;

    /**
     * User information store
     */
    @Getter
    private UserStorage userStorage;

    /**
     * Storage information store
     */
    @Getter
    private ServerStorage serverStorage;

    /**
     * The toggle registry
     */
    @Getter
    private WeakToggleRegistryProxy toggleRegistry;

    /**
     * A transient (non-permanent) toggle registry to use as a fallback if no toggle registry implementation is
     * registered
     */
    private MemoryTransientToggleRegistry backupRegistry;

    /**
     * The time that the bot started
     */
    @Getter
    private Instant startTime;

    /**
     * Constructor
     */
    public WahrBotImpl() {
        //  Get the bot's working directory. If the environment variable "com.divinitor.discord.wahrbot.home" is set,
        //  try using that. If not, use the current directory
        this.botDir = Paths.get(
                System.getProperty("com.divinitor.discord.wahrbot.home", ""))
                .toAbsolutePath();
        //  Add a shutdown hook to clean up
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

        //  Create an executor with a thread pool with minimum as many threads as there are available processors on
        //  the machine
        this.executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

        //  Localizer
        this.localizer = new LocalizerImpl();

        //  Event bus, using our executor service, and handling errors through our handler
        this.eventBus = new AsyncEventBus(this.executorService, this::handleEventBusException);
        //  Event bus metrics
        this.eventBusMetricSet = new EventBusMetricSet(eventBus);

        //  Metric registry
        this.metrics = new MetricRegistry();
        //  Register our event bus metrics
        this.metrics.registerAll(this.eventBusMetricSet);

        //  TEMPORARY: Report our metrics to the console every minute
        ConsoleReporter re = ConsoleReporter.forRegistry(this.metrics)
            .convertDurationsTo(TimeUnit.SECONDS)
            .convertRatesTo(TimeUnit.SECONDS)
            .build();
        re.start(1, TimeUnit.MINUTES);
        this.reporter = re;

        //  Service bus
        this.serviceBus = new ServiceBusImpl();
    }

    /**
     * Handles unhandled exceptions during event handling. The exeception is logged and the failure recorded in metrics
     * @param exception The exception that was thrown
     * @param context The context in which the exception was thrown
     */
    private void handleEventBusException(Throwable exception, SubscriberExceptionContext context) {
        this.eventBusMetricSet.incrEventExceptionCount();
        String err = String.format("Exception while bussing an event %3$s to subscriber %1$s with listener %2$s",
            context.getSubscriber().getClass().toString(),  //  Throwing class
            context.getSubscriberMethod().toGenericString(),    //  Throwing method
            context.getEvent().getClass().toString());  //  Event class
        this.LOGGER.error(err, exception);
    }

    @Override
    public void run() {
        //  Initialize, load modules, and then start doing work
        this.init();
        this.loadModules();
        this.startBot();
    }

    /**
     * Initializes the bot. The configuration is loaded, databases connected to, and services started.
     */
    private void init() {
        LOGGER.info("Starting {}...", this.getApplicationName());
        this.startTime = Instant.now();

        //  Use no-op toggle
        this.toggleRegistry = new WeakToggleRegistryProxy(null);

        //  Load config, init core services, and connect to DBs/KVSs
        Gson gson = StandardGson.pretty();
        try (BufferedReader reader = Files.newBufferedReader(this.botDir.resolve("config.json"), UTF_8)) {
            this.config = gson.fromJson(reader, BotConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }

        LOGGER.info("Instance: {}", this.config.getInstanceName());

        //  Init SQL connection
        SQLCredentials sqlCredentials = this.config.getSqlCredentials();

        try {
            String sqlUrl = String.format("jdbc:postgresql://%s:%d/%s",
                    sqlCredentials.getHost(),
                    sqlCredentials.getPort(),
                    sqlCredentials.getDatabase());
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(sqlUrl);
            hikariConfig.setUsername(sqlCredentials.getUsername());
            hikariConfig.setPassword(sqlCredentials.getPassword());
            hikariConfig.setConnectionTimeout(DATA_TIMEOUT_MS);
            this.dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            //  TODO We don't currently use this so make this a soft error, but we should figure out something
//            throw new RuntimeException("Unable to connect to SQL server", e);
        }

        //  Init Redis connection
        RedisCredentials redisCredentials = this.config.getRedis();
        GenericObjectPoolConfig objectPoolConfig = new GenericObjectPoolConfig();
        this.jedisPool = new JedisPool(
                objectPoolConfig,
                redisCredentials.getHost(),
                redisCredentials.getPort(),
                DATA_TIMEOUT_MS,
                redisCredentials.getPassword(),
                redisCredentials.getDatabase());

        //  Test the connection
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.ping();
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to Redis server", e);
        }

        //  Set up DI
        this.injector = Guice.createInjector(new WahrBotModule(this));

        //  Start services

        //  event listener
        this.eventListener = this.injector.getInstance(BotEventDispatcher.class);
        this.serviceBus.registerService(eventListener);
        //  server and user data storage
        this.serverStorage = this.injector.getInstance(ServerStorageImpl.class);
        this.userStorage = this.injector.getInstance(UserStorageImpl.class);
        //  dynconfig
        RedisDynConfigStore rdcs = new RedisDynConfigStore();
        this.injector.injectMembers(rdcs);
        this.dynConfigStore = new CachingDynConfigStore(rdcs);
        this.serviceBus.registerService(DynConfigStore.class, this.dynConfigStore);
        //  command dispatch
        this.commandDispatcher = new CommandDispatcherImpl(this);
        this.injector.injectMembers(this.commandDispatcher);
        this.eventBus.register(this.commandDispatcher);
        this.serviceBus.registerService(CommandDispatcher.class, this.commandDispatcher);
    }

    private void loadModules() {
        //  Load modules
        this.moduleManager = new ModuleManagerImpl(this);
        this.injector.injectMembers(this.moduleManager);

        //  Load the modules from the modules.json file
        this.moduleManager.loadLatestModulesFromList();

        //  A module should have registered a ToggleRegistry, but if not we'll use a temporary one
        if (this.toggleRegistry.getRegistry().get() == null) {
            LOGGER.warn("No toggle registry implementation was set by a module. " +
                "Using an in-memory transient toggle registry instead.");
            this.backupRegistry = new MemoryTransientToggleRegistry();
            this.toggleRegistry.setBaseRegistry(this.backupRegistry);
        }
    }

    /**
     * Connect to Discord and begin execution
     */
    private void startBot() {
        //  Connect to Discord and begin general execution
        LOGGER.info("Connecting to Discord...");

        try {
            this.apiClient = JDABuilder.createDefault(this.getConfig().getDiscord().getToken())
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setAutoReconnect(true)
                    .setEventManager(new InterfacedEventManager())
                    .addEventListeners(this.getEventListener())
                    .build();
        } catch (LoginException e) {
            throw new RuntimeException("Invalid token", e);
        }

        LOGGER.info("Connected to Discord as {}#{} ({})",
            apiClient.getSelfUser().getName(),
            apiClient.getSelfUser().getDiscriminator(),
            SnowflakeUtils.encode(apiClient.getSelfUser().getIdLong()));

        LOGGER.info("Connected to {} servers with {} channels and {} unique users",
            String.format("%,d", apiClient.getGuilds().size()),
            String.format("%,d", apiClient.getTextChannels().size()),
            String.format("%,d", apiClient.getUsers().size()));
    }

    /**
     * Handles cleanup on shutdown
     */
    private void onShutdown() {
        LOGGER.info("Shutting down {}", this.getApplicationName());
        Map<String, Exception> shutdownExceptions = new HashMap<>();

        //  Shut down services
        try {
            if (this.serviceBus != null) {
                this.serviceBus.unregisterAll();
            }
        } catch (Exception e) {
            shutdownExceptions.put("service", e);
        }

        //  Shut down modules
        try {
            if (this.moduleManager != null) {
                this.moduleManager.saveCurrentModuleList();
                this.moduleManager.unloadAll();
            }
        } catch (Exception e) {
            shutdownExceptions.put("module", e);
        }

        //  Shut down API client
        try {
            if (this.apiClient != null) {
                this.apiClient.shutdownNow();
            }
        } catch (Exception e) {
            shutdownExceptions.put("discordapi", e);
        }

        //  Shut down SQL connection
        try {
            if (this.dataSource != null) {
                this.dataSource.close();
            }
        } catch (Exception e) {
            shutdownExceptions.put("sql", e);
        }

        //  Shut down redis connection
        try {
            this.jedisPool.close();
        } catch (Exception e) {
            shutdownExceptions.put("redis", e);
        }

        //  If we got no errors, we're good, if we did then warn the user but that's all we can really do
        if (shutdownExceptions.isEmpty()) {
            LOGGER.info("Shutdown successful");
        } else {
            LOGGER.warn("One or more exceptions during shutdown. Shutdown of {} may not be clean!",
                    this.getApplicationName());
            shutdownExceptions.forEach(LOGGER::warn);
        }
    }

    @Override
    public void setToggleRegistry(ToggleRegistry registry) {
        this.toggleRegistry.setBaseRegistry(toggleRegistry);
    }

    @Override
    public String getApplicationName() {
        return "WahrBot";
    }

    @Override
    public void shutdown() {
        //  The management script expects a 0 exit code for clean exit
        System.exit(0);
    }

    @Override
    public void restart() {
        //  The management script will re-launch the bot if it gets an exit code of 20
        System.exit(20);
    }
}
