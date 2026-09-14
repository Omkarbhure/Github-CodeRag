package com.example.coderag.dto;

import com.example.coderag.model.AuthProvider;
import com.example.coderag.model.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserDto {

    private UUID id;
    private String email;
    private String githubUsername;
    private AuthProvider authProvider;
    private OffsetDateTime createdAt;

    public UserDto() {
    }

    public UserDto(UUID id, String email, String githubUsername, AuthProvider authProvider, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.githubUsername = githubUsername;
        this.authProvider = authProvider;
        this.createdAt = createdAt;
    }

    public static UserDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getGithubUsername(),
                user.getAuthProvider(),
                user.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
