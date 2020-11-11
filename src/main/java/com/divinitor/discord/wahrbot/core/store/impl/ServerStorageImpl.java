package com.divinitor.discord.wahrbot.core.store.impl;

import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.dv8tion.jda.api.entities.Guild;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerStorageImpl implements ServerStorage {

    private final ReadWriteLock lock;
    private final TLongObjectMap<WeakReference<ServerStoreImpl>> cache;
    private final Injector injector;

    @Inject
    public ServerStorageImpl(Injector injector) {
        this.injector = injector;
        this.lock = new ReentrantReadWriteLock();
        this.cache = new TLongObjectHashMap<>();
    }

    @Override
    public ServerStore forServer(Guild server) {
        WeakReference<ServerStoreImpl> ret;
        ServerStoreImpl rett;
        try (Lockable rl = Lockable.acquire(this.lock.readLock())) {
            ret = this.cache.get(server.getIdLong());
        }

        if (ret == null || ret.get() == null) {
            try (Lockable wl = Lockable.acquire(this.lock.writeLock())) {
                //  Re-check
                ret = this.cache.get(server.getIdLong());
                if (ret == null || ret.get() == null) {
                    rett = new ServerStoreImpl(server, this.injector);
                    this.injector.injectMembers(rett);
                    ret = new WeakReference<>(rett);
                    this.cache.put(server.getIdLong(), ret);
                } else {
                    rett = ret.get();
                }
            }
        } else {
            rett = ret.get();
        }

        return rett;
    }
}
