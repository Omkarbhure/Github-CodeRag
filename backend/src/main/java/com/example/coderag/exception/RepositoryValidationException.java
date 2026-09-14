package com.example.coderag.exception;

public class RepositoryValidationException extends RuntimeException {
    public RepositoryValidationException(String message) {
        super(message);
    }
}
