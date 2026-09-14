package com.example.coderag.dto;

import com.example.coderag.model.RepositoryFile;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RepositoryFileDto {

    private UUID id;
    private UUID repositoryId;
    private String filePath;
    private String language;
    private Long sizeBytes;
    private Boolean skipped;
    private String skipReason;
    private OffsetDateTime createdAt;

    public RepositoryFileDto() {
    }

    public RepositoryFileDto(UUID id, UUID repositoryId, String filePath, String language, Long sizeBytes, Boolean skipped, String skipReason, OffsetDateTime createdAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.filePath = filePath;
        this.language = language;
        this.sizeBytes = sizeBytes;
        this.skipped = skipped;
        this.skipReason = skipReason;
        this.createdAt = createdAt;
    }

    public static RepositoryFileDto fromEntity(RepositoryFile file) {
        if (file == null) {
            return null;
        }
        return new RepositoryFileDto(
                file.getId(),
                file.getRepositoryId(),
                file.getFilePath(),
                file.getLanguage(),
                file.getSizeBytes(),
                file.getSkipped(),
                file.getSkipReason(),
                file.getCreatedAt()
        );
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Boolean getSkipped() {
        return skipped;
    }

    public void setSkipped(Boolean skipped) {
        this.skipped = skipped;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
