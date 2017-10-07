package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.UserStorage;
import lombok.Getter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@Getter
public class StandardGuildCommandContext implements CommandContext {

    private final WahrBot bot;
    private final GuildMessageReceivedEvent event;
    private final CommandLine commandLine;
    private final CommandRegistry registry;

    public StandardGuildCommandContext(WahrBot bot,
                                       GuildMessageReceivedEvent event,
                                       CommandLine commandLine,
                                       CommandRegistry registry) {
        this.bot = bot;
        this.event = event;
        this.commandLine = commandLine;
        this.registry = registry;
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
}
