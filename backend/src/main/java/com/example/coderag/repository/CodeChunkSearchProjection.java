package com.example.coderag.repository;

import java.util.UUID;

public interface CodeChunkSearchProjection {
    UUID getId();
    UUID getRepositoryId();
    UUID getRepositoryFileId();
    String getFilePath();
    Integer getStartLine();
    Integer getEndLine();
    Integer getChunkIndex();
    String getCommitSha();
    String getContent();
    Double getScore();
}
