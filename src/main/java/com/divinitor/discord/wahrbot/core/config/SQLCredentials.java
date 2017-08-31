package com.divinitor.discord.wahrbot.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SQLCredentials {

    private String host;
    private int port;
    private String username;
    private String password;
    private String database;
    private String jdbcProviderName;
    private String urlScheme;
}
