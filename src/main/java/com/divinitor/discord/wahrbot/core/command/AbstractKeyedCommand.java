package com.divinitor.discord.wahrbot.core.command;

import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;

import java.util.StringJoiner;

/**
 * An abstract Command implementation that takes care of basics such as registration and resource registration
 */
public abstract class AbstractKeyedCommand implements Command {

    /**
     * Get the unique key that identifies this command
     * @return The unique key for this command
     */
    public abstract String getKey();

    /**
     * Get the class path to this command's ResourceBundle
     * @return The class path to this command's ResourceBundle
     */
    protected abstract String getResourcePath();

    /**
     * Construct a key based off of the command's base key. For example, with a base key of foo.bar, an argument of
     * [baz, boo], the result would be foo.bar.baz.boo
     * @see AbstractKeyedCommand#getKey()
     * @param args The sub-key values
     * @return The constructed key
     */
    public String key(String... args) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(getKey());
        for (String s : args) {
            joiner.add(s);
        }

        return joiner.toString();
    }

    /**
     * Register this command to the given registry and localizer
     * @param commandRegistry The registry to add this command to
     * @param localizer The localizer to add this command to
     */
    public void register(CommandRegistry commandRegistry, Localizer localizer) {
        localizer.registerBundle(this.key(),
            new ResourceBundleBundle(this.getResourcePath(),
                this.getClass().getClassLoader()));
        commandRegistry.registerCommand(this, this.key());
    }

    /**
     * Unregister this command from the given registry and localizer
     * @param commandRegistry The command to remove this command to
     * @param localizer The localizer to unregister from
     */
    public void unregister(CommandRegistry commandRegistry, Localizer localizer) {
        localizer.unregisterBundle(this.key());
        commandRegistry.unregisterCommand(this.key());
    }
}
