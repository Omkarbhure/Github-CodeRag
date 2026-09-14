package com.example.coderag.exception;

public class RepoTooLargeException extends RuntimeException {
    public RepoTooLargeException(String message) {
        super(message);
    }
}
