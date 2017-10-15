package com.divinitor.discord.wahrbot.core.store;

import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

public interface ServerStore extends DynConfigStore {

    /**
     * Get the server that this ServerStore is for.
     * @return This ServerStore's server
     */
    Guild getServer();

    /**
     * Gets a {@link MemberStore} for the given member, creating and initializing it if one doesn't already exist.
     * @param member The member
     * @return A {@link MemberStore} for the user
     */
    MemberStore forMember(Member member);

    void purge();
}
