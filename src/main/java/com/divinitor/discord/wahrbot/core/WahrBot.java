package com.divinitor.discord.wahrbot.core;

import com.divinitor.discord.wahrbot.core.config.BotConfig;
import com.divinitor.discord.wahrbot.core.util.WahrBotModule;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Getter;
import net.dv8tion.jda.core.JDA;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WahrBot {

    public static void main(String[] args) {
        WahrBot bot = new WahrBot();
        bot.run();
    }

    @Getter
    private Injector injector;

    @Getter
    private BotConfig config;

    @Getter
    private final Path botDir;

    @Getter
    private JDA apiClient;

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
        //  Load config, init core services, and connect to DBs/KVSs
        Gson gson = StandardGson.pretty();
        try (BufferedReader reader = Files.newBufferedReader(botDir.resolve("config.json"), UTF_8)) {
            config = gson.fromJson(reader, BotConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //  Set up DI
        injector = Guice.createInjector(new WahrBotModule(this));

        //  Init PostgrSQL connection

        //  Init Redis connection

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

    }
}
