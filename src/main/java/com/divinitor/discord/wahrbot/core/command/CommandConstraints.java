package com.divinitor.discord.wahrbot.core.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.internal.utils.PermissionUtil;

public class CommandConstraints {

    public static <T> CommandConstraint<T> allow() {
        return context -> true;
    }

    public static <T> CommandConstraint<T> deny() {
        return context -> false;
    }

    /**
     * If the context user has all of the given permissions. Always fails for direct messages.
     * @param perms The required permissions
     * @return A PermissionConstraint checking for all the given permissions
     */
    public static CommandConstraint<CommandContext> hasAll(Permission... perms) {
        return context -> hasAllPerms(context, perms);
    }

    /**
     * If the context user has any of the given permissions. Always fails for direct messages.
     * @param perms The required permissions
     * @return A PermissionConstraint checking for any of the given permissions
     */
    public static CommandConstraint<CommandContext> hasAny(Permission... perms) {
        return context -> hasAnyPerms(context, perms);
    }

    /**
     * If the context user is the owner of the context server. Always fails for direct messages.
     * @return A PermissionConstraint checking that the user is the owner of the server
     */
    public static CommandConstraint<CommandContext> isOwner() {
        return context -> {
            Member owner = context.getServer().getOwner();
            if (owner == null) {
                owner = context.getServer().retrieveOwner().complete();
            }
            return !context.isPrivate() && owner.equals(context.getMember());
        };
    }

    /**
     * If the bot has all of the permissions. Always fails for direct messages.
     * @param perms The required permissions
     * @return A PermissionConstraint checking for all of the given permissions
     */
    public static CommandConstraint<CommandContext> botHasAll(Permission... perms) {
        return context -> hasAllPerms(context.getBot().getApiClient().getSelfUser(), context, perms);
    }

    /**
     * If the bot has any of the permissions. Always fails for direct messages.
     * @param perms The required permissions
     * @return A PermissionConstraint checking for any of the given permissions
     */
    public static CommandConstraint<CommandContext> botHasAny(Permission... perms) {
        return context -> hasAnyPerms(context.getBot().getApiClient().getSelfUser(), context, perms);
    }

    public static CommandConstraint<CommandContext> isPublic() {
        return CommandConstraint.not(CommandContext::isPrivate);
    }

    public static CommandConstraint<CommandContext> isPrivate() {
        return CommandContext::isPrivate;
    }

    static boolean hasAllPerms(CommandContext context, Permission... perms) {
        Guild server = context.getServer();
        if (server == null) {
            return false;
        }
        return hasAllPerms(context.getInvoker(), context, perms);
    }

    static boolean hasAllPerms(User user, CommandContext context, Permission... perms) {
        Guild server = context.getServer();
        if (server == null) {
            return false;
        }

        Member member = server.getMember(user);
        if (member == null) {
            member = server.retrieveMember(user).complete();
        }

        if (context.isPrivate()) {
            return true;
        }

        TextChannel invocationChannel = (TextChannel)context.getInvocationChannel();
        return PermissionUtil.checkPermission(invocationChannel, member, perms);
    }

    static boolean hasAnyPerms(CommandContext context, Permission... perms) {
        Guild server = context.getServer();
        if (server == null) {
            return false;
        }

        return hasAnyPerms(context.getInvoker(), context, perms);
    }

    private static boolean hasAnyPerms(User user, CommandContext context, Permission... perms) {
        Guild server = context.getServer();
        if (server == null) {
            return false;
        }

        Member member = server.getMember(user);
        if (member == null) {
            member = server.retrieveMember(user, true).complete();
        }

        if (context.isPrivate()) {
            return true;
        }

        TextChannel invocationChannel = (TextChannel)context.getInvocationChannel();

        for (Permission p : perms) {
            if (PermissionUtil.checkPermission(invocationChannel, member, p)) {
                return true;
            }
        }

        return false;
    }
}
