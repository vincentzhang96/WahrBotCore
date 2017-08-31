package com.divinitor.discord.wahrbot.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotConfig {

    private DiscordCredentials discord;
    private RedisCredentials redis;
    private SQLCredentials sqlCredentials;
    private String instanceName;
}
