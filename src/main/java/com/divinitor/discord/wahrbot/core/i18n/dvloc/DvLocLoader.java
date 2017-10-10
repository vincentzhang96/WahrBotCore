package com.divinitor.discord.wahrbot.core.i18n.dvloc;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class DvLocLoader {

    private String line;

    public DvLocBundle read(String data) {
        this.line = data;
        return this.root();
    }

    private DvLocBundle root() {
        return null;
    }

    private void skipWhitespace() {
        if (this.line.isEmpty()) {
            return;
        }

        //  Skip whitespace
        int startIdx = 0;
        while (startIdx < this.line.length() && Character.isWhitespace(this.line.charAt(startIdx))) {
            ++startIdx;
        }

        this.line = this.line.substring(startIdx).trim();
    }

    public boolean take(String s) {
        skipWhitespace();
        if (this.line.startsWith(s)) {
            this.line = this.line.substring(s.length());
            return true;
        }

        return false;
    }

    public <T extends Throwable> void takeOrThrow(String s, Supplier<T> exception) throws T {
        if (!take(s)) {
            throw exception.get();
        }
    }

    public boolean hasNext() {
        return !this.line.isEmpty();
    }

    public String next() {
        skipWhitespace();
        if (this.line.isEmpty()) {
            throw new NoSuchElementException("End of command line");
        }

        if (this.line.startsWith("\"")) {
            int endIndex = consumeQuotedString(this.line);
            if (endIndex == -1) {
                String ret = this.line.substring(1);
                this.line = "";
                return ret;
            } else {
                String ret = this.line.substring(1, endIndex);
                this.line = this.line.substring(endIndex + 1).trim();
                return ret;
            }
        } else {
            StringBuilder builder = new StringBuilder();
            int end = this.line.length();
            boolean escaped = false;
            for (int i = 0; i < this.line.length(); i++) {
                char c = this.line.charAt(i);
                if (escaped) {
                    builder.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (Character.isWhitespace(c)) {
                    end = i;
                    break;
                } else {
                    builder.append(c);
                }
            }

            this.line = this.line.substring(end).trim();
            return builder.toString();
        }
    }

    private static int consumeQuotedString(String s) {
        //  We know it starts with a quote
        boolean escape = false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }

        //  unclosed quote but who cares
        return -1;
    }
}
