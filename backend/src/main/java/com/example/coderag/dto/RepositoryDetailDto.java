package com.example.coderag.dto;

import com.example.coderag.model.IndexingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RepositoryDetailDto {

    private UUID id;
    private String owner;
    private String name;
    private String fullName;
    private String defaultBranch;
    private String latestCommitSha;
    private String url;
    private Long sizeKb;
    private Long totalFiles;
    private Long skippedFiles;
    private Long keptFiles;
    private Long lowValueSkippedCount;
    private Long totalChunks;
    private Long embeddedChunkCount;
    private IndexingStatus status;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;

    public RepositoryDetailDto() {
    }

    public RepositoryDetailDto(UUID id, String owner, String name, String fullName, String defaultBranch,
                               String latestCommitSha, String url, Long sizeKb, Long totalFiles,
                               Long skippedFiles, Long keptFiles, Long lowValueSkippedCount, Long totalChunks,
                               Long embeddedChunkCount, IndexingStatus status, String errorMessage,
                               OffsetDateTime startedAt, OffsetDateTime completedAt, OffsetDateTime createdAt) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.fullName = fullName;
        this.defaultBranch = defaultBranch;
        this.latestCommitSha = latestCommitSha;
        this.url = url;
        this.sizeKb = sizeKb;
        this.totalFiles = totalFiles;
        this.skippedFiles = skippedFiles;
        this.keptFiles = keptFiles;
        this.lowValueSkippedCount = lowValueSkippedCount;
        this.totalChunks = totalChunks;
        this.embeddedChunkCount = embeddedChunkCount;
        this.status = status;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String owner;
        private String name;
        private String fullName;
        private String defaultBranch;
        private String latestCommitSha;
        private String url;
        private Long sizeKb;
        private Long totalFiles;
        private Long skippedFiles;
        private Long keptFiles;
        private Long lowValueSkippedCount;
        private Long totalChunks;
        private Long embeddedChunkCount;
        private IndexingStatus status;
        private String errorMessage;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder defaultBranch(String defaultBranch) {
            this.defaultBranch = defaultBranch;
            return this;
        }

        public Builder latestCommitSha(String latestCommitSha) {
            this.latestCommitSha = latestCommitSha;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder sizeKb(Long sizeKb) {
            this.sizeKb = sizeKb;
            return this;
        }

        public Builder totalFiles(Long totalFiles) {
            this.totalFiles = totalFiles;
            return this;
        }

        public Builder skippedFiles(Long skippedFiles) {
            this.skippedFiles = skippedFiles;
            return this;
        }

        public Builder keptFiles(Long keptFiles) {
            this.keptFiles = keptFiles;
            return this;
        }

        public Builder lowValueSkippedCount(Long lowValueSkippedCount) {
            this.lowValueSkippedCount = lowValueSkippedCount;
            return this;
        }

        public Builder totalChunks(Long totalChunks) {
            this.totalChunks = totalChunks;
            return this;
        }

        public Builder embeddedChunkCount(Long embeddedChunkCount) {
            this.embeddedChunkCount = embeddedChunkCount;
            return this;
        }

        public Builder status(IndexingStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder startedAt(OffsetDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(OffsetDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RepositoryDetailDto build() {
            return new RepositoryDetailDto(id, owner, name, fullName, defaultBranch, latestCommitSha, url,
                    sizeKb, totalFiles, skippedFiles, keptFiles, lowValueSkippedCount, totalChunks,
                    embeddedChunkCount, status, errorMessage, startedAt, completedAt, createdAt);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getLatestCommitSha() {
        return latestCommitSha;
    }

    public void setLatestCommitSha(String latestCommitSha) {
        this.latestCommitSha = latestCommitSha;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getSizeKb() {
        return sizeKb;
    }

    public void setSizeKb(Long sizeKb) {
        this.sizeKb = sizeKb;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public Long getSkippedFiles() {
        return skippedFiles;
    }

    public void setSkippedFiles(Long skippedFiles) {
        this.skippedFiles = skippedFiles;
    }

    public Long getKeptFiles() {
        return keptFiles;
    }

    public void setKeptFiles(Long keptFiles) {
        this.keptFiles = keptFiles;
    }

    public Long getLowValueSkippedCount() {
        return lowValueSkippedCount;
    }

    public void setLowValueSkippedCount(Long lowValueSkippedCount) {
        this.lowValueSkippedCount = lowValueSkippedCount;
    }

    public Long getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Long totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Long getEmbeddedChunkCount() {
        return embeddedChunkCount;
    }

    public void setEmbeddedChunkCount(Long embeddedChunkCount) {
        this.embeddedChunkCount = embeddedChunkCount;
    }

    public IndexingStatus getStatus() {
        return status;
    }

    public void setStatus(IndexingStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
