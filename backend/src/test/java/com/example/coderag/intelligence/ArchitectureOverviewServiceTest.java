package com.example.coderag.intelligence;

import com.example.coderag.chat.AnswerGenerationService;
import com.example.coderag.chat.ContextBuilder;
import com.example.coderag.dto.ArchitectureOverviewDto;
import com.example.coderag.model.ArchitectureOverview;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.ArchitectureOverviewRepository;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchitectureOverviewServiceTest {

    private GitHubRepositoryRepository repositoryRepository;
    private RepositoryFileRepository repositoryFileRepository;
    private CodeChunkRepository codeChunkRepository;
    private ArchitectureOverviewRepository architectureOverviewRepository;
    private ContextBuilder contextBuilder;
    private AnswerGenerationService answerGenerationService;

    private ArchitectureOverviewService architectureOverviewService;

    private UUID userId;
    private UUID repoId;
    private GitHubRepository testRepo;

    @BeforeEach
    void setUp() {
        repositoryRepository = Mockito.mock(GitHubRepositoryRepository.class);
        repositoryFileRepository = Mockito.mock(RepositoryFileRepository.class);
        codeChunkRepository = Mockito.mock(CodeChunkRepository.class);
        architectureOverviewRepository = Mockito.mock(ArchitectureOverviewRepository.class);
        contextBuilder = new ContextBuilder();
        answerGenerationService = Mockito.mock(AnswerGenerationService.class);

        architectureOverviewService = new ArchitectureOverviewService(
                repositoryRepository,
                repositoryFileRepository,
                codeChunkRepository,
                architectureOverviewRepository,
                contextBuilder,
                answerGenerationService
        );

        userId = UUID.randomUUID();
        repoId = UUID.randomUUID();
        testRepo = GitHubRepository.builder()
                .id(repoId)
                .ownerId(userId)
                .fullName("owner/repo")
                .latestCommitSha("commit123")
                .build();
    }

    @Test
    void getOrGenerateOverview_ShouldReturnCached_WhenAvailableAndNotForced() {
        when(repositoryRepository.findByIdAndOwnerId(repoId, userId)).thenReturn(Optional.of(testRepo));

        ArchitectureOverview cached = ArchitectureOverview.builder()
                .id(UUID.randomUUID())
                .repositoryId(repoId)
                .commitSha("commit123")
                .overviewText("Cached architecture summary")
                .technologies("Java, Spring Boot")
                .modules("backend, frontend")
                .createdAt(OffsetDateTime.now())
                .build();

        when(architectureOverviewRepository.findTopByRepositoryIdAndCommitShaOrderByCreatedAtDesc(repoId, "commit123"))
                .thenReturn(Optional.of(cached));

        ArchitectureOverviewDto result = architectureOverviewService.getOrGenerateOverview(userId, repoId, false);

        assertNotNull(result);
        assertEquals("Cached architecture summary", result.getOverviewText());
        assertEquals(List.of("Java", "Spring Boot"), result.getTechnologies());
        verify(answerGenerationService, never()).generateAnswer(anyString(), anyString(), anyList());
    }

    @Test
    void getOrGenerateOverview_ShouldGenerateAndCache_WhenForceTrue() {
        when(repositoryRepository.findByIdAndOwnerId(repoId, userId)).thenReturn(Optional.of(testRepo));

        RepositoryFile readme = RepositoryFile.builder().id(UUID.randomUUID()).repositoryId(repoId).filePath("README.md").sizeBytes(500L).build();
        RepositoryFile pom = RepositoryFile.builder().id(UUID.randomUUID()).repositoryId(repoId).filePath("pom.xml").sizeBytes(1200L).build();
        RepositoryFile app = RepositoryFile.builder().id(UUID.randomUUID()).repositoryId(repoId).filePath("src/main/java/App.java").sizeBytes(800L).build();

        when(repositoryFileRepository.findByRepositoryId(repoId)).thenReturn(List.of(readme, pom, app));

        CodeChunk chunk1 = CodeChunk.builder().id(UUID.randomUUID()).repositoryFileId(readme.getId()).filePath("README.md").chunkIndex(0).content("# CodeRAG").build();
        CodeChunk chunk2 = CodeChunk.builder().id(UUID.randomUUID()).repositoryFileId(pom.getId()).filePath("pom.xml").chunkIndex(0).content("<project>Spring Boot</project>").build();
        CodeChunk chunk3 = CodeChunk.builder().id(UUID.randomUUID()).repositoryFileId(app.getId()).filePath("src/main/java/App.java").chunkIndex(0).content("public class App {}").build();

        when(codeChunkRepository.findByRepositoryId(eq(repoId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(chunk1, chunk2, chunk3)));

        when(answerGenerationService.generateAnswer(anyString(), anyString(), anyList()))
                .thenReturn("Generated architecture overview using Spring Boot, PostgreSQL and Qdrant [pom.xml:1-10].");

        when(architectureOverviewRepository.save(any(ArchitectureOverview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArchitectureOverviewDto result = architectureOverviewService.getOrGenerateOverview(userId, repoId, true);

        assertNotNull(result);
        assertTrue(result.getOverviewText().contains("Generated architecture overview"));
        assertTrue(result.getTechnologies().contains("Spring Boot"));
        assertTrue(result.getTechnologies().contains("PostgreSQL"));
        verify(architectureOverviewRepository).save(any(ArchitectureOverview.class));
    }
}
