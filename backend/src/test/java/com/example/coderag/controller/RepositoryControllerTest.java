package com.example.coderag.controller;

import com.example.coderag.dto.ImportRepositoryRequest;
import com.example.coderag.dto.PageResponse;
import com.example.coderag.dto.RepositoryDetailDto;
import com.example.coderag.dto.RepositoryFileDto;
import com.example.coderag.dto.RepositorySummaryDto;
import com.example.coderag.model.AuthProvider;
import com.example.coderag.model.IndexingStatus;
import com.example.coderag.model.User;
import com.example.coderag.security.CookieService;
import com.example.coderag.security.JwtService;
import com.example.coderag.security.UserPrincipal;
import com.example.coderag.service.RepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepositoryService repositoryService;

    private UserPrincipal testUserPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .authProvider(AuthProvider.LOCAL)
                .build();
        testUserPrincipal = UserPrincipal.create(user);
    }

    @Test
    void importRepository_ShouldReturn202_WhenValidUrl() throws Exception {
        ImportRepositoryRequest request = new ImportRepositoryRequest("https://github.com/owner/project");
        UUID repoId = UUID.randomUUID();

        RepositorySummaryDto summary = RepositorySummaryDto.builder()
                .id(repoId)
                .owner("owner")
                .name("project")
                .fullName("owner/project")
                .defaultBranch("main")
                .latestCommitSha("abc1234")
                .url("https://github.com/owner/project")
                .sizeKb(1024L)
                .totalFiles(10L)
                .skippedFiles(0L)
                .status(IndexingStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .alreadyIndexed(false)
                .build();

        when(repositoryService.importRepository(eq(userId), any(ImportRepositoryRequest.class))).thenReturn(summary);

        mockMvc.perform(post("/api/repositories")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.fullName").value("owner/project"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void importRepository_ShouldReturn200_WhenAlreadyIndexed() throws Exception {
        ImportRepositoryRequest request = new ImportRepositoryRequest("https://github.com/owner/project");
        UUID repoId = UUID.randomUUID();

        RepositorySummaryDto summary = RepositorySummaryDto.builder()
                .id(repoId)
                .fullName("owner/project")
                .status(IndexingStatus.COMPLETED)
                .alreadyIndexed(true)
                .build();

        when(repositoryService.importRepository(eq(userId), any(ImportRepositoryRequest.class))).thenReturn(summary);

        mockMvc.perform(post("/api/repositories")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("owner/project"))
                .andExpect(jsonPath("$.alreadyIndexed").value(true));
    }

    @Test
    void importRepository_ShouldReturn400_WhenInvalidUrl() throws Exception {
        ImportRepositoryRequest request = new ImportRepositoryRequest("invalid-url");

        mockMvc.perform(post("/api/repositories")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserRepositories_ShouldReturn200AndList() throws Exception {
        RepositorySummaryDto summary = RepositorySummaryDto.builder()
                .id(UUID.randomUUID())
                .fullName("owner/project")
                .status(IndexingStatus.COMPLETED)
                .build();

        when(repositoryService.getUserRepositories(eq(userId))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/repositories")
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("owner/project"));
    }

    @Test
    void getRepositoryDetail_ShouldReturn200AndDetail() throws Exception {
        UUID repoId = UUID.randomUUID();
        RepositoryDetailDto detail = RepositoryDetailDto.builder()
                .id(repoId)
                .fullName("owner/project")
                .totalFiles(25L)
                .skippedFiles(2L)
                .keptFiles(23L)
                .status(IndexingStatus.COMPLETED)
                .build();

        when(repositoryService.getRepositoryDetail(eq(userId), eq(repoId))).thenReturn(detail);

        mockMvc.perform(get("/api/repositories/" + repoId)
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(25))
                .andExpect(jsonPath("$.keptFiles").value(23));
    }

    @Test
    void getRepositoryChunks_ShouldReturn200AndChunks() throws Exception {
        UUID repoId = UUID.randomUUID();
        com.example.coderag.dto.CodeChunkDto chunkDto = com.example.coderag.dto.CodeChunkDto.builder()
                .id(UUID.randomUUID())
                .repositoryId(repoId)
                .filePath("src/App.java")
                .chunkIndex(0)
                .startLine(1)
                .endLine(50)
                .content("public class App {}")
                .build();

        PageResponse<com.example.coderag.dto.CodeChunkDto> response = new PageResponse<>(
                List.of(chunkDto), 0, 50, 1L, 1, true, true
        );

        when(repositoryService.getRepositoryChunks(eq(userId), eq(repoId), eq(false), any())).thenReturn(response);

        mockMvc.perform(get("/api/repositories/" + repoId + "/chunks")
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].filePath").value("src/App.java"))
                .andExpect(jsonPath("$.content[0].startLine").value(1))
                .andExpect(jsonPath("$.content[0].endLine").value(50));
    }

    @Test
    void searchRepository_ShouldReturn200AndResults() throws Exception {
        UUID repoId = UUID.randomUUID();
        com.example.coderag.dto.SearchRequestDto request = new com.example.coderag.dto.SearchRequestDto("authentication jwt", 5);

        com.example.coderag.dto.SearchResultDto resultDto = com.example.coderag.dto.SearchResultDto.builder()
                .id(UUID.randomUUID())
                .repositoryId(repoId)
                .filePath("src/security/JwtService.java")
                .startLine(1)
                .endLine(60)
                .chunkIndex(0)
                .score(0.95)
                .vectorScore(0.90)
                .keywordScore(0.80)
                .finalScore(0.95)
                .content("public class JwtService { ... }")
                .build();

        when(repositoryService.searchRepository(eq(userId), eq(repoId), any())).thenReturn(List.of(resultDto));

        mockMvc.perform(post("/api/repositories/" + repoId + "/search")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filePath").value("src/security/JwtService.java"))
                .andExpect(jsonPath("$[0].score").value(0.95))
                .andExpect(jsonPath("$[0].vectorScore").value(0.90))
                .andExpect(jsonPath("$[0].keywordScore").value(0.80))
                .andExpect(jsonPath("$[0].finalScore").value(0.95));
    }

    @Test
    void getFileContent_ShouldReturn200AndContent() throws Exception {
        UUID repoId = UUID.randomUUID();
        String filePath = "src/main/java/App.java";
        String sampleCode = "package com.example;\npublic class App {}";

        when(repositoryService.getFileContent(eq(userId), eq(repoId), eq(filePath))).thenReturn(sampleCode);

        mockMvc.perform(get("/api/repositories/" + repoId + "/files/content")
                        .param("path", filePath)
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(status().is(200));
    }
}


