package com.divinitor.discord.wahrbot.core.util.gson.adapters;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.*;

public class VersionTypeAdapterTest {
    @Test
    public void write() throws Exception {
        TestClass testClass = new TestClass();
        testClass.version = Version.forIntegers(0, 1, 4);

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();

        String res = gson.toJson(testClass);

        Assert.assertTrue(res.contains("\"0.1.4\""));
    }

    @Test
    public void read() throws Exception {
        String test = "{\"version\": \"0.1.4\"}";

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();

        TestClass testClass = gson.fromJson(test, TestClass.class);

        Assert.assertEquals(Version.forIntegers(0, 1, 4),
            testClass.version);
    }

    public static class TestClass {
        public Version version;
    }

}
