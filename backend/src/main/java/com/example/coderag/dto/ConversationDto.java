package com.example.coderag.dto;

import com.example.coderag.model.Conversation;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ConversationDto {

    private UUID id;
    private UUID repositoryId;
    private UUID userId;
    private String title;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ConversationDto() {
    }

    public ConversationDto(UUID id, UUID repositoryId, UUID userId, String title, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.userId = userId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ConversationDto fromEntity(Conversation entity) {
        if (entity == null) {
            return null;
        }
        return new ConversationDto(
                entity.getId(),
                entity.getRepositoryId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
