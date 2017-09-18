package com.divinitor.discord.wahrbot.core.util.inject;

import redis.clients.jedis.Jedis;

import java.util.function.Supplier;

public class JedisProvider {

    private final Supplier<Jedis> supplier;

    public JedisProvider(Supplier<Jedis> supplier) {
        this.supplier = supplier;
    }

    public Jedis get() {
        return this.supplier.get();
    }
}
