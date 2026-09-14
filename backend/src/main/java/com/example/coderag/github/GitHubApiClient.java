package com.example.coderag.github;

import com.example.coderag.exception.GitHubApiException;
import com.example.coderag.exception.GitHubRateLimitException;
import com.example.coderag.exception.RepositoryValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?/?$"
    );

    private final RestClient restClient;
    private final GitHubConfig gitHubConfig;
    private final ObjectMapper objectMapper;

    public GitHubApiClient(GitHubConfig gitHubConfig, ObjectMapper objectMapper) {
        this.gitHubConfig = gitHubConfig;
        this.objectMapper = objectMapper;

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(gitHubConfig.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "GitHub-CodeRAG-Application");

        if (StringUtils.hasText(gitHubConfig.getToken())) {
            builder.defaultHeader("Authorization", "Bearer " + gitHubConfig.getToken().trim());
        }

        this.restClient = builder.build();
    }

    public record ParsedRepoUrl(String owner, String name, String fullName) {}

    public record GitHubRepoMetadata(
            String owner,
            String name,
            String fullName,
            String defaultBranch,
            Long sizeKb,
            boolean isPrivate,
            String htmlUrl
    ) {}

    public ParsedRepoUrl parseUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new RepositoryValidationException("GitHub repository URL cannot be empty");
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new RepositoryValidationException("Invalid GitHub URL format. Expected: https://github.com/owner/repository");
        }

        String owner = matcher.group(1);
        String name = matcher.group(2);
        String fullName = owner + "/" + name;
        return new ParsedRepoUrl(owner, name, fullName);
    }

    public GitHubRepoMetadata getRepoMetadata(String owner, String name) {
        log.info("Fetching GitHub repository metadata for {}/{}", owner, name);

        try {
            String json = restClient.get()
                    .uri("/repos/{owner}/{name}", owner, name)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int code = response.getStatusCode().value();
                        if (code == 404) {
                            throw new GitHubApiException("Repository not found or is private: " + owner + "/" + name, 404);
                        }
                        if (code == 403) {
                            throw new GitHubRateLimitException("GitHub API rate limit exceeded or access denied");
                        }
                        throw new GitHubApiException("GitHub API error: " + response.getStatusText(), code);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new GitHubApiException("GitHub service is currently unavailable", response.getStatusCode().value());
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(json);
            boolean isPrivate = root.path("private").asBoolean(false);
            if (isPrivate) {
                throw new RepositoryValidationException("Repository is private. Only public repositories are supported in this phase.");
            }

            String repoOwner = root.path("owner").path("login").asText(owner);
            String repoName = root.path("name").asText(name);
            String fullName = root.path("full_name").asText(repoOwner + "/" + repoName);
            String defaultBranch = root.path("default_branch").asText("main");
            long sizeKb = root.path("size").asLong(0L);
            String htmlUrl = root.path("html_url").asText("https://github.com/" + fullName);

            return new GitHubRepoMetadata(repoOwner, repoName, fullName, defaultBranch, sizeKb, isPrivate, htmlUrl);

        } catch (GitHubApiException | GitHubRateLimitException | RepositoryValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch repository metadata for {}/{}: {}", owner, name, e.getMessage());
            throw new GitHubApiException("Failed to communicate with GitHub API: " + e.getMessage(), 500);
        }
    }

    public String getLatestCommitSha(String owner, String name, String branch) {
        log.info("Fetching latest commit SHA for {}/{} on branch {}", owner, name, branch);

        try {
            String json = restClient.get()
                    .uri("/repos/{owner}/{name}/commits/{branch}", owner, name, branch)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int code = response.getStatusCode().value();
                        if (code == 404) {
                            throw new GitHubApiException("Branch or commit not found: " + branch, 404);
                        }
                        if (code == 403) {
                            throw new GitHubRateLimitException("GitHub API rate limit exceeded");
                        }
                        throw new GitHubApiException("GitHub API error fetching commit: " + response.getStatusText(), code);
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(json);
            String sha = root.path("sha").asText();
            if (!StringUtils.hasText(sha)) {
                throw new GitHubApiException("Unable to find commit SHA for repository: " + owner + "/" + name, 500);
            }
            return sha;

        } catch (GitHubApiException | GitHubRateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch commit SHA for {}/{}: {}", owner, name, e.getMessage());
            throw new GitHubApiException("Failed to retrieve latest commit SHA from GitHub: " + e.getMessage(), 500);
        }
    }
}
