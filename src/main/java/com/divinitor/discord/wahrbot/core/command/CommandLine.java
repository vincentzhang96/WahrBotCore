package com.divinitor.discord.wahrbot.core.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.NoSuchElementException;

@Getter
@Setter
@AllArgsConstructor
public class CommandLine {

    private String line;

    public void takeOptionalPrefix(String prefix) {
        if (this.line.toLowerCase().startsWith(prefix.toLowerCase())) {
            this.line = this.line.substring(prefix.length());
        }
    }

    public boolean hasPrefixAndTake(String prefix) {
        if (this.line.toLowerCase().startsWith(prefix.toLowerCase())) {
            this.line = this.line.substring(prefix.length());
            return true;
        }

        return false;
    }

    private void skipWhitespace() {
        //  Skip whitespace
        int startIdx = 0;
        while (Character.isWhitespace(this.line.charAt(startIdx))) {
            ++startIdx;
        }

        this.line = this.line.substring(startIdx);
    }

    public String next() {
        skipWhitespace();
        if (this.line.isEmpty()) {
            throw new NoSuchElementException("End of command line");
        }



    }

    public String peek() {
        String s = this.line;
        try {
            this.skipWhitespace();
            

        } finally {
            this.line = s;
        }
    }

    public String remainder() {
        skipWhitespace();
        String ret = this.line;
        this.line = "";
        return ret;
    }
}
