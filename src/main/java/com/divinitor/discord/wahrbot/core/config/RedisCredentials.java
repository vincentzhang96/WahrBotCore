package com.divinitor.discord.wahrbot.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RedisCredentials {

    private String host;
    private int port;
    private String password;
    private int database;
}
