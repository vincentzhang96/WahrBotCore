package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.store.UserStore;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Getter
public class StandardGuildCommandContext implements CommandContext {

    public static final String USER_LOCALE_KEY = "opt.locale";

    private final WahrBot bot;
    private final MessageReceivedEvent event;
    private final CommandLine commandLine;
    private final CommandRegistry registry;
    private final UUID uuid;

    @Setter
    private String nameKey;

    public StandardGuildCommandContext(WahrBot bot,
                                       MessageReceivedEvent event,
                                       CommandLine commandLine,
                                       CommandRegistry registry) {
        this.bot = bot;
        this.event = event;
        this.commandLine = commandLine;
        this.registry = registry;
        this.uuid = UUID.randomUUID();
    }

    public StandardGuildCommandContext(CommandContext context, CommandRegistry newRegistry) {
        this(context.getBot(),
            (MessageReceivedEvent) context.getEvent(),
            context.getCommandLine(),
            newRegistry);
    }

    private static Supplier<String> wrap(Supplier<String> s) {
        return s;
    }

    @Override
    public JDA getApi() {
        return bot.getApiClient();
    }

    @Override
    public Message getMessage() {
        return this.event.getMessage();
    }

    @Override
    public MessageChannel getInvocationChannel() {
        return this.event.getChannel();
    }

    @Override
    public MessageChannel getFeedbackChannel() {
        return this.event.getChannel();
    }

    @Override
    public User getInvoker() {
        return this.event.getAuthor();
    }

    @Override
    public User getImpersonator() {
        return null;
    }

    @Override
    public Member getMember() {
        return this.event.getMember();
    }

    @Override
    public Guild getServer() {
        return this.event.getGuild();
    }

    @Override
    public ServerStore getServerStorage() {
        return this.bot.getServerStorage().forServer(this.getServer());
    }

    @Override
    public UserStore getUserStorage() {
        return this.bot.getUserStorage().forUser(this.getInvoker());
    }

    @Override
    public Event getEvent() {
        return this.event;
    }

    @Override
    public Locale getLocale() {
        String userLocale = this.getUserStorage().getString(USER_LOCALE_KEY);
        if (Strings.isNullOrEmpty(userLocale)) {
            return this.bot.getLocalizer().getDefaultLocale();
        }

        return Locale.forLanguageTag(userLocale);
    }

    @Override
    public Map<String, Object> getNamedLocalizationContextParams() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("NAMECHAIN", wrap(() -> this.getRegistry().getCommandNameChain(this)));
        ret.put("COMMANDLINE", wrap(this.getCommandLine()::getOriginal));
        ret.put("COMMANDNAME", wrap(() -> this.getLocalizer().localizeToLocale(
            this.getCommandNameKey(), this.getLocale())));
        ret.put("USER.NAME", wrap(this.getInvoker()::getName));
        ret.put("USER.DISCRIM", wrap(this.getInvoker()::getDiscriminator));
        ret.put("USER.ID", wrap(this.getInvoker()::getId));
        ret.put("USER.SID", wrap(() -> SnowflakeUtils.encode(this.getInvoker().getIdLong())));
        ret.put("CHANNEL.NAME", wrap(this.getFeedbackChannel()::getName));
        ret.put("CHANNEL.ID", wrap(this.getFeedbackChannel()::getId));
        ret.put("SERVER.NAME", wrap(this.getServer()::getName));
        ret.put("SERVER.ID", wrap(this.getServer()::getId));
        ret.put("MESSAGE.ID", wrap(this.getMessage()::getId));
        ret.put("UUID", wrap(this.getUuid()::toString));
        return ret;
    }

    @Override
    public UUID contextUuid() {
        return this.uuid;
    }

    @Override
    public String getCommandNameKey() {
        return this.nameKey;
    }
}
