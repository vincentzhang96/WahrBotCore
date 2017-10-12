package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.UserStorage;
import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.*;
import java.util.function.Supplier;

@Getter
public class StandardGuildCommandContext implements CommandContext {

    private final WahrBot bot;
    private final GuildMessageReceivedEvent event;
    private final CommandLine commandLine;
    private final CommandRegistry registry;
    private final UUID uuid;

    public StandardGuildCommandContext(WahrBot bot,
                                       GuildMessageReceivedEvent event,
                                       CommandLine commandLine,
                                       CommandRegistry registry) {
        this.bot = bot;
        this.event = event;
        this.commandLine = commandLine;
        this.registry = registry;
        this.uuid = UUID.randomUUID();
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
    public TextChannel getInvocationChannel() {
        return this.event.getChannel();
    }

    @Override
    public TextChannel getFeedbackChannel() {
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
    public ServerStorage getServerStorage() {
        //  TODO
        return null;
    }

    @Override
    public UserStorage getUserStorage() {
        //  TODO
        return null;
    }

    @Override
    public Event getEvent() {
        return this.event;
    }

    @Override
    public Locale getLocale() {
        //  TODO
        return this.bot.getLocalizer().getDefaultLocale();
    }

    @Override
    public Map<String, Object> getNamedLocalizationContextParams() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("NAMECHAIN", wrap(() -> this.getRegistry().getCommandNameChain(this)));
        ret.put("USER.NAME", wrap(this.getInvoker()::getName));
        ret.put("USER.DISCRIM", wrap(this.getInvoker()::getDiscriminator));
        ret.put("USER.ID", wrap(this.getInvoker()::getId));
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

    private static Supplier<String> wrap(Supplier<String> s) {
        return s;
    }
}
