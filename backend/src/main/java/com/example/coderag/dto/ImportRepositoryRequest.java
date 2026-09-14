package com.example.coderag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ImportRepositoryRequest {

    @NotBlank(message = "GitHub repository URL is required")
    @Pattern(
            regexp = "^(https?://)?(www\\.)?github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/?$",
            message = "Invalid GitHub URL format. Expected: https://github.com/owner/repository"
    )
    private String githubUrl;

    public ImportRepositoryRequest() {
    }

    public ImportRepositoryRequest(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }
}
