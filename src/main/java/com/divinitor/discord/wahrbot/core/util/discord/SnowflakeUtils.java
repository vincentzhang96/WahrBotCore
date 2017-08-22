package com.divinitor.discord.wahrbot.core.util.discord;

import net.dv8tion.jda.core.entities.ISnowflake;

public class SnowflakeUtils {

    public static final long DISCORD_EPOCH_MS = 1420070400000L;

    public static final String CODEX = "0123456789ABCDEFGH_JKLMN=PQRSTUVWXYZabcdefghijk-mnopqrstuvwxyz";
    public static final int BASE = CODEX.length();

    private SnowflakeUtils() {
    }

    public static long getTimestampMs(long snowflake) {
        //  Shift out low bits
        snowflake = snowflake >>> 22;
        //  Add Discord epoch
        snowflake += DISCORD_EPOCH_MS;

        return snowflake;
    }

    public static long getTimestampMs(String snowflake) {
        return getTimestampMs(Long.parseUnsignedLong(snowflake));
    }

    public static String encode(long snowflake) {
        long accum = snowflake;
        StringBuilder builder = new StringBuilder();
        long remainder;
        while (Long.compareUnsigned(accum, 0) > 0) {
            long last = accum;
            accum = accum / BASE;
            remainder = last - (accum * BASE);
            builder.append(digitToChar(Math.abs((int) remainder)));
        }
        String ret = builder.reverse().toString();

        //  Trim leading zeros
        int idx = 0;
        int length = ret.length();
        while (idx < length && ret.charAt(idx) == '0') {
            ++idx;
        }

        ret = ret.substring(idx);

        if (ret.isEmpty()) {
            ret = "0";
        }

        //  Format
        ret = "$" + ret;

        return ret;
    }

    public static String encode(String snowflake) {
        return encode(Long.parseUnsignedLong(snowflake));
    }

    public static long decode(String encodedSnowflake) {
        if (!encodedSnowflake.startsWith("$")) {
            throw new IllegalArgumentException("Not a valid encoded snowflake: " + encodedSnowflake);
        }
        encodedSnowflake = encodedSnowflake.substring(1);
        return parse(encodedSnowflake);
    }

    public static String decodeToString(String encodedSnowflake) {
        return Long.toUnsignedString(decode(encodedSnowflake));
    }

    private static long parse(String r62) {
        char[] chars = r62.toCharArray();
        int length = chars.length;
        long ret = 0;
        //  Read the number from right to left (smallest place to largest)
        //  WARNING: NO UNDER/OVERFLOW CHECKING IS DONE
        int lenLessOne = length - 1;
        for (int i = lenLessOne; i >= 0; i--) {
            long digit = charToDigit(chars[i]);
            int placeValue = lenLessOne - i;
            long addnum = digit * (long) Math.pow(BASE, placeValue);
            ret += addnum;
        }
        return ret;
    }

    private static char digitToChar(int i) {
        return CODEX.charAt(i);
    }

    private static int charToDigit(char c) {
        return CODEX.indexOf(c);
    }
}
