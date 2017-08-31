package com.divinitor.discord.wahrbot.core;

import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.google.inject.Injector;
import net.dv8tion.jda.core.JDA;
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;
import java.nio.file.Path;

public interface WahrBot {
    void run();

    String getApplicationName();

    Injector getInjector();

    BotConfig getConfig();

    Path getBotDir();

    JDA getApiClient();

    DataSource getDataSource();

    JedisPool getJedisPool();
}
