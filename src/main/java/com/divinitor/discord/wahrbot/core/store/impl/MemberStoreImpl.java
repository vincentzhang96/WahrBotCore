package com.divinitor.discord.wahrbot.core.store.impl;

import com.divinitor.discord.wahrbot.core.store.MemberStore;
import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.inject.Inject;
import net.dv8tion.jda.core.entities.Member;

import java.util.function.BiFunction;
import java.util.function.Function;

public class MemberStoreImpl implements MemberStore {

    @Inject
    private JedisProvider provider;
    private final Member member;

    public MemberStoreImpl(Member member) {
        this.member = member;
    }

    @Override
    public Member getMember() {
        return this.member;
    }

    @Override
    public void purge() {

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
