package com.divinitor.discord.wahrbot.core.store;

import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import net.dv8tion.jda.core.entities.Member;

public interface MemberStore extends DynConfigStore {

    /**
     * Get the member that this MemberStore is for.
     * @return This MemberStore's member
     */
    Member getMember();

    /**
     * Remove all data associated with this user.
     */
    void purge();
}
