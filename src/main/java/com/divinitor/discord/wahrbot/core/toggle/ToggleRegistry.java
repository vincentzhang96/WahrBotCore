package com.divinitor.discord.wahrbot.core.toggle;

public interface ToggleRegistry {

    Toggle getToggle(String key);

    Toggle addChildToggle(Toggle parent, String key);

}
