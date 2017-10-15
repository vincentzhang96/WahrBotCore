package com.divinitor.discord.wahrbot.core.store.impl;

import com.divinitor.discord.wahrbot.core.store.UserStorage;
import com.divinitor.discord.wahrbot.core.store.UserStore;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.dv8tion.jda.core.entities.User;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserStorageImpl implements UserStorage {

    private final ReadWriteLock lock;
    private final TLongObjectMap<WeakReference<UserStoreImpl>> cache;
    private final Injector injector;

    @Inject
    public UserStorageImpl(Injector injector) {
        this.injector = injector;
        this.lock = new ReentrantReadWriteLock();
        this.cache = new TLongObjectHashMap<>();
    }

    @Override
    public UserStore forUser(User user) {
        WeakReference<UserStoreImpl> ret;
        UserStoreImpl rett;
        try (Lockable rl = Lockable.acquire(this.lock.readLock())) {
            ret = this.cache.get(user.getIdLong());
        }

        if (ret == null || ret.get() == null) {
            try (Lockable wl = Lockable.acquire(this.lock.writeLock())) {
                //  Re-check
                ret = this.cache.get(user.getIdLong());
                if (ret == null || ret.get() == null) {
                    rett = new UserStoreImpl(user);
                    this.injector.injectMembers(rett);
                    ret = new WeakReference<>(rett);
                    this.cache.put(user.getIdLong(), ret);
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
