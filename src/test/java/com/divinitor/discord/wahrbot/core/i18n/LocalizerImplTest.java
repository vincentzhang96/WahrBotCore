package com.divinitor.discord.wahrbot.core.i18n;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class LocalizerImplTest {

    @Test
    public void testNestedEscapes() throws Exception {
        Localizer localizer = new LocalizerImpl();
        localizer.registerBundle("test", new Bundle());
        Assert.assertEquals("Foo [baz]", localizer.localize("test.foo"));
        Assert.assertEquals("Foo Foo [baz]", localizer.localize("test.afoo"));
    }

    @Test
    public void testNamedParams() throws Exception {
        Localizer localizer = new LocalizerImpl();
        localizer.registerBundle("test", new Bundle());
        Map<String, Object> named = new HashMap<>();
        named.put("POTATO", "potato");
        named.put("TOP", (Supplier<String>)() -> "cancer");

        Assert.assertEquals("potato cancer toast", localizer.localize("test.named", "toast", named));

        named.put("CARROT", 1000);
        Assert.assertEquals("1,000 carrots", localizer.localize("test.named.plural", named));
        named.put("CARROT", 1);
        Assert.assertEquals("1 carrot", localizer.localize("test.named.plural", named));
    }

    @Test
    public void testParentRef() throws Exception {
        Localizer localizer = new LocalizerImpl();
        localizer.registerBundle("test", new Bundle());

        Assert.assertEquals("Foo BFoo", localizer.localize("test.bfoo.foo"));
    }

    class Bundle implements LocalizerBundle {

        private Map<String, String> values;

        public Bundle() {
            values = new HashMap<>();
            values.put("test.foo", "Foo [.bar]");
            values.put("test.foo.bar", "\\[baz]");
            values.put("test.afoo", "Foo [.bar]");
            values.put("test.afoo.bar", "Foo [.baz]");
            values.put("test.afoo.bar.baz", "\\[baz]");
            values.put("test.named", "{%POTATO%} {%TOP%} {0}");
            values.put("test.named.plural", "{%CARROT%|%,d} {%CARROT%|(EN_PLURAL;carrots),(ONE;carrot)}");
            values.put("test.bfoo", "BFoo");
            values.put("test.bfoo.foo", "Foo [..]");
        }

        @Override
        public String get(String key, Locale locale) {
            return values.get(key);
        }

        @Override
        public boolean contains(String key, Locale locale) {
            return values.containsKey(key);
        }
    }

}
