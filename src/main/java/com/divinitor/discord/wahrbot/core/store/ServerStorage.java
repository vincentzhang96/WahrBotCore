package com.divinitor.discord.wahrbot.core.store;

import net.dv8tion.jda.api.entities.Guild;

/**
 * Per-server storage and server-context user storage.
 */
public interface ServerStorage {

    /**
     * Gets a {@link ServerStore} for the given user, creating and initializing it if one doesn't already exist.
     * @param server The server
     * @return A {@link ServerStore} for the server
     */
    ServerStore forServer(Guild server);
}
