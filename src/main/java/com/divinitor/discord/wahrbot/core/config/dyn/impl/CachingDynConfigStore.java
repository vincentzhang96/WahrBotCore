package com.divinitor.discord.wahrbot.core.config.dyn.impl;

import com.divinitor.discord.wahrbot.core.config.dyn.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CachingDynConfigStore implements DynConfigStore {

    private final DynConfigStore supplier;
    private final LoadingCache<String, String> cache;

    public CachingDynConfigStore(DynConfigStore supplier) {
        this.supplier = supplier;
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws Exception {
                    return CachingDynConfigStore.this.supplier.getString(key);
                }
            });
    }

    @Override
    public void put(String key, String value) {
        this.supplier.put(key, value);
        this.cache.invalidate(key);
    }

    @Override
    public String getString(String key) {
        try {
            return this.cache.get(key);
        } catch (ExecutionException e) {
            return this.supplier.getString(key);
        } catch (CacheLoader.InvalidCacheLoadException icle) {
            //  This is only returned if the key loaded is null
            return null;
        }
    }

    @Override
    public <T> BiFunction<String, Class<T>, T> deserializer(Class<T> clazz) {
        return this.supplier.deserializer(clazz);
    }

    @Override
    public <T> Function<T, String> serializer() {
        return this.supplier.serializer();
    }
}
