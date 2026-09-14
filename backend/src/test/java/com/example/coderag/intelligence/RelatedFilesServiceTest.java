package com.example.coderag.intelligence;

import com.example.coderag.dto.RelatedFileDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import com.example.coderag.service.KeywordSearchService;
import com.example.coderag.vector.EmbeddingService;
import com.example.coderag.vector.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RelatedFilesServiceTest {

    private GitHubRepositoryRepository repositoryRepository;
    private RepositoryFileRepository repositoryFileRepository;
    private CodeChunkRepository codeChunkRepository;
    private EmbeddingService embeddingService;
    private VectorStoreService vectorStoreService;
    private KeywordSearchService keywordSearchService;

    private RelatedFilesService relatedFilesService;

    private UUID userId;
    private UUID repoId;
    private GitHubRepository testRepo;

    @BeforeEach
    void setUp() {
        repositoryRepository = Mockito.mock(GitHubRepositoryRepository.class);
        repositoryFileRepository = Mockito.mock(RepositoryFileRepository.class);
        codeChunkRepository = Mockito.mock(CodeChunkRepository.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        vectorStoreService = Mockito.mock(VectorStoreService.class);
        keywordSearchService = Mockito.mock(KeywordSearchService.class);

        relatedFilesService = new RelatedFilesService(
                repositoryRepository,
                repositoryFileRepository,
                codeChunkRepository,
                embeddingService,
                vectorStoreService,
                keywordSearchService
        );

        userId = UUID.randomUUID();
        repoId = UUID.randomUUID();
        testRepo = GitHubRepository.builder().id(repoId).ownerId(userId).fullName("owner/repo").build();
    }

    @Test
    void findRelatedFiles_ShouldBoostFilesWithDirectSymbolReferences() {
        when(repositoryRepository.findByIdAndOwnerId(repoId, userId)).thenReturn(Optional.of(testRepo));

        UUID targetFileId = UUID.randomUUID();
        RepositoryFile targetFile = RepositoryFile.builder()
                .id(targetFileId)
                .repositoryId(repoId)
                .filePath("src/main/AuthService.java")
                .language("java")
                .build();

        CodeChunk targetChunk = CodeChunk.builder()
                .id(UUID.randomUUID())
                .repositoryFileId(targetFileId)
                .filePath("src/main/AuthService.java")
                .content("public class AuthService { public void login() {} }")
                .build();

        when(repositoryFileRepository.findByRepositoryIdAndFilePath(repoId, "src/main/AuthService.java"))
                .thenReturn(Optional.of(targetFile));
        when(codeChunkRepository.findByRepositoryFileId(eq(targetFileId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(targetChunk)));

        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        // Vector match: Controller (score 0.70) and Helper (score 0.85)
        SearchResultDto vec1 = SearchResultDto.builder().filePath("src/main/AuthController.java").language("java").score(0.70).build();
        SearchResultDto vec2 = SearchResultDto.builder().filePath("src/main/TokenHelper.java").language("java").score(0.85).build();

        when(vectorStoreService.search(eq(repoId), any(float[].class), anyInt()))
                .thenReturn(List.of(vec1, vec2));

        // Keyword match for symbol "AuthService": AuthController contains import
        SearchResultDto kw1 = SearchResultDto.builder()
                .filePath("src/main/AuthController.java")
                .content("import com.example.AuthService; public class AuthController { private AuthService authService; }")
                .build();

        when(keywordSearchService.search(eq(repoId), eq("AuthService"), anyInt()))
                .thenReturn(List.of(kw1));

        List<RelatedFileDto> related = relatedFilesService.findRelatedFiles(userId, repoId, "src/main/AuthService.java", 10);

        assertNotNull(related);
        assertEquals(2, related.size());

        // AuthController gets boosted: 0.70 + 0.35 = 1.0 (capped), beats TokenHelper (0.85)
        assertEquals("src/main/AuthController.java", related.get(0).getFilePath());
        assertEquals(1.0, related.get(0).getRelevanceScore());
        assertTrue(related.get(0).getReason().contains("references / imports AuthService"));

        assertEquals("src/main/TokenHelper.java", related.get(1).getFilePath());
        assertEquals(0.85, related.get(1).getRelevanceScore());
    }
}
