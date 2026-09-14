package com.example.coderag.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitHubConfig {

    @Value("${app.github.api-base-url:https://api.github.com}")
    private String apiBaseUrl;

    @Value("${app.github.token:}")
    private String token;

    @Value("${app.github.max-repo-size-mb:100}")
    private long maxRepoSizeMb;

    @Value("${app.github.max-file-size-mb:10}")
    private long maxFileSizeMb;

    @Value("${app.storage.repo-base-path:./data/repos}")
    private String repoBasePath;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getToken() {
        return token;
    }

    public long getMaxRepoSizeMb() {
        return maxRepoSizeMb;
    }

    public long getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public String getRepoBasePath() {
        return repoBasePath;
    }
}
