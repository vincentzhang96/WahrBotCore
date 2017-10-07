package com.divinitor.discord.wahrbot.core.command;

import org.junit.*;

import java.util.NoSuchElementException;

public class CommandLineTest {

    @Test
    public void hasPrefixAndTake() {
        CommandLine line = new CommandLine(".dn fd 100k");
        Assert.assertTrue(line.hasPrefixAndTake("."));
        Assert.assertEquals("dn fd 100k", line.getLine());
    }

    @Test
    public void hasPrefixAndTakeMulti() {
        CommandLine line = new CommandLine(".o3odn fd 100k");
        Assert.assertTrue(line.hasPrefixAndTake(".o3o"));
        Assert.assertEquals("dn fd 100k", line.getLine());
    }

    @Test
    public void hasPrefixAndTakeSpace() {
        CommandLine line = new CommandLine(".o3odn fd 100k");
        Assert.assertTrue(line.hasPrefixAndTake(".o3odn"));
        Assert.assertEquals(" fd 100k", line.getLine());
    }

    @Test
    public void hasPrefixAndTakeNot() {
        CommandLine line = new CommandLine(".o3odn fd 100k");
        Assert.assertFalse(line.hasPrefixAndTake(".o3odq"));
        Assert.assertEquals(".o3odn fd 100k", line.getLine());
    }

    @Test
    public void takeOptionalPrefix() {
        CommandLine line = new CommandLine(".dn fd 100k");
        line.takeOptionalPrefix(".");
        Assert.assertEquals("dn fd 100k", line.getLine());
    }

    @Test
    public void takeOptionalPrefixMissing() {
        CommandLine line = new CommandLine("dn fd 100k");
        line.takeOptionalPrefix(".");
        Assert.assertEquals("dn fd 100k", line.getLine());
    }

    @Test
    public void takeOptionalPrefixMulti() {
        CommandLine line = new CommandLine(".dno3o fd 100k");
        line.takeOptionalPrefix(".dno3o");
        Assert.assertEquals(" fd 100k", line.getLine());
    }

    @Test
    public void next() {
        CommandLine line = new CommandLine(".dn first second third");
        Assert.assertEquals(".dn", line.next());
        Assert.assertEquals("first second third", line.getLine());

        Assert.assertEquals("first", line.next());
        Assert.assertEquals("second third", line.getLine());

        Assert.assertEquals("second", line.next());
        Assert.assertEquals("third", line.getLine());

        Assert.assertEquals("third", line.next());
        Assert.assertEquals("", line.getLine());
    }

    @Test
    public void nextQuoted() {
        CommandLine line = new CommandLine(".dn \"first second\" third");
        Assert.assertEquals(".dn", line.next());
        Assert.assertEquals("\"first second\" third", line.getLine());

        Assert.assertEquals("first second", line.next());
        Assert.assertEquals("third", line.getLine());

        Assert.assertEquals("third", line.next());
        Assert.assertEquals("", line.getLine());
    }

    @Test
    public void nextEscaped() {
        CommandLine line = new CommandLine(".dn first\\ second third");
        Assert.assertEquals(".dn", line.next());
        Assert.assertEquals("first\\ second third", line.getLine());

        Assert.assertEquals("first second", line.next());
        Assert.assertEquals("third", line.getLine());

        Assert.assertEquals("third", line.next());
        Assert.assertEquals("", line.getLine());
    }

    @Test
    public void nextNewline() {
        CommandLine line = new CommandLine(".dn first\nsecond\nthird");
        Assert.assertEquals(".dn", line.next());
        Assert.assertEquals("first\nsecond\nthird", line.getLine());

        Assert.assertEquals("first", line.next());
        Assert.assertEquals("second\nthird", line.getLine());

        Assert.assertEquals("second", line.next());
        Assert.assertEquals("third", line.getLine());

        Assert.assertEquals("third", line.next());
        Assert.assertEquals("", line.getLine());
    }


    @Test
    public void nextOpenQuoted() {
        CommandLine line = new CommandLine(".dn \"first second third");
        Assert.assertEquals(".dn", line.next());
        Assert.assertEquals("\"first second third", line.getLine());

        Assert.assertEquals("first second third", line.next());
        Assert.assertEquals("", line.getLine());
    }

    @Test(expected = NoSuchElementException.class)
    public void nextEndOfLine() {
        CommandLine line = new CommandLine("");
        line.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void nextEndOfLineSpaced() {
        CommandLine line = new CommandLine(" \t");
        line.next();
    }

    @Test
    public void remainder() {
        CommandLine line = new CommandLine(".dn first second third");
        line.next();
        Assert.assertEquals("first second third", line.remainder());
        Assert.assertEquals("", line.getLine());
    }

    @Test
    public void remainderEnd() {
        CommandLine line = new CommandLine("a");
        line.next();
        Assert.assertEquals("", line.remainder());
        Assert.assertEquals("", line.remainder());
    }

    @Test
    public void peek() {
        CommandLine line = new CommandLine(".dn first second third");
        Assert.assertEquals(".dn", line.peek());
        Assert.assertEquals(".dn", line.peek());

        line.next();
        Assert.assertEquals("first", line.peek());
        Assert.assertEquals("first", line.peek());
    }

    @Test
    public void peekLast() {
        CommandLine line = new CommandLine("a");
        line.next();
        Assert.assertEquals("", line.peek());
    }
}
