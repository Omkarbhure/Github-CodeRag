package com.example.coderag.exception;

public class GeminiQuotaExceededException extends RuntimeException {

    public GeminiQuotaExceededException(long dailyLimit) {
        super(String.format("Daily AI quota reached (%d calls/day). Please try again tomorrow.", dailyLimit));
    }

    public GeminiQuotaExceededException(String message) {
        super(message);
    }
}
