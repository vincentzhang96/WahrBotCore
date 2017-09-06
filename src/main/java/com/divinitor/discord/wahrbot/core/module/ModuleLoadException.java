package com.divinitor.discord.wahrbot.core.module;

public class ModuleLoadException extends RuntimeException {

    public ModuleLoadException() {
    }

    public ModuleLoadException(String message) {
        super(message);
    }

    public ModuleLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModuleLoadException(Throwable cause) {
        super(cause);
    }

    public ModuleLoadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
