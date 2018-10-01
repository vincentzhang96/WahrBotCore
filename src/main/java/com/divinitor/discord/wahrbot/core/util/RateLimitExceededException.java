package com.divinitor.discord.wahrbot.core.util;


public class RateLimitExceededException extends Exception {

    private long retryIn;

    public RateLimitExceededException() {
        super();
    }

    public RateLimitExceededException(String msg) {
        super(msg);
    }

    public RateLimitExceededException(long retryIn) {
        super("Retry in " + retryIn);
        this.retryIn = retryIn;
    }

    public RateLimitExceededException(String msg, long retryIn) {
        super("Retry in " + retryIn + ": " + msg);
        this.retryIn = retryIn;
    }

    public long getRetryIn() {
        return retryIn;
    }
}
