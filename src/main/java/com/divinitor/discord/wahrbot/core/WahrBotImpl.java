package com.divinitor.discord.wahrbot.core;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
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
import com.divinitor.discord.wahrbot.core.util.inject.WahrBotModule;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.divinitor.discord.wahrbot.core.util.logging.SimpleLogRedirect;
import com.divinitor.discord.wahrbot.core.util.metrics.EventBusMetricSet;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WahrBotImpl implements WahrBot {

    private static final int DATA_TIMEOUT_MS = 2000;

    public static void main(String[] args) {
        WahrBot bot = new WahrBotImpl();
        bot.run();
    }

    private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    private Injector injector;

    @Getter
    private BotConfig config;

    @Getter
    private final Path botDir;

    @Getter
    private JDA apiClient;

    @Getter
    private HikariDataSource dataSource;

    @Getter
    private JedisPool jedisPool;

    @Getter
    private BotEventDispatcher eventListener;

    @Getter
    private AsyncEventBus eventBus;

    @Getter
    private ScheduledExecutorService executorService;

    @Getter
    private MetricRegistry metrics;

    private EventBusMetricSet eventBusMetricSet;
    private final Reporter reporter;

    @Getter
    private ModuleManager moduleManager;

    @Getter
    private DynConfigStore dynConfigStore;

    @Getter
    private ServiceBus serviceBus;

    @Getter
    private Localizer localizer;

    public WahrBotImpl() {
        this.botDir = Paths.get(
                System.getProperty("com.divinitor.discord.wahrbot.home", ""))
                .toAbsolutePath();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

        this.executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.localizer = new LocalizerImpl();

        this.eventBus = new AsyncEventBus(this.executorService, this::handleEventBusException);
        this.eventBusMetricSet = new EventBusMetricSet(eventBus);

        this.metrics = new MetricRegistry();
        this.metrics.registerAll(this.eventBusMetricSet);

        ConsoleReporter re = ConsoleReporter.forRegistry(this.metrics)
            .convertDurationsTo(TimeUnit.SECONDS)
            .convertRatesTo(TimeUnit.SECONDS)
            .build();
        re.start(1, TimeUnit.MINUTES);
        this.reporter = re;
        this.serviceBus = new ServiceBusImpl();
    }

    private void handleEventBusException(Throwable exception, SubscriberExceptionContext context) {
        this.eventBusMetricSet.incrEventExceptionCount();
        String err = String.format("Exception while bussing an event %3$s to subscriber %1$s with listener %2$s",
            context.getSubscriber().getClass().toString(),
            context.getSubscriberMethod().toGenericString(),
            context.getEvent().getClass().toString());
        this.LOGGER.error(err, exception);
    }

    @Override
    public void run() {
        this.init();
        this.loadModules();
        this.startBot();
    }

    private void init() {
        LOGGER.info("Starting {}...", this.getApplicationName());

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
            throw new RuntimeException("Unable to connect to SQL server", e);
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

        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.ping();
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to Redis server", e);
        }

        //  Set up DI
        this.injector = Guice.createInjector(new WahrBotModule(this));

        //  Start services
        this.eventListener = this.injector.getInstance(BotEventDispatcher.class);
        this.serviceBus.registerService(eventListener);

        RedisDynConfigStore rdcs = new RedisDynConfigStore();
        this.injector.injectMembers(rdcs);
        this.dynConfigStore = new CachingDynConfigStore(rdcs);
        this.serviceBus.registerService(DynConfigStore.class, this.dynConfigStore);
    }

    private void loadModules() {
        //  Load modules
        this.moduleManager = new ModuleManagerImpl(this);
        this.injector.injectMembers(this.moduleManager);

        this.moduleManager.loadLatestModulesFromList();
    }

    private void startBot() {
        //  Connect to Discord and begin general execution
        LOGGER.info("Connecting to Discord...");
        SimpleLogRedirect.addListener();

        while (true) {
            try {
                this.apiClient = new JDABuilder(AccountType.BOT)
                    .setToken(this.getConfig().getDiscord().getToken())
                    .setAutoReconnect(true)
                    .setEventManager(new InterfacedEventManager())
                    .addEventListener(this.getEventListener())
                    .buildBlocking();
                break;
            } catch (LoginException e) {
                LOGGER.error("Bad Discord token", e);
            } catch (RateLimitedException e) {
                long retryAfter = e.getRetryAfter();
                LOGGER.warn("Login was rate limited, retrying after {}s...",
                    retryAfter);
                try {
                    Thread.sleep(Duration.ofSeconds(retryAfter).toMillis());
                } catch (InterruptedException ignored) {
                    //  Ignore
                }
            } catch (InterruptedException ignored) {
                //  Ignore
            }
        }

        LOGGER.info("Connected to Discord as {}#{}",
            apiClient.getSelfUser().getName(),
            apiClient.getSelfUser().getDiscriminator());

        LOGGER.info("Connected to {} servers with {} channels and {} unique users",
            String.format("%,d", apiClient.getGuilds().size()),
            String.format("%,d", apiClient.getTextChannels().size()),
            String.format("%,d", apiClient.getUsers().size()));
    }

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

        if (shutdownExceptions.isEmpty()) {
            LOGGER.info("Shutdown successful");
        } else {
            LOGGER.warn("One or more exceptions during shutdown. Shutdown of {} may not be clean!",
                    this.getApplicationName());
            shutdownExceptions.forEach(LOGGER::warn);
        }
    }

    @Override
    public String getApplicationName() {
        return "WahrBot";
    }

    @Override
    public void shutdown() {
        System.exit(0);
    }

    @Override
    public void restart() {
        System.exit(20);
    }
}
