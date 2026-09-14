package com.example.coderag.model;

public enum IndexingStatus {
    PENDING,
    DOWNLOADING,
    SCANNING,
    CHUNKING,
    EMBEDDING,
    COMPLETED,
    FAILED,
    REJECTED_TOO_LARGE
}
