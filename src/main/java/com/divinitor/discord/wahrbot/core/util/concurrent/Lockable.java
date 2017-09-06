package com.divinitor.discord.wahrbot.core.util.concurrent;

import java.util.concurrent.locks.Lock;

public interface Lockable extends AutoCloseable {

    void close();

    static Lockable acquire(Lock lock) {
        lock.lock();
        return lock::unlock;
    }
}
