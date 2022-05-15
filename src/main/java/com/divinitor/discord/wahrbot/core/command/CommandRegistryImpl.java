package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.divinitor.discord.wahrbot.core.command.CommandDispatcherImpl.getRootLocaleKey;
import static com.divinitor.discord.wahrbot.core.util.concurrent.Lockable.acquire;

public class CommandRegistryImpl implements CommandRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<String, CommandWrapper> commands;
    private final ReadWriteLock commandLock;
    private final String nameKey;
    private CommandRegistry parent;
    private Command defaultCommand;
    private CommandWrapper defaultCommandWrapper;
    @Setter
    private CommandConstraint<CommandContext> userPermissionConstraints;
    @Setter
    private CommandConstraint<CommandContext> botPermissionConstraints;
    @Setter
    private CommandConstraint<CommandContext> otherConstraints;

    @Inject
    private Localizer loc;

    @Inject
    private Injector injector;

    private HelpCommand helpCommand;
    private CommandWrapper helpCommandWrapper;

    public CommandRegistryImpl(String nameKey) {
        this(nameKey, null);
    }

    public CommandRegistryImpl(String nameKey, CommandRegistry parent) {
        this.nameKey = nameKey;
        this.setParent(parent);
        this.commands = new HashMap<>();
        this.commandLock = new ReentrantReadWriteLock();
        this.defaultCommand = new HelpCommand();
        this.defaultCommandWrapper = new CommandWrapper(Localizer.PREFIX_DO_NOT_RESOLVE, this.defaultCommand);
        this.userPermissionConstraints = CommandConstraints.allow();
        this.botPermissionConstraints = CommandConstraints.allow();
        this.otherConstraints = CommandConstraints.allow();
        this.helpCommand = new HelpCommand();
        this.helpCommandWrapper = new CommandWrapper("com.divinitor.discord.wahrbot.cmd.help", this.helpCommand);
    }

    @Override
    public CommandResult invoke(CommandContext ctx) {
        StandardGuildCommandContext context = new StandardGuildCommandContext(ctx, this);

        CommandWrapper command = this.getWrapperFor(context.getCommandLine(), context);

        //  Consume next token cuz getWrapperFor uses peek
        if (context.getCommandLine().hasNext()) {
            context.getCommandLine().next();
        }

        if (command != null) {
            if (!hasPermissionFor(command, context)) {
                return CommandResult.noPerm();
            }

            Command cmd = command.getCommand();
            if (!ctx.getUserStorage().getBoolean("sudo", false)) {
                if (!cmd.getBotPermissionConstraints().check(context)) {
                    return CommandResult.noBotPerm();
                }

                if (!cmd.getOtherConstraints().check(context)) {
                    return CommandResult.rejected();
                }
            }

            context.setNameKey(command.getKey());

            return cmd.invoke(context);
        }

        return CommandResult.noSuchCommand();
    }

    @Override
    public CommandRegistry getParent() {
        return this.parent;
    }

    @Override
    public void setParent(CommandRegistry parent) throws IllegalArgumentException {
        //  Verify we don't have any cycles
        CommandRegistry pp = parent;
        if (pp != null) {
            while (pp != null) {
                if (pp == this) {
                    throw new IllegalArgumentException("Cycle detected");
                }
                pp = pp.getParent();
            }
        }

        this.parent = parent;
    }

    @Override
    public Command getCommandFor(CommandLine commandLine, CommandContext context) {
        if (!commandLine.hasNext()) {
            return this.defaultCommand;
        }

        CommandWrapper wrapper = getWrapperFor(commandLine, context);
        if (wrapper != null) {
            return wrapper.getCommand();
        }
        return null;
    }

    private CommandWrapper getWrapperFor(CommandLine commandLine, CommandContext context) {
        if (!commandLine.hasNext()) {
            return this.defaultCommandWrapper;
        }

        String head = commandLine.peek();
        Locale locale = context.getLocale();

        CommandWrapper wrapper = getCommandWrapper(head, locale);
        if (wrapper != null) {
            return wrapper;
        }
        return null;
    }

    @Nullable
    private CommandWrapper getCommandWrapper(String cmd, Locale locale) {
        try (Lockable l = acquire(this.commandLock.readLock())) {
            for (String key : this.commands.keySet()) {
                String locKey = this.loc.localizeToLocale(key, locale);
                if (cmd.equalsIgnoreCase(locKey)) {
                    return this.commands.get(key);
                }
            }
        }

        if (cmd.equalsIgnoreCase(this.loc.localizeToLocale("com.divinitor.discord.wahrbot.cmd.help", locale))) {
            return this.helpCommandWrapper;
        }

        return null;
    }

    @Override
    public boolean hasPermissionFor(CommandLine commandLine, CommandContext context) {
        String head = commandLine.peek();
        Locale locale = context.getLocale();
        return this.hasPermissionFor(getCommandWrapper(head, locale), context);
    }

    private boolean hasPermissionFor(CommandWrapper wrapper, CommandContext context) {
        Command command = wrapper.getCommand();
        return command
            .getUserPermissionConstraints()
            .and(this::checkExternalPermissions)
            .or(ctx -> ctx.getUserStorage().getBoolean("sudo", false))
            .and(command.getOtherConstraints())
            .check(context);
    }

    private boolean checkExternalPermissions(CommandContext context) {
        //  TODO
        return true;
    }

    @Override
    public void setDefaultCommand(Command command) {
        if (command == null) {
            this.defaultCommand = this.helpCommand;
            this.defaultCommandWrapper = this.helpCommandWrapper;
        } else {
            this.defaultCommand = command;
            this.defaultCommandWrapper = new CommandWrapper(Localizer.PREFIX_DO_NOT_RESOLVE, this.defaultCommand);
        }
    }

    @Override
    public void registerCommand(Command command, String commandKey) {
        CommandWrapper wrapper = new CommandWrapper(commandKey, command);
        try (Lockable l = acquire(this.commandLock.writeLock())) {
            this.commands.put(commandKey, wrapper);
            LOGGER.info("Registered command {} under {}", loc.localize(commandKey), loc.localize(this.nameKey));
        }
    }

    @Override
    public void unregisterCommand(String commandKey) {
        try (Lockable l = acquire(this.commandLock.writeLock())) {
            boolean removed = this.commands.remove(commandKey) != null;
            if (removed) {
                LOGGER.info("Unregistered command {} under {}",
                    loc.localize(commandKey), loc.localize(this.nameKey));
            } else {
                LOGGER.warn("Attempted to unregister nonexistant command {} under {}",
                    loc.localize(commandKey), loc.localize(this.nameKey));
            }
        }
    }

    @Override
    public boolean hasCommand(String commandKey) {
        try (Lockable l = acquire(this.commandLock.readLock())) {
            return this.commands.containsKey(commandKey);
        }
    }

    @Override
    public CommandRegistry getChild(String registryNameKey) {
        CommandWrapper wrapper;
        try (Lockable l = acquire(this.commandLock.readLock())) {
            wrapper = this.commands.get(registryNameKey);
        }
        if (wrapper == null) {
            return null;
        }

        Command c = wrapper.getCommand();
        if (c instanceof CommandRegistry) {
            return (CommandRegistry) c;
        }

        return null;
    }

    @Override
    public String getCommandNameChain(CommandContext context) {
        Locale locale = context.getLocale();
        if (this.parent != null) {
            return this.parent.getCommandNameChain(context)
                + this.loc.localizeToLocale(this.nameKey, locale) + " ";
        } else {
            return "";
        }
    }

    @Override
    public CommandRegistry makeRegistries(String... keys) {
        if (keys.length == 0) {
            return this;
        }

        String first = keys[0];
        CommandRegistry sub = this.getChild(first);
        if (sub == null) {
            sub = new CommandRegistryImpl(first, this);
            this.injector.injectMembers(sub);
            this.registerCommand(sub, first);
        }

        String[] reduced = new String[keys.length - 1];
        System.arraycopy(keys, 1, reduced, 0, reduced.length);
        return sub.makeRegistries(reduced);
    }

    @Override
    public CommandConstraint<CommandContext> getUserPermissionConstraints() {
        return this.userPermissionConstraints;
    }

    @Override
    public CommandConstraint<CommandContext> getBotPermissionConstraints() {
        return this.botPermissionConstraints;
    }

    @Override
    public CommandConstraint<CommandContext> getOtherConstraints() {
        return this.otherConstraints;
    }

    @Getter
    private class CommandWrapper {

        private final String key;
        private final Command command;

        private CommandWrapper(String key, Command command) {
            this.key = key;
            this.command = command;
        }

        public String localizeKey(CommandContext context) {
            return context.getLocalizer().localizeToLocale(this.key, context.getLocale());
        }

        public String helpKey() {
            return this.key + ".help";
        }

        public String descKey() {
            return this.key + ".help.desc";
        }

        public String syntaxListKey() {
            return this.key + ".help.syntax.list";
        }

        public String syntaxParamsKey() {
            return this.key + ".help.syntax.params";
        }

        public String exampleKey() {
            return this.key + ".help.examples";
        }

        public String remarksKey() {
            return this.key + ".help.remarks";
        }
    }

    class HelpCommand implements Command {

        @Override
        public CommandResult invoke(CommandContext context) {
            Locale l = context.getLocale();
            EmbedBuilder builder = new EmbedBuilder();
            Map<String, Object> nlcParams = context.getNamedLocalizationContextParams();

            CommandLine cl = context.getCommandLine();
            if (cl.hasNext()) {
                //  Subcommand
                String command = cl.next();
                CommandWrapper wrap = CommandRegistryImpl.this.getCommandWrapper(command, context.getLocale());
                if (wrap == null) {
                    builder.setDescription(loc.localizeToLocale(
                        getRootLocaleKey() + "help.command.notfound",
                        l,
                        nlcParams
                    ));
                } else {
                    //  TITLE
                    builder.setTitle(loc.localizeToLocale(
                        getRootLocaleKey() + "help.command.title",
                        l,
                        loc.localizeToLocale(wrap.key, l), nlcParams));

                    //  DESCRIPTION
                    builder.setDescription(loc.localizeToLocale(
                        wrap.descKey(),
                        l,
                        nlcParams
                    ));

                    //  SYNTAX
                    String syntaxBody;
                    String syntaxList = loc.localizeToLocale(
                        wrap.syntaxListKey(),
                        l,
                        nlcParams
                    );
                    String syntaxParams = loc.localizeToLocale(
                        wrap.syntaxParamsKey(),
                        l,
                        nlcParams
                    );
                    syntaxBody = loc.localizeToLocale(
                        getRootLocaleKey() + "help.command.value.syntax",
                        l,
                        syntaxList, syntaxParams,
                        nlcParams
                    );

                    builder.addField(loc.localizeToLocale(
                        getRootLocaleKey() + "help.command.heading.syntax",
                        l,
                        nlcParams
                        ),
                        syntaxBody, false);

                    //  EXAMPLES
                    if (loc.contains(wrap.exampleKey())) {
                        builder.addField(
                            loc.localizeToLocale(
                                getRootLocaleKey() + "help.command.heading.examples",
                                l,
                                nlcParams
                            ),
                            loc.localizeToLocale(
                                wrap.exampleKey(),
                                l,
                                nlcParams
                            ), false);
                    }

                    //  REMARKS
                    if (loc.contains(wrap.remarksKey())) {
                        builder.addField(
                            loc.localizeToLocale(
                                getRootLocaleKey() + "help.command.heading.remarks",
                                l,
                                nlcParams
                            ),
                            loc.localizeToLocale(
                                wrap.remarksKey(),
                                l,
                                nlcParams
                            ), false);
                    }
                }
            } else {
                //  Bulk
                builder.setTitle(loc.localizeToLocale(
                    getRootLocaleKey() + "help.title",
                    l,
                    nlcParams));

                //  DESCRIPTION
                String descKey = nameKey + ".desc";
                builder.setDescription(this.localizeOrDefault(descKey, getRootLocaleKey() + "help.desc",
                    l,
                    nlcParams));

                //  FOOTER
                String footerKey = nameKey + ".footer";
                builder.setFooter(this.localizeOrDefault(footerKey, getRootLocaleKey() + "help.footer",
                    l,
                    nlcParams), null);

                //  COMMANDS
                //  TODO categories (for now just bulk)
                //  Gotta sort

                String list;
                try (Lockable lock = acquire(CommandRegistryImpl.this.commandLock.readLock())) {
                    list = CommandRegistryImpl.this.commands.values().stream()
                        .sorted(Comparator.comparing(cw -> cw.localizeKey(context)))
                        .filter(cw -> CommandRegistryImpl.this.hasPermissionFor(cw, context))
                        .map(cw -> loc.localizeToLocale(
                            "com.divinitor.discord.wahrbot.cmd.help.command",
                            l,
                            cw.localizeKey(context),
                            loc.localizeToLocale(cw.helpKey(), l),
                            nlcParams))
                        .collect(Collectors.joining("\n"));
                }

                if (list.isEmpty()) {
                    list = loc.localizeToLocale("com.divinitor.discord.wahrbot.cmd.help.category.empty", l);
                }

                builder.addField(
                    loc.localizeToLocale(
                        "com.divinitor.discord.wahrbot.cmd.help.category.available",
                        l),
                    list,
                    false
                );
            }

            context.getFeedbackChannel().sendMessage(new MessageBuilder().setEmbeds(builder.build()).build())
                .queue();

            return CommandResult.ok();
        }

        private String localizeOrDefault(String key, String defaultKey, Locale l, Object... args) {
            if (loc.contains(key)) {
                return loc.localizeToLocale(
                    key,
                    l,
                    args);
            } else {
                return loc.localizeToLocale(
                    defaultKey,
                    l,
                    args);
            }
        }
    }

}
