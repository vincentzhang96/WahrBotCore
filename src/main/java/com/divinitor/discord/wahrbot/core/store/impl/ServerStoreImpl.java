package com.divinitor.discord.wahrbot.core.store.impl;

import com.divinitor.discord.wahrbot.core.store.MemberStore;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServerStoreImpl implements ServerStore {

    private final Guild guild;
    private final Injector injector;
    private final ReadWriteLock lock;
    private final TLongObjectMap<WeakReference<MemberStoreImpl>> cache;
    @Inject
    private JedisProvider provider;

    public ServerStoreImpl(Guild guild, Injector injector) {
        this.guild = guild;
        this.injector = injector;
        this.lock = new ReentrantReadWriteLock();
        this.cache = new TLongObjectHashMap<>();
    }

    @Override
    public Guild getServer() {
        return this.guild;
    }

    @Override
    public MemberStore forMember(Member member) {
        WeakReference<MemberStoreImpl> ret;
        MemberStoreImpl rett;
        try (Lockable rl = Lockable.acquire(this.lock.readLock())) {
            ret = this.cache.get(member.getUser().getIdLong());
        }

        if (ret == null || ret.get() == null) {
            try (Lockable wl = Lockable.acquire(this.lock.writeLock())) {
                //  Re-check
                ret = this.cache.get(member.getUser().getIdLong());
                if (ret == null || ret.get() == null) {
                    rett = new MemberStoreImpl(member);
                    this.injector.injectMembers(rett);
                    ret = new WeakReference<>(rett);
                    this.cache.put(member.getUser().getIdLong(), ret);
                } else {
                    rett = ret.get();
                }
            }
        } else {
            rett = ret.get();
        }

        return rett;
    }

    @Override
    public void put(String key, String value) {

    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public <T> BiFunction<String, Class<T>, T> deserializer(Class<T> clazz) {
        return null;
    }

    @Override
    public <T> Function<T, String> serializer() {
        return null;
    }
}
