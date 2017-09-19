package com.divinitor.discord.wahrbot.core.service.impl;

import com.divinitor.discord.wahrbot.core.service.ManagedService;
import com.divinitor.discord.wahrbot.core.service.ServiceBus;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.divinitor.discord.wahrbot.core.util.concurrent.Lockable.acquire;

public class ServiceBusImpl implements ServiceBus {

    private final Map<Class<?>, Object> services;
    private final ReadWriteLock lock;

    public ServiceBusImpl() {
        this.lock = new ReentrantReadWriteLock();
        this.services = new HashMap<>();
    }

    @Override
    public void registerService(Object o) throws IllegalArgumentException {
        this.registerService(o.getClass(), o);
    }

    @Override
    public void registerService(Class<?> clazz, Object val) throws IllegalArgumentException {
        if (!clazz.isInstance(val)) {
            throw new IllegalArgumentException("Value is not a valid type for " + clazz);
        }

        try (Lockable l = acquire(this.lock.writeLock())) {
            if (this.services.putIfAbsent(clazz, val) != null) {
                throw new IllegalArgumentException("Service " + clazz.getClass() + "already registered");
            }

            if (val instanceof ManagedService) {
                ((ManagedService) val).onRegister();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getService(Class<T> clazz) throws NoSuchElementException {
        try (Lockable l = acquire(this.lock.readLock())) {
            T t = (T) this.services.get(clazz);
            if (t == null) {
                throw new NoSuchElementException(clazz.getName());
            }

            return t;
        }
    }

    @Override
    public void unregisterService(Class<?> clazz) throws NoSuchElementException {
        try (Lockable l = acquire(this.lock.writeLock())) {
            Object o = this.services.remove(clazz);
            if (o == null) {
                throw new NoSuchElementException(clazz.toString());
            }

            if (o instanceof ManagedService) {
                ((ManagedService) o).onUnregister();
            }
        }
    }

    @Override
    public void unregisterAll() {
        try (Lockable l = acquire(this.lock.writeLock())) {
            //  Handle managed services
            this.services.values().stream()
                    .filter(o -> o instanceof ManagedService)
                    .map(o -> (ManagedService) o)
                    .forEach(ManagedService::onUnregister);
            //  Clear
            this.services.clear();
        }
    }
}
