package com.divinitor.discord.wahrbot.core.util.logging;

import net.dv8tion.jda.core.utils.SimpleLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

public class SimpleLogRedirect implements SimpleLog.LogListener {

    private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SimpleLogRedirect() {}

    public static void addListener() {
        SimpleLog.LEVEL = SimpleLog.Level.OFF;
        SimpleLog.addListener(new SimpleLogRedirect());
    }

    @Override
    public void onLog(SimpleLog simpleLog, SimpleLog.Level level, Object o) {
        Logger logger = LoggerFactory.getLogger(LOGGER.getName()  + "." + simpleLog.name);
        Consumer<String> c;
        switch (level) {
            case TRACE:
                c = logger::trace;
                break;
            case DEBUG:
                c = logger::debug;
                break;
            case INFO:
                c = logger::info;
                break;
            case WARNING:
                c = logger::warn;
                break;
            case FATAL:
                c = logger::error;
                break;
            default:
                c = logger::info;
        }
        c.accept(String.valueOf(o));
    }

    @Override
    public void onError(SimpleLog simpleLog, Throwable throwable) {
        Logger logger = LoggerFactory.getLogger(LOGGER.getName()  + "." + simpleLog.name);
        logger.error("JDA unhandled exception", throwable);
    }
}
