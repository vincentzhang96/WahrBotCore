package com.divinitor.discord.wahrbot.core.command;

import lombok.Getter;
import lombok.Setter;

import java.util.NoSuchElementException;
import java.util.Objects;

@Getter
@Setter
public class CommandLine {

    private String line;

    /**
     * Constructs a new CommandLine instance with the given input.
     * @param line The input to parse
     */
    public CommandLine(String line) {
        this.line = Objects.requireNonNull(line);
    }

    /**
     * Removes a prefix. If the prefix is present, it is removed, otherwise nothing is changed.
     * @param prefix The optional prefix to remove, if present
     */
    public void takeOptionalPrefix(String prefix) {
        if (this.line.toLowerCase().startsWith(prefix.toLowerCase())) {
            this.line = this.line.substring(prefix.length());
        }
    }

    /**
     * Attempts to remove a prefix. If the prefix is present, it is removed.
     * @param prefix The prefix to remove, if present
     * @return True if the prefix was present and removed, false otherwise
     */
    public boolean hasPrefixAndTake(String prefix) {
        if (this.line.toLowerCase().startsWith(prefix.toLowerCase())) {
            this.line = this.line.substring(prefix.length());
            return true;
        }

        return false;
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

    /**
     * Takes the next token from the command line. Respects quoted strings and escaped whitespace.
     * @return
     */
    public String next() {
        skipWhitespace();
        if (this.line.isEmpty()) {
            throw new NoSuchElementException("End of command line");
        }

        if (line.startsWith("\"")) {
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

    public String peek() {
        String s = this.line;
        try {
            return next();
        } catch (NoSuchElementException nsee) {
            return "";
        } finally {
            this.line = s;
        }
    }

    public boolean hasNext() {
        return !this.line.isEmpty();
    }

    public String remainder() {
        skipWhitespace();
        String ret = this.line;
        this.line = "";
        return ret;
    }

    public CommandLine copy() {
        return new CommandLine(this.line);
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
