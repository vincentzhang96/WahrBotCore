package com.divinitor.discord.wahrbot.core.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.utils.PermissionUtil;

public class CommandConstraints {

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
        return context -> !context.isPrivate() && context.getServer().getOwner().equals(context.getMember());
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
        return PermissionUtil.checkPermission(context.getInvocationChannel(), member, perms);
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
        for (Permission p : perms) {
            if (PermissionUtil.checkPermission(context.getInvocationChannel(), member, p)) {
                return true;
            }
        }

        return false;
    }
}
