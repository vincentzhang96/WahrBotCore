package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.store.UserStore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public interface CommandContext {

    /**
     * Gets the bot that this command is being executed on.
     * @return The WahrBot instance
     */
    WahrBot getBot();

    /**
     * Gets the API instance that this command is being executed on.
     * @return The API client
     */
    JDA getApi();

    /**
     * Gets the JDA message object that invoked this command.
     * @return The JDA message that invoked this command
     */
    Message getMessage();

    /**
     * Gets the JDA channel object that the command was invoked in.
     * @return The JDA channel that invoked this command
     */
    TextChannel getInvocationChannel();

    /**
     * Gets the JDA channel object that command feedback responses should be sent to. In most cases, this will be the
     * same as the invocation channel.
     * @return The JDA channel that should be used for command feedback
     */
    TextChannel getFeedbackChannel();

    /**
     * Gets the JDA user that invoked this command.
     * @return The JDA user that invoked this command
     */
    User getInvoker();

    /**
     * Gets the JDA user that impersonated this command invocation. Null if this is not an impersonation situation.
     * @return The JDA user that impersonated the invocation of this command, or null if N/A
     */
    User getImpersonator();

    /**
     * Gets the JDA member that invoked this command. Null if this is not an invocation from a server.
     * @return The JDA member that invoked this command, or null if private
     */
    Member getMember();

    /**
     * Gets the JDA server that invoked this command, or null if this was a private message.
     * @return The JDA server that invoked this command, or null if private
     */
    Guild getServer();

    /**
     * Gets the server store associated with this server. For private messages, this is null.
     * @return The ServerStore associated with this server, or a null if private
     */
    ServerStore getServerStorage();

    /**
     * Gets the user store associated with the command invoker.
     * @return The UserStore associated with the command invoker
     */
    UserStore getUserStorage();

    /**
     * Gets the current command line that this command is being executed on.
     * @return The current command line for this context
     */
    CommandLine getCommandLine();

    /**
     * Get the command registry that owns this command.
     * @return The owning command registry
     */
    CommandRegistry getRegistry();

    /**
     * Get the JDA event that invoked this command.
     *
     * <b>WARNING:</b> The event's type is not guaranteed. Perform the necessary checks and do not assume.
     * @return The JDA Event that caused the invocation of this command.
     */
    Event getEvent();

    /**
     * Whether or not this command was invoked from a private message.
     * @return True if private, false otherwise
     */
    default boolean isPrivate() {
        return this.getServer() == null;
    }

    /**
     * Returns the locale for this invocation.
     * @return The locale to use
     */
    Locale getLocale();

    /**
     * Get the named localization params. This map contains various context-based named params and should be passed to
     * as the last argument in localization.
     * @return A map of named params for localization
     */
    Map<String, Object> getNamedLocalizationContextParams();

    /**
     * Get the UUID associated with this context invocation.
     * @return The UUID for this context
     */
    UUID contextUuid();

    /**
     * Convenience access to the bot localizer instance.
     * @return The bot localizer instance
     */
    default Localizer getLocalizer() {
        return this.getBot().getLocalizer();
    }

    /**
     * Get the name key associated with this command invocation.
     * @return The name key for the executing command
     */
    String getCommandNameKey();
}
