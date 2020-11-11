package com.divinitor.discord.wahrbot.core.util.discord;

import com.divinitor.discord.wahrbot.core.command.CommandContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A helper class for resolving member names
 */
public class MemberResolution {

    /**
     * A Regex pattern that matches usernames-discriminator (Name#1234) format
     */
    private static final Pattern USERNAME_DISCRIM = Pattern.compile(".+?#[0-9][0-9][0-9][0-9]$");

    /**
     * Private constructor
     */
    private MemberResolution() {}

    /**
     * Try to find a member, given a query. The query can be an exact name, a nickname, a prefix, a full
     * username#discrim, a short ID, or a long ID.
     * @param query The query
     * @param context The command context to find the member in
     * @return The best matched member, or null if no match could be made
     */
    public static Member findMember(String query, CommandContext context) {
        String queryLower = query.toLowerCase();
        Guild guild = context.getServer();
        List<Member> members = guild.getMembers();

        //  If it looks like a username + discrim, then go for an exact match
        if (USERNAME_DISCRIM.matcher(queryLower).find()) {
            String name = queryLower.substring(0, queryLower.length() - 5);
            String discrim = queryLower.substring(queryLower.length() - 4);
            for (Member member : members) {
                User user = member.getUser();
                if (user.getName().equalsIgnoreCase(name) && user.getDiscriminator().equals(discrim)) {
                    return member;
                }
            }

            //  Exact matches only if you go for that format
            return null;
        }

        //  WahrBot short ID
        if (query.startsWith(SnowflakeUtils.PREFIX)) {
            try {
                long id = SnowflakeUtils.decode(query);
                Member memberById = guild.getMemberById(id);
                if (memberById != null) {
                    return memberById;
                }
            } catch (Exception ignored) {}
        }

        //  Discord long ID
        long id;
        try {
            id = Long.parseUnsignedLong(query);
            //  If we don't got bits after the 22nd bit, it probs aint an ID
            if (id >>> 22 != 0) {
                Member memberById = guild.getMemberById(id);
                if (memberById != null) {
                    return memberById;
                }
            }
        } catch (Exception ignored) {}

        //  Try nicknames first
        Member temp = null;
        for (Member member : members) {
            String nickname = member.getNickname();
            if (nickname != null) {
                //  Exact matches first
                if (nickname.toLowerCase().equals(queryLower)) {
                    return member;
                }

                //  Try partial frontal matches!
                if (nickname.toLowerCase().startsWith(queryLower)) {
                    if (temp == null || nickname.length() <= temp.getNickname().length()) {
                        temp = member;
                    }
                }
            }
        }
        
        if (temp != null) {
            return temp;
        }

        temp = null;
        //  Try usernames
        for (Member member : members) {
            String username = member.getUser().getName();
            if (username.toLowerCase().equals(queryLower)) {
                return member;
            }

            if (username.toLowerCase().startsWith(queryLower)) {
                if (temp == null || username.length() <= temp.getUser().getName().length()) {
                    temp = member;
                }
            }
        }

        if (temp != null) {
            return temp;
        }

        return null;
    }

}
