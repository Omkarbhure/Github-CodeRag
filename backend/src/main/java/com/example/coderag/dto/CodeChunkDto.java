package com.example.coderag.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CodeChunkDto {

    private UUID id;
    private UUID repositoryId;
    private UUID repositoryFileId;
    private String filePath;
    private String commitSha;
    private Integer chunkIndex;
    private Integer startLine;
    private Integer endLine;
    private String content;
    private OffsetDateTime createdAt;

    public CodeChunkDto() {
    }

    public CodeChunkDto(UUID id, UUID repositoryId, UUID repositoryFileId, String filePath,
                        String commitSha, Integer chunkIndex, Integer startLine, Integer endLine,
                        String content, OffsetDateTime createdAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.repositoryFileId = repositoryFileId;
        this.filePath = filePath;
        this.commitSha = commitSha;
        this.chunkIndex = chunkIndex;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID repositoryId;
        private UUID repositoryFileId;
        private String filePath;
        private String commitSha;
        private Integer chunkIndex;
        private Integer startLine;
        private Integer endLine;
        private String content;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder repositoryId(UUID repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public Builder repositoryFileId(UUID repositoryFileId) {
            this.repositoryFileId = repositoryFileId;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder commitSha(String commitSha) {
            this.commitSha = commitSha;
            return this;
        }

        public Builder chunkIndex(Integer chunkIndex) {
            this.chunkIndex = chunkIndex;
            return this;
        }

        public Builder startLine(Integer startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder endLine(Integer endLine) {
            this.endLine = endLine;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CodeChunkDto build() {
            return new CodeChunkDto(id, repositoryId, repositoryFileId, filePath, commitSha, chunkIndex, startLine, endLine, content, createdAt);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(UUID repositoryId) {
        this.repositoryId = repositoryId;
    }

    public UUID getRepositoryFileId() {
        return repositoryFileId;
    }

    public void setRepositoryFileId(UUID repositoryFileId) {
        this.repositoryFileId = repositoryFileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
