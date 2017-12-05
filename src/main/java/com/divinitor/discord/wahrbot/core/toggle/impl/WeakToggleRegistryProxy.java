package com.divinitor.discord.wahrbot.core.toggle.impl;

import com.divinitor.discord.wahrbot.core.toggle.Toggle;
import com.divinitor.discord.wahrbot.core.toggle.ToggleRegistry;
import com.divinitor.discord.wahrbot.core.toggle.ToggleState;
import lombok.Getter;

import java.lang.ref.WeakReference;

public class WeakToggleRegistryProxy implements ToggleRegistry {

    @Getter
    private WeakReference<ToggleRegistry> registry;
    private final NoOpToggle toggleInstance;

    public WeakToggleRegistryProxy(ToggleRegistry registry) {
        toggleInstance = new NoOpToggle();
        this.setBaseRegistry(registry);
    }

    public void setBaseRegistry(ToggleRegistry registry) {
        this.registry = new WeakReference<>(registry);
    }

    @Override
    public Toggle getToggle(String key) {
        ToggleRegistry base = this.registry.get();
        if (base != null) {
            return base.getToggle(key);
        }

        return this.toggleInstance;
    }

    @Override
    public Toggle addChildToggle(Toggle parent, String key) {
        ToggleRegistry base = this.registry.get();
        if (base != null) {
            return base.addChildToggle(parent, key);
        }

        return this.toggleInstance;
    }

    class NoOpToggle implements Toggle {

        @Override
        public Toggle getParent() {
            return null;
        }

        @Override
        public ToggleState getState() {
            return ToggleState.UNSET;
        }

        @Override
        public void setState(ToggleState state) {

        }

        @Override
        public ToggleRegistry getRegistry() {
            return WeakToggleRegistryProxy.this;
        }
    }
}
