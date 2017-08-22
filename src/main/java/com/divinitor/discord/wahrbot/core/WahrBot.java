package com.divinitor.discord.wahrbot.core;

public class WahrBot {

    public static void main(String[] args) {

    }


    public void run() {
        init();
        loadModules();
        startHttpServer();
        startBot();
    }

    private void init() {
        //  Load config, init core services, and connect to DBs/KVSs

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
}
