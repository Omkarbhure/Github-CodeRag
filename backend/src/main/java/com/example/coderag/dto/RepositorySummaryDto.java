package com.example.coderag.dto;

import com.example.coderag.model.IndexingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RepositorySummaryDto {

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
    private IndexingStatus status;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private Boolean alreadyIndexed;

    public RepositorySummaryDto() {
    }

    public RepositorySummaryDto(UUID id, String owner, String name, String fullName, String defaultBranch, String latestCommitSha, String url, Long sizeKb, Long totalFiles, Long skippedFiles, IndexingStatus status, String errorMessage, OffsetDateTime createdAt, Boolean alreadyIndexed) {
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
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.alreadyIndexed = alreadyIndexed;
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
        private IndexingStatus status;
        private String errorMessage;
        private OffsetDateTime createdAt;
        private Boolean alreadyIndexed = false;

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

        public Builder status(IndexingStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder alreadyIndexed(Boolean alreadyIndexed) {
            this.alreadyIndexed = alreadyIndexed;
            return this;
        }

        public RepositorySummaryDto build() {
            return new RepositorySummaryDto(id, owner, name, fullName, defaultBranch, latestCommitSha, url, sizeKb, totalFiles, skippedFiles, status, errorMessage, createdAt, alreadyIndexed);
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getAlreadyIndexed() {
        return alreadyIndexed;
    }

    public void setAlreadyIndexed(Boolean alreadyIndexed) {
        this.alreadyIndexed = alreadyIndexed;
    }
}
