package com.example.coderag.dto;

import com.example.coderag.model.Message;
import com.example.coderag.model.MessageRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public class MessageDto {

    private UUID id;
    private UUID conversationId;
    private MessageRole role;
    private String content;
    private OffsetDateTime createdAt;

    public MessageDto() {
    }

    public MessageDto(UUID id, UUID conversationId, MessageRole role, String content, OffsetDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static MessageDto fromEntity(Message entity) {
        if (entity == null) {
            return null;
        }
        return new MessageDto(
                entity.getId(),
                entity.getConversationId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
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
