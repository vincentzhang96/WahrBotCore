package com.divinitor.discord.wahrbot.core.util.inject;

import redis.clients.jedis.Jedis;

import java.util.function.Supplier;

/**
 * An injectable provider for Jedis
 */
public class JedisProvider {

    private final Supplier<Jedis> supplier;

    public JedisProvider(Supplier<Jedis> supplier) {
        this.supplier = supplier;
    }

    /**
     * Get a Jedis instance from the supplier pool
     * @return A Jedis instance
     */
    public Jedis get() {
        return this.supplier.get();
    }

    /**
     * @see {@link JedisProvider#get()}
     * @return A Jedis instance
     */
    public Jedis getResource() {
        return this.get();
    }
}
