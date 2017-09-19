package com.divinitor.discord.wahrbot.core.module;

public interface Module {

    /**
     * Called after this module is loaded to perform basic initialization. Implementing classes are automatically
     * registered to the application event and service buses. Any additional registrations should also occur here.
     * If this module depends on other modules, then initialization that depends on other modules being loaded
     * should occur in {@link #postBatchInit()}.
     * @param context The module's context
     * @throws Exception If there was an error
     */
    void init(ModuleContext context) throws Exception;

    /**
     * Called after all modules in the current loading batch have finished initializing. Avoid registering additional
     * services here.
     * @throws Exception If there was an error
     */
    default void postBatchInit() throws Exception {}

    /**
     * Called when the module is about to be unloaded. The module is automatically unregistered from the event and
     * service buses after this method returns. Any additional registrations should be unregistered and resources
     * released. Failure to properly unregister will result in the module remaining in memory, unable to be garbage
     * collected. Failing to unregister also can result in event handlers being called erroneously or result in
     * undefined or undesireable effects.
     */
    void shutDown();
}
