package com.example.coderag.service;

import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.CodeChunkSearchProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class KeywordSearchServiceTest {

    private CodeChunkRepository codeChunkRepository;
    private KeywordSearchService keywordSearchService;

    @BeforeEach
    void setUp() {
        codeChunkRepository = Mockito.mock(CodeChunkRepository.class);
        keywordSearchService = new KeywordSearchService(codeChunkRepository);
    }

    @Test
    void search_ShouldReturnEmptyList_WhenQueryIsBlankOrNull() {
        UUID repoId = UUID.randomUUID();
        assertTrue(keywordSearchService.search(repoId, null, 10).isEmpty());
        assertTrue(keywordSearchService.search(repoId, "", 10).isEmpty());
        assertTrue(keywordSearchService.search(repoId, "   ", 10).isEmpty());
        assertTrue(keywordSearchService.search(null, "query", 10).isEmpty());
    }

    @Test
    void search_ShouldMapProjectionsToSearchResultDtos() {
        UUID repoId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        CodeChunkSearchProjection projection = new CodeChunkSearchProjection() {
            @Override public UUID getId() { return chunkId; }
            @Override public UUID getRepositoryId() { return repoId; }
            @Override public UUID getRepositoryFileId() { return fileId; }
            @Override public String getFilePath() { return "src/main/Auth.java"; }
            @Override public Integer getStartLine() { return 1; }
            @Override public Integer getEndLine() { return 25; }
            @Override public Integer getChunkIndex() { return 0; }
            @Override public String getCommitSha() { return "sha123"; }
            @Override public String getContent() { return "public class Auth {}"; }
            @Override public Double getScore() { return 0.65; }
        };

        when(codeChunkRepository.searchByKeyword(eq(repoId), eq("auth"), eq(10)))
                .thenReturn(List.of(projection));

        List<SearchResultDto> results = keywordSearchService.search(repoId, "auth", 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        SearchResultDto dto = results.get(0);
        assertEquals(chunkId, dto.getId());
        assertEquals(repoId, dto.getRepositoryId());
        assertEquals("src/main/Auth.java", dto.getFilePath());
        assertEquals(0.65, dto.getScore());
        assertEquals(0.65, dto.getKeywordScore());
    }

    @Test
    void search_ShouldHandleExceptionsGracefully() {
        UUID repoId = UUID.randomUUID();
        when(codeChunkRepository.searchByKeyword(eq(repoId), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        List<SearchResultDto> results = keywordSearchService.search(repoId, "token", 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
