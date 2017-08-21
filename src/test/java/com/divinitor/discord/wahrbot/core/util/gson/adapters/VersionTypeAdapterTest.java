package com.divinitor.discord.wahrbot.core.util.gson.adapters;

import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import org.junit.*;

public class VersionTypeAdapterTest {
    @Test
    public void write() throws Exception {
        TestClass testClass = new TestClass();
        testClass.version = Version.forIntegers(0, 1, 4);

        Gson gson = StandardGson.pretty();

        String res = gson.toJson(testClass);

        Assert.assertTrue(res.contains("\"0.1.4\""));
    }

    @Test
    public void read() throws Exception {
        String test = "{\"version\": \"0.1.4\"}";

        Gson gson = StandardGson.pretty();

        TestClass testClass = gson.fromJson(test, TestClass.class);

        Assert.assertEquals(Version.forIntegers(0, 1, 4),
            testClass.version);
    }

    public static class TestClass {
        public Version version;
    }

}
