package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.EmbedBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.divinitor.discord.wahrbot.core.util.concurrent.Lockable.acquire;

public class CommandRegistryImpl implements CommandRegistry {

    private final Map<String, CommandWrapper> commands;
    private final ReadWriteLock commandLock;
    private CommandRegistry parent;
    private final String nameKey;
    private Command defaultCommand;
    @Setter
    private CommandConstraint<CommandContext> userPermissionConstraints;
    @Setter
    private CommandConstraint<CommandContext> botPermissionConstraints;
    @Setter
    private CommandConstraint<CommandContext> otherConstraints;

    @Inject
    private Localizer loc;

    public CommandRegistryImpl(String nameKey) {
        this(nameKey, null);
    }

    public CommandRegistryImpl(String nameKey, CommandRegistry parent) {
        this.nameKey = nameKey;
        this.setParent(parent);
        this.commands = new HashMap<>();
        this.commandLock = new ReentrantReadWriteLock();
        this.defaultCommand = new HelpCommand();
        this.userPermissionConstraints = CommandConstraints.allow();
        this.botPermissionConstraints = CommandConstraints.allow();
        this.otherConstraints = CommandConstraints.allow();
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        context = new StandardGuildCommandContext(context, this);

        CommandWrapper command = this.getWrapperFor(context.getCommandLine(), context);

        if (command != null) {
            if (!hasPermissionFor(command, context)) {
                return CommandResult.noPerm();
            }

            Command cmd = command.getCommand();
            if (!cmd.getBotPermissionConstraints().check(context)) {
                return CommandResult.noBotPerm();
            }

            if (!cmd.getOtherConstraints().check(context)) {
                return CommandResult.rejected();
            }

            return cmd.invoke(context);
        }

        return CommandResult.noSuchCommand();
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
    public CommandRegistry getParent() {
        return this.parent;
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
            return null;
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
        return null;
    }

    @Override
    public boolean hasPermissionFor(CommandLine commandLine, CommandContext context) {
        String head = commandLine.peek();
        Locale locale = context.getLocale();
        return this.hasPermissionFor(getCommandWrapper(head, locale), context);
    }

    private boolean hasPermissionFor(CommandWrapper wrapper, CommandContext context) {
        return wrapper.getCommand().getUserPermissionConstraints().and(this::checkExternalPermissions).check(context);
    }

    private boolean checkExternalPermissions(CommandContext context) {
        //  TODO
        return true;
    }

    @Override
    public void setDefaultCommand(Command command) {
        if (command == null) {
            command = new HelpCommand();
        }
        this.defaultCommand = command;
    }

    @Override
    public void registerCommand(Command command, String commandKey) {
        CommandWrapper wrapper = new CommandWrapper(commandKey, command);
        try (Lockable l = acquire(this.commandLock.writeLock())) {
            this.commands.put(commandKey, wrapper);
        }
    }

    @Override
    public void unregisterCommand(String commandKey) {
        try (Lockable l = acquire(this.commandLock.writeLock())) {
            this.commands.remove(commandKey);
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
        if (this.parent != null) {
            return this.parent.getCommandNameChain(context)
                + " " + this.loc.localizeToLocale(this.nameKey, context.getLocale());
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
                        "com.divinitor.discord.wahrbot.cmd.help.command.notfound",
                        l,
                        nlcParams
                    ));
                } else {

                }
            } else {
                //  Bulk
                builder.setTitle(loc.localizeToLocale(
                    CommandDispatcherImpl.getRootLocaleKey() + "help.title",
                    l,
                    nlcParams));

                //  DESCRIPTION
                String descKey = nameKey + ".desc";
                if (loc.contains(descKey)) {
                    builder.setDescription(loc.localizeToLocale(
                        descKey,
                        l,
                        nlcParams));
                } else {
                    builder.setDescription(loc.localizeToLocale(
                        CommandDispatcherImpl.getRootLocaleKey() + "help.desc",
                        l,
                        nlcParams));
                }

                //  FOOTER
                String footerKey = nameKey + ".footer";
                if (loc.contains(footerKey)) {
                    builder.setFooter(loc.localizeToLocale(
                        footerKey,
                        l,
                        nlcParams), null);
                } else {
                    builder.setFooter(loc.localizeToLocale(
                        CommandDispatcherImpl.getRootLocaleKey() + "help.footer",
                        l,
                        nlcParams), null);
                }

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

            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();

            return CommandResult.ok();
        }
    }
}
