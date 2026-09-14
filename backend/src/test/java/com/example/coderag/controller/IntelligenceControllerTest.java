package com.example.coderag.controller;

import com.example.coderag.dto.ArchitectureOverviewDto;
import com.example.coderag.dto.BugInvestigationRequestDto;
import com.example.coderag.dto.BugInvestigationResponseDto;
import com.example.coderag.dto.RelatedFileDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.intelligence.ArchitectureOverviewService;
import com.example.coderag.intelligence.BugInvestigationService;
import com.example.coderag.intelligence.RelatedFilesService;
import com.example.coderag.model.AuthProvider;
import com.example.coderag.model.User;
import com.example.coderag.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
class IntelligenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArchitectureOverviewService architectureOverviewService;

    @MockBean
    private BugInvestigationService bugInvestigationService;

    @MockBean
    private RelatedFilesService relatedFilesService;

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
    void getArchitectureOverview_ShouldReturn200AndOverview() throws Exception {
        UUID repoId = UUID.randomUUID();
        ArchitectureOverviewDto dto = ArchitectureOverviewDto.builder()
                .id(UUID.randomUUID())
                .repositoryId(repoId)
                .commitSha("sha123")
                .overviewText("Spring Boot backend with Next.js frontend")
                .technologies(List.of("Java", "Spring Boot", "Next.js"))
                .modules(List.of("backend", "frontend"))
                .createdAt(OffsetDateTime.now())
                .build();

        when(architectureOverviewService.getOrGenerateOverview(eq(userId), eq(repoId), anyBoolean())).thenReturn(dto);

        mockMvc.perform(post("/api/repositories/" + repoId + "/architecture-overview")
                        .param("force", "false")
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overviewText").value("Spring Boot backend with Next.js frontend"))
                .andExpect(jsonPath("$.technologies[0]").value("Java"));
    }

    @Test
    void investigateBug_ShouldReturn200AndAnalysis() throws Exception {
        UUID repoId = UUID.randomUUID();
        BugInvestigationRequestDto request = new BugInvestigationRequestDto("NullPointerException at AuthService.java:42");

        BugInvestigationResponseDto responseDto = BugInvestigationResponseDto.builder()
                .analysis("Null check missing in AuthService [src/AuthService.java:40-50].")
                .identifiedFiles(List.of("AuthService.java"))
                .relevantChunks(List.of(
                        SearchResultDto.builder().filePath("src/AuthService.java").startLine(40).endLine(50).build()
                ))
                .build();

        when(bugInvestigationService.investigateBug(eq(userId), eq(repoId), any(BugInvestigationRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/repositories/" + repoId + "/investigate-bug")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("Null check missing in AuthService [src/AuthService.java:40-50]."))
                .andExpect(jsonPath("$.identifiedFiles[0]").value("AuthService.java"));
    }

    @Test
    void getRelatedFiles_ShouldReturn200AndList() throws Exception {
        UUID repoId = UUID.randomUUID();
        String path = "src/AuthService.java";

        RelatedFileDto related = RelatedFileDto.builder()
                .filePath("src/AuthController.java")
                .language("java")
                .relevanceScore(0.95)
                .reason("Directly references / imports AuthService")
                .build();

        when(relatedFilesService.findRelatedFiles(eq(userId), eq(repoId), eq(path), anyInt()))
                .thenReturn(List.of(related));

        mockMvc.perform(get("/api/repositories/" + repoId + "/files/related")
                        .param("path", path)
                        .param("topK", "10")
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filePath").value("src/AuthController.java"))
                .andExpect(jsonPath("$[0].relevanceScore").value(0.95))
                .andExpect(jsonPath("$[0].reason").value("Directly references / imports AuthService"));
    }
}
