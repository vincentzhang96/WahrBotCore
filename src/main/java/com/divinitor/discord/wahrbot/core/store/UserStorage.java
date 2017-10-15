package com.divinitor.discord.wahrbot.core.store;

import net.dv8tion.jda.core.entities.User;

/**
 * Per-user global storage.
 */
public interface UserStorage {

    /**
     * Gets a {@link UserStore} for the given user, creating and initializing it if one doesn't already exist.
     * @param user The user
     * @return A {@link UserStore} for the user
     */
    UserStore forUser(User user);
}
