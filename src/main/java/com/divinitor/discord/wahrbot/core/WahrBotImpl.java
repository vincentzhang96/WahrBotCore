package com.divinitor.discord.wahrbot.core;

import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.divinitor.discord.wahrbot.core.config.RedisCredentials;
import com.divinitor.discord.wahrbot.core.config.SQLCredentials;
import com.divinitor.discord.wahrbot.core.util.WahrBotModule;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WahrBotImpl implements WahrBot {

    private static final int DATA_TIMEOUT_MS = 2000;

    public static void main(String[] args) {
        WahrBot bot = new WahrBotImpl();
        bot.run();
    }

    private final Logger LOGGER = LoggerFactory.getLogger(WahrBotImpl.class);

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

    public WahrBotImpl() {
        this.botDir = Paths.get(
                System.getProperty("com.divinitor.discord.wahrbot.home", ""))
                .toAbsolutePath();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
    }

    @Override
    public void run() {
        this.init();
        this.loadModules();
        this.startHttpServer();
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

    }

    private void loadModules() {
        //  Load modules

    }

    private void startHttpServer() {
        //  Start management HTTP server

    }

    private void startBot() {
        //  Connect to Discord and begin general execution

    }

    private void onShutdown() {
        LOGGER.info("Shutting down {}", this.getApplicationName());
        Map<String, Exception> shutdownExceptions = new HashMap<>();

        //  Shut down modules

        //  Shut down services

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
}
