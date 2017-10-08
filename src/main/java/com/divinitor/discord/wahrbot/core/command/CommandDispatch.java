package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigHandle;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public class CommandDispatch {

    public static final String DEFAULT_COMMAND_PREFIX_KEY = "com.divinitor.discord.wahrbot.core.command.prefix.default";

    private static final String ROOT_LOCALE_KEY = "com.divinitor.discord.wahrbot.cmd.";

    public static String getRootLocaleKey() {
        return ROOT_LOCALE_KEY;
    }

    private WahrBot bot;
    private final CommandRegistry rootRegistry;

    private final DynConfigHandle defaultCommandPrefixHandle;

    public CommandDispatch(WahrBot bot) {
        this.bot = bot;
        this.rootRegistry = new RootCommandRegistry(getRootLocaleKey() + "root");
        this.defaultCommandPrefixHandle = bot.getDynConfigStore().getStringHandle(DEFAULT_COMMAND_PREFIX_KEY);
    }

    @Subscribe
    public void handlePrivateMessage(PrivateMessageReceivedEvent event) {
        CommandLine cmdline = new CommandLine(event.getMessage().getRawContent());
        cmdline.takeOptionalPrefix(this.defaultCommandPrefixHandle.get());
        //  TODO
        CommandContext context = null;

        this.rootRegistry.invoke(context);
    }

    @Subscribe
    public void handleServerMessage(GuildMessageReceivedEvent event) {
        CommandLine cmdline = new CommandLine(event.getMessage().getRawContent());
        if (!cmdline.hasPrefixAndTake(this.getPrefixForServer(event.getGuild()))) {
            return;
        }

        StandardGuildCommandContext context = new StandardGuildCommandContext(this.bot,
            event,
            cmdline,
            this.rootRegistry);

        this.rootRegistry.invoke(context);
    }

    public String getPrefixForServer(ISnowflake guildId) {
        //  TODO
        return this.defaultCommandPrefixHandle.get();
    }


    class RootCommandRegistry extends CommandRegistryImpl {

        public RootCommandRegistry(String nameKey) {
            super(nameKey, null);
        }

        @Override
        public String getCommandNameChain(CommandContext context) {
            return CommandDispatch.this.getPrefixForServer(context.getServer());
        }
    }
}
