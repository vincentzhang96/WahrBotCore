package com.divinitor.discord.wahrbot.core.service;

import java.util.NoSuchElementException;

public interface ServiceBus {

    void registerService(Class<?> clazz, Object val) throws IllegalArgumentException;

    void registerService(Object val) throws IllegalArgumentException;

    <T> T getService(Class<T> clazz) throws NoSuchElementException;

    void unregisterService(Class<?> clazz) throws NoSuchElementException;

    void unregisterAll();
}
