package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.google.inject.Inject;
import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.divinitor.discord.wahrbot.core.util.concurrent.Lockable.acquire;

public class CommandRegistryImpl implements CommandRegistry {

    private final Map<String, CommandWrapper> commands;
    private final ReadWriteLock commandLock;
    private CommandRegistry parent;
    private final String nameKey;
    private Command defaultCommand;

    @Inject
    private Localizer loc;

    public CommandRegistryImpl(String nameKey, CommandRegistry parent) {
        this.nameKey = nameKey;
        this.setParent(parent);
        this.commands = new HashMap<>();
        this.commandLock = new ReentrantReadWriteLock();
        this.defaultCommand = new HelpCommand();
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        CommandWrapper command = this.getWrapperFor(context.getCommandLine(), context);

        if (command != null) {
            if (!hasPermissionFor(command, context)) {
                return CommandResult.NO_PERM;
            }

            Command cmd = command.getCommand();
            if (!cmd.getBotPermissionConstraints().check(context)) {
                return CommandResult.NO_BOT_PERM;
            }

            if (!cmd.getOtherConstraints().check(context)) {
                return CommandResult.ERROR;
            }

            return cmd.invoke(context);
        }

        return CommandResult.NO_SUCH_COMMAND;
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
    public String getCommandNameChain(CommandContext context) {
        if (this.parent != null) {
            return this.parent.getCommandNameChain(context)
                + " " + this.loc.localizeToLocale(this.nameKey, context.getLocale());
        } else {
            return "";
        }
    }

    @Getter
    private class CommandWrapper {

        private final String key;
        private final Command command;

        private CommandWrapper(String key, Command command) {
            this.key = key;
            this.command = command;
        }
    }

    class HelpCommand implements Command {

        @Override
        public CommandResult invoke(CommandContext context) {
            Locale l = context.getLocale();
            EmbedBuilder builder = new EmbedBuilder();

            if (context.getCommandLine().hasNext()) {
                //  Subcommand


            } else {
                //  Bulk
                builder.setTitle(loc.localizeToLocale(
                    CommandDispatch.getRootLocaleKey() + "help.title",
                    l,
                    getCommandNameChain(context)));

            }

            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();

            return CommandResult.OK;
        }
    }
}
