package com.divinitor.discord.wahrbot.core.store;

import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import net.dv8tion.jda.api.entities.User;

public interface UserStore extends DynConfigStore {

    /**
     * Get the user that this UserStore is for.
     * @return This UserStore's user
     */
    User getUser();

    /**
     * Remove all data associated with this user.
     */
    void purge();
}
