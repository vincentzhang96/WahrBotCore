package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigHandle;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public class CommandDispatcherImpl implements CommandDispatcher {

    public static final String DEFAULT_COMMAND_PREFIX_KEY = "com.divinitor.discord.wahrbot.core.command.prefix.default";

    private static final String ROOT_LOCALE_KEY = "com.divinitor.discord.wahrbot.cmd.";

    public static String getRootLocaleKey() {
        return ROOT_LOCALE_KEY;
    }

    private WahrBot bot;

    @Getter
    private final CommandRegistry rootRegistry;

    private final DynConfigHandle defaultCommandPrefixHandle;

    public CommandDispatcherImpl(WahrBot bot) {
        this.bot = bot;
        this.rootRegistry = new RootCommandRegistry(getRootLocaleKey() + "root");
        this.defaultCommandPrefixHandle = bot.getDynConfigStore().getStringHandle(DEFAULT_COMMAND_PREFIX_KEY);

        //  Load command localization strings
        //  TODO use an external DVLOC bundle
        this.bot.getLocalizer().registerBundle("com.divinitor.discord.wahrbot.core.command",
            new ResourceBundleBundle("com.divinitor.discord.wahrbot.core.command.locale"));
    }

    @Override
    @Subscribe
    public void handlePrivateMessage(PrivateMessageReceivedEvent event) {
        if (shouldIgnore(event.getAuthor())) {
            return;
        }

        CommandLine cmdline = new CommandLine(event.getMessage().getRawContent());
        cmdline.takeOptionalPrefix(this.defaultCommandPrefixHandle.get());
        //  TODO
        CommandContext context = null;

//        this.rootRegistry.invoke(context);
    }

    @Override
    @Subscribe
    public void handleServerMessage(GuildMessageReceivedEvent event) {
        if (shouldIgnore(event.getAuthor())) {
            return;
        }

        CommandLine cmdline = new CommandLine(event.getMessage().getRawContent());
        if (!cmdline.hasPrefixAndTake(this.getPrefixForServer(event.getGuild()))) {
            return;
        }

        StandardGuildCommandContext context = new StandardGuildCommandContext(this.bot,
            event,
            cmdline,
            this.rootRegistry);

        CommandResult result = CommandResult.error();
        Throwable err = null;
        try {
            result = this.rootRegistry.invoke(context);
        } catch (Exception e) {
            err = e;
        }
        switch (result.getType()) {
            case ERROR:
                this.handleCommandError(context, err);
                break;
            case NO_PERM:
                this.handleNoPerms(context);
                break;
            case NO_BOT_PERM:
                this.handleBotNoPerms(context);
                break;
            case NO_SUCH_COMMAND:
            case OK:
            case HANDLED:
                //  Ignored
                break;
        }
    }

    private void handleCommandError(CommandContext context, Throwable throwable) {
        //  TODO
        context.getFeedbackChannel().sendMessage(new MessageBuilder()
            .append("PLACEHOLDER Command execution error")
            .appendCodeBlock(throwable.toString(), null)
            .append("UUID ")
            .append(context.contextUuid())
            .build())
            .queue();
    }

    private void handleNoPerms(CommandContext context) {
        //  TODO
        context.getFeedbackChannel().sendMessage(new MessageBuilder()
            .append("PLACEHOLDER You don't have permission for this")
            .append("UUID ")
            .append(context.contextUuid())
            .build())
            .queue();
    }

    private void handleBotNoPerms(CommandContext context) {
        //  TODO
        context.getFeedbackChannel().sendMessage(new MessageBuilder()
            .append("PLACEHOLDER Bot doesn't have permission for this")
            .append("UUID ")
            .append(context.contextUuid())
            .build())
            .queue();
    }

    private boolean shouldIgnore(User author) {
        //  Bots and other undesirables get ignored here
        //  TODO user blacklist
        if (author.isBot()) {
            return true;
        }
        return false;
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
            return CommandDispatcherImpl.this.getPrefixForServer(context.getServer());
        }
    }
}
