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
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WahrBot {

    private static final int DATA_TIMEOUT_MS = 2000;

    public static void main(String[] args) {
        WahrBot bot = new WahrBot();
        bot.run();
    }

    private final Logger LOGGER = LoggerFactory.getLogger(WahrBot.class);

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

    public WahrBot() {
        botDir = Paths.get(
                System.getProperty("com.divinitor.discord.wahrbot.home", ""))
                .toAbsolutePath();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
    }

    public void run() {
        init();
        loadModules();
        startHttpServer();
        startBot();
    }

    private void init() {
        LOGGER.info("Starting {}...", getApplicationName());

        //  Load config, init core services, and connect to DBs/KVSs
        Gson gson = StandardGson.pretty();
        try (BufferedReader reader = Files.newBufferedReader(botDir.resolve("config.json"), UTF_8)) {
            config = gson.fromJson(reader, BotConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }

        LOGGER.info("Instance: {}", config.getInstanceName());

        //  Init SQL connection
        SQLCredentials sqlCredentials = config.getSqlCredentials();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find SQL driver class", e);
        }

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
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to SQL server", e);
        }

        //  Init Redis connection
        RedisCredentials redisCredentials = config.getRedis();
        GenericObjectPoolConfig objectPoolConfig = new GenericObjectPoolConfig();
        jedisPool = new JedisPool(
                objectPoolConfig,
                redisCredentials.getHost(),
                redisCredentials.getPort(),
                DATA_TIMEOUT_MS,
                redisCredentials.getPassword(),
                redisCredentials.getDatabase());

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to Redis server", e);
        }

        //  Set up DI
        injector = Guice.createInjector(new WahrBotModule(this));

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
        LOGGER.info("Shutting down {}", getApplicationName());
        Map<String, Exception> shutdownExceptions = new HashMap<>();

        //  Shut down modules

        //  Shut down services

        //  Shut down API client
        try {
            if (apiClient != null) {
                apiClient.shutdownNow();
            }
        } catch (Exception e) {
            shutdownExceptions.put("discordapi", e);
        }

        //  Shut down SQL connection
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (Exception e) {
            shutdownExceptions.put("sql", e);
        }

        //  Shut down redis connection
        try {
            jedisPool.close();
        } catch (Exception e) {
            shutdownExceptions.put("redis", e);
        }

        if (shutdownExceptions.isEmpty()) {
            LOGGER.info("Shutdown successful");
        } else {
            LOGGER.warn("One or more exceptions during shutdown. Shutdown of {} may not be clean!",
                    getApplicationName());
            shutdownExceptions.forEach(LOGGER::warn);
        }
    }

    public String getApplicationName() {
        return "WahrBot";
    }
}
