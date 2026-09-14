package com.example.coderag.intelligence;

import com.example.coderag.chat.AnswerGenerationService;
import com.example.coderag.chat.ContextBuilder;
import com.example.coderag.dto.BugInvestigationRequestDto;
import com.example.coderag.dto.BugInvestigationResponseDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import com.example.coderag.service.HybridSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BugInvestigationServiceTest {

    private GitHubRepositoryRepository repositoryRepository;
    private RepositoryFileRepository repositoryFileRepository;
    private CodeChunkRepository codeChunkRepository;
    private HybridSearchService hybridSearchService;
    private ContextBuilder contextBuilder;
    private AnswerGenerationService answerGenerationService;

    private BugInvestigationService bugInvestigationService;

    private UUID userId;
    private UUID repoId;
    private GitHubRepository testRepo;

    @BeforeEach
    void setUp() {
        repositoryRepository = Mockito.mock(GitHubRepositoryRepository.class);
        repositoryFileRepository = Mockito.mock(RepositoryFileRepository.class);
        codeChunkRepository = Mockito.mock(CodeChunkRepository.class);
        hybridSearchService = Mockito.mock(HybridSearchService.class);
        contextBuilder = new ContextBuilder();
        answerGenerationService = Mockito.mock(AnswerGenerationService.class);

        bugInvestigationService = new BugInvestigationService(
                repositoryRepository,
                repositoryFileRepository,
                codeChunkRepository,
                hybridSearchService,
                contextBuilder,
                answerGenerationService
        );

        userId = UUID.randomUUID();
        repoId = UUID.randomUUID();
        testRepo = GitHubRepository.builder().id(repoId).ownerId(userId).fullName("owner/repo").build();
    }

    @Test
    void parseStackTraceSignals_ShouldExtractJavaAndPythonLines() {
        String javaTrace = """
                java.lang.NullPointerException: Cannot invoke method
                    at com.example.service.AuthService.validateToken(AuthService.java:42)
                    at com.example.controller.AuthController.login(AuthController.java:25)
                """;

        Set<BugInvestigationService.ParsedSignal> signals = bugInvestigationService.parseStackTraceSignals(javaTrace);

        assertEquals(2, signals.size());
        assertTrue(signals.stream().anyMatch(s -> "AuthService.java".equals(s.fileName()) && s.line() == 42));
        assertTrue(signals.stream().anyMatch(s -> "AuthController.java".equals(s.fileName()) && s.line() == 25));
    }

    @Test
    void investigateBug_ShouldPrioritizeDirectFileMatchesFromStackTrace() {
        when(repositoryRepository.findByIdAndOwnerId(repoId, userId)).thenReturn(Optional.of(testRepo));

        String errorText = "NullPointerException at com.example.AuthService.login(AuthService.java:35)";
        BugInvestigationRequestDto request = new BugInvestigationRequestDto(errorText);

        UUID fileId = UUID.randomUUID();
        RepositoryFile file = RepositoryFile.builder().id(fileId).repositoryId(repoId).filePath("src/main/java/AuthService.java").build();
        CodeChunk chunk = CodeChunk.builder().id(UUID.randomUUID()).repositoryFileId(fileId).filePath("src/main/java/AuthService.java").startLine(1).endLine(50).content("public void login() {}").build();

        when(repositoryFileRepository.findByRepositoryId(repoId)).thenReturn(List.of(file));
        when(codeChunkRepository.findByRepositoryId(eq(repoId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(chunk)));
        when(hybridSearchService.search(eq(repoId), anyString(), anyInt())).thenReturn(List.of());

        when(answerGenerationService.generateAnswer(anyString(), anyString(), anyList()))
                .thenReturn("The bug is caused by null pointer in AuthService [src/main/java/AuthService.java:30-40].");

        BugInvestigationResponseDto response = bugInvestigationService.investigateBug(userId, repoId, request);

        assertNotNull(response);
        assertTrue(response.getIdentifiedFiles().contains("AuthService.java"));
        assertTrue(response.getAnalysis().contains("AuthService"));
        assertEquals(1, response.getRelevantChunks().size());
        assertEquals("src/main/java/AuthService.java", response.getRelevantChunks().get(0).getFilePath());
    }
}
