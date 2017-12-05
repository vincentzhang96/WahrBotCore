package com.divinitor.discord.wahrbot.core.toggle.impl;

import com.divinitor.discord.wahrbot.core.toggle.Toggle;
import com.divinitor.discord.wahrbot.core.toggle.ToggleRegistry;
import com.divinitor.discord.wahrbot.core.toggle.ToggleState;

import java.util.HashMap;
import java.util.Map;

public class MemoryTransientToggleRegistry implements ToggleRegistry {

    private final Map<String, ToggleImpl> toggles;

    public MemoryTransientToggleRegistry() {
        toggles = new HashMap<>();
    }

    @Override
    public Toggle getToggle(String key) {
        return toggles.computeIfAbsent(key, ToggleImpl::new);
    }

    @Override
    public Toggle addChildToggle(Toggle parent, String key) {
        ToggleImpl ret = (ToggleImpl) this.getToggle(key);
        ret.parent = parent;
        return ret;
    }


    class ToggleImpl implements Toggle {

        private ToggleState state;
        private transient Toggle parent;
        private final String key;

        public ToggleImpl(String key) {
            this.state = ToggleState.UNSET;
            this.parent = null;
            this.key = key;
        }

        @Override
        public Toggle getParent() {
            return this.parent;
        }

        @Override
        public ToggleState getState() {
            return this.state;
        }

        @Override
        public void setState(ToggleState state) {
            this.state = state;
        }

        @Override
        public ToggleRegistry getRegistry() {
            return MemoryTransientToggleRegistry.this;
        }
    }
}
