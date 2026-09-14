package com.example.coderag.exception;

public class GitHubRateLimitException extends RuntimeException {
    public GitHubRateLimitException(String message) {
        super(message);
    }
}
