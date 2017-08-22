package com.divinitor.discord.wahrbot.core.util.discord;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class SnowflakeUtilsTest {

    private final long snowflake = 90844803180273664L;
    private final String snowflakeStr = "90844803180273664";
    private final String encodedSnowflake = "$6i4Lheb0Ns";

    @Test
    public void encode() throws Exception {
        String encoded = SnowflakeUtils.encode(snowflake);
        Assert.assertEquals(encodedSnowflake, encoded);
    }

    @Test
    public void encode1() throws Exception {
        String encoded = SnowflakeUtils.encode(snowflakeStr);
        Assert.assertEquals(encodedSnowflake, encoded);
    }

    @Test
    public void decode() throws Exception {
        long decoded = SnowflakeUtils.decode(encodedSnowflake);
        Assert.assertEquals(snowflake, decoded);
    }

    @Test
    public void decodeToString() throws Exception {
        String decoded = SnowflakeUtils.decodeToString(encodedSnowflake);
        Assert.assertEquals(snowflakeStr, decoded);
    }

    @Test
    public void identity() throws Exception {
        //  Test a bunch of IDs
        for(long l = 0; l < 0xFFFFFFFFL; l += 0x1A73) {
            long flake = snowflake + l;
            Assert.assertEquals(flake, SnowflakeUtils.decode(SnowflakeUtils.encode(flake)));
        }
    }

}
