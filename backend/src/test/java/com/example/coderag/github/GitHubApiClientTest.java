package com.example.coderag.github;

import com.example.coderag.exception.RepositoryValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubApiClientTest {

    private GitHubApiClient gitHubApiClient;

    @BeforeEach
    void setUp() {
        GitHubConfig config = new GitHubConfig();
        gitHubApiClient = new GitHubApiClient(config, new ObjectMapper());
    }

    @Test
    void parseUrl_ShouldExtractOwnerAndNameFromValidUrls() {
        GitHubApiClient.ParsedRepoUrl r1 = gitHubApiClient.parseUrl("https://github.com/spring-projects/spring-boot");
        assertEquals("spring-projects", r1.owner());
        assertEquals("spring-boot", r1.name());
        assertEquals("spring-projects/spring-boot", r1.fullName());

        GitHubApiClient.ParsedRepoUrl r2 = gitHubApiClient.parseUrl("http://www.github.com/facebook/react.git");
        assertEquals("facebook", r2.owner());
        assertEquals("react", r2.name());

        GitHubApiClient.ParsedRepoUrl r3 = gitHubApiClient.parseUrl("github.com/torvalds/linux/");
        assertEquals("torvalds", r3.owner());
        assertEquals("linux", r3.name());
    }

    @Test
    void parseUrl_ShouldRejectInvalidUrls() {
        assertThrows(RepositoryValidationException.class, () -> gitHubApiClient.parseUrl(""));
        assertThrows(RepositoryValidationException.class, () -> gitHubApiClient.parseUrl("https://gitlab.com/owner/repo"));
        assertThrows(RepositoryValidationException.class, () -> gitHubApiClient.parseUrl("https://github.com/onlyowner"));
        assertThrows(RepositoryValidationException.class, () -> gitHubApiClient.parseUrl("not-a-url"));
    }
}
