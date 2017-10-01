package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigHandle;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import static com.divinitor.discord.wahrbot.core.command.CommandTokenizer.*;

public class CommandDispatch {

    public static final String DEFAULT_COMMAND_PREFIX_KEY = "com.divinitor.discord.wahrbot.core.command.prefix.default";

    private WahrBot bot;
    private final CommandRegistry rootRegistry;

    private final DynConfigHandle defaultCommandPrefixHandle;

    public CommandDispatch(WahrBot bot) {
        this.bot = bot;
        rootRegistry = null;
        this.defaultCommandPrefixHandle = bot.getDynConfigStore().getStringHandle(DEFAULT_COMMAND_PREFIX_KEY);
    }

    @Subscribe
    public void handlePrivateMessage(PrivateMessageReceivedEvent event) {
        CommandLine cmdline = new CommandLine(event.getMessage().getRawContent());
        cmdline.takeOptionalPrefix(this.defaultCommandPrefixHandle.get());

        CommandContext commandContext = CommandContext.from(event);


    }

    @Subscribe
    public void handleServerMessage(GuildMessageReceivedEvent event) {
        CommandLine cmdline = new CommandLine(event.getMessage().getRawContent());
        if (!cmdline.hasPrefixAndTake(this.getPrefixForServer(event.getGuild()))) {
            return;
        }

        CommandContext commandContext = CommandContext.from(event);

    }

    public String getPrefixForServer(ISnowflake guildId) {
        //  TODO
        return this.defaultCommandPrefixHandle.get();
    }


}
