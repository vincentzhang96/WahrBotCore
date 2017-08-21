package com.divinitor.discord.wahrbot.core.module;

public interface Module {

    void init(ModuleContext context) throws Exception;

    void shutDown();
}
