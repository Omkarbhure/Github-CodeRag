package com.example.coderag.exception;

public class RateLimitException extends RuntimeException {

    private final String action;
    private final long retryAfterSeconds;

    public RateLimitException(String action, long retryAfterSeconds) {
        super(String.format("Rate limit exceeded for %s. Please retry in %d seconds.", action, retryAfterSeconds));
        this.action = action;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getAction() {
        return action;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
