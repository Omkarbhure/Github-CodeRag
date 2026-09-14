package com.example.coderag.service;

import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.vector.EmbeddingService;
import com.example.coderag.vector.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class HybridSearchServiceTest {

    private EmbeddingService embeddingService;
    private VectorStoreService vectorStoreService;
    private KeywordSearchService keywordSearchService;
    private HybridSearchService hybridSearchService;

    private static final double VECTOR_WEIGHT = 0.7;
    private static final double KEYWORD_WEIGHT = 0.3;

    @BeforeEach
    void setUp() {
        embeddingService = Mockito.mock(EmbeddingService.class);
        vectorStoreService = Mockito.mock(VectorStoreService.class);
        keywordSearchService = Mockito.mock(KeywordSearchService.class);
        hybridSearchService = new HybridSearchService(
                embeddingService,
                vectorStoreService,
                keywordSearchService,
                VECTOR_WEIGHT,
                KEYWORD_WEIGHT
        );
    }

    @Test
    void search_ShouldReturnEmpty_WhenInputInvalid() {
        UUID repoId = UUID.randomUUID();
        assertTrue(hybridSearchService.search(null, "test", 5).isEmpty());
        assertTrue(hybridSearchService.search(repoId, null, 5).isEmpty());
        assertTrue(hybridSearchService.search(repoId, "  ", 5).isEmpty());
    }

    @Test
    void fuseResults_ShouldHandleBothEmpty() {
        assertTrue(hybridSearchService.fuseResults(Collections.emptyList(), Collections.emptyList(), 10).isEmpty());
        assertTrue(hybridSearchService.fuseResults(null, null, 10).isEmpty());
    }

    @Test
    void fuseResults_ShouldNormalizeAndCombineScores() {
        UUID repoId = UUID.randomUUID();
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();
        UUID chunk3 = UUID.randomUUID();

        // Vector results: chunk1 (score=0.9), chunk2 (score=0.5)
        // Range = 0.4 -> norm(chunk1) = 1.0, norm(chunk2) = 0.0
        SearchResultDto vec1 = SearchResultDto.builder().id(chunk1).repositoryId(repoId).filePath("A.java").score(0.9).build();
        SearchResultDto vec2 = SearchResultDto.builder().id(chunk2).repositoryId(repoId).filePath("B.java").score(0.5).build();

        // Keyword results: chunk1 (score=0.1), chunk3 (score=0.3)
        // Range = 0.2 -> norm(chunk3) = 1.0, norm(chunk1) = 0.0
        SearchResultDto kw1 = SearchResultDto.builder().id(chunk1).repositoryId(repoId).filePath("A.java").score(0.1).build();
        SearchResultDto kw3 = SearchResultDto.builder().id(chunk3).repositoryId(repoId).filePath("C.java").score(0.3).build();

        List<SearchResultDto> fused = hybridSearchService.fuseResults(List.of(vec1, vec2), List.of(kw1, kw3), 10);

        assertEquals(3, fused.size());

        // Expected scores:
        // chunk1: 0.7 * 1.0 (vec) + 0.3 * 0.0 (kw) = 0.70
        // chunk3: 0.7 * 0.0 (vec) + 0.3 * 1.0 (kw) = 0.30
        // chunk2: 0.7 * 0.0 (vec) + 0.3 * 0.0 (kw) = 0.00
        assertEquals(chunk1, fused.get(0).getId());
        assertEquals(0.70, fused.get(0).getFinalScore(), 0.001);
        assertEquals(0.9, fused.get(0).getVectorScore(), 0.001);
        assertEquals(0.1, fused.get(0).getKeywordScore(), 0.001);

        assertEquals(chunk3, fused.get(1).getId());
        assertEquals(0.30, fused.get(1).getFinalScore(), 0.001);
        assertEquals(0.0, fused.get(1).getVectorScore(), 0.001);
        assertEquals(0.3, fused.get(1).getKeywordScore(), 0.001);

        assertEquals(chunk2, fused.get(2).getId());
        assertEquals(0.00, fused.get(2).getFinalScore(), 0.001);
        assertEquals(0.5, fused.get(2).getVectorScore(), 0.001);
        assertEquals(0.0, fused.get(2).getKeywordScore(), 0.001);
    }

    @Test
    void fuseResults_ShouldRankChunkWithStrongKeywordMatchHigher() {
        UUID repoId = UUID.randomUUID();
        UUID semanticChunk = UUID.randomUUID();
        UUID exactIdentifierChunk = UUID.randomUUID();

        // Vector: semanticChunk=0.85, exactIdentifierChunk=0.80
        // min=0.80, max=0.85 -> norm(semantic)=1.0, norm(exact)=0.0
        SearchResultDto vecSemantic = SearchResultDto.builder().id(semanticChunk).repositoryId(repoId).filePath("Semantic.java").score(0.85).build();
        SearchResultDto vecExact = SearchResultDto.builder().id(exactIdentifierChunk).repositoryId(repoId).filePath("Exact.java").score(0.80).build();

        // Keyword: exactIdentifierChunk has exact match (score 0.4), semantic has no match
        // Only 1 item in keyword results -> normalized to 1.0
        SearchResultDto kwExact = SearchResultDto.builder().id(exactIdentifierChunk).repositoryId(repoId).filePath("Exact.java").score(0.4).build();

        List<SearchResultDto> fused = hybridSearchService.fuseResults(List.of(vecSemantic, vecExact), List.of(kwExact), 10);

        // Score exact: (0.7 * 0.0) + (0.3 * 1.0) = 0.30
        // Score semantic: (0.7 * 1.0) + (0.3 * 0.0) = 0.70
        // If vector had equal scores (0.85 each) -> exact gets 0.7*1.0 + 0.3*1.0 = 1.0 vs 0.7*1.0 + 0 = 0.7
        assertEquals(2, fused.size());
    }

    @Test
    void fuseResults_ShouldGracefullyFallbackWhenKeywordResultsEmpty() {
        UUID repoId = UUID.randomUUID();
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();

        SearchResultDto vec1 = SearchResultDto.builder().id(chunk1).repositoryId(repoId).filePath("A.java").score(0.9).build();
        SearchResultDto vec2 = SearchResultDto.builder().id(chunk2).repositoryId(repoId).filePath("B.java").score(0.7).build();

        List<SearchResultDto> fused = hybridSearchService.fuseResults(List.of(vec1, vec2), Collections.emptyList(), 10);

        assertEquals(2, fused.size());
        assertEquals(chunk1, fused.get(0).getId());
        assertEquals(0.70, fused.get(0).getFinalScore(), 0.001); // 0.7 * 1.0 + 0 = 0.7
        assertEquals(0.00, fused.get(1).getFinalScore(), 0.001); // 0.7 * 0.0 + 0 = 0.0
        assertEquals(0.9, fused.get(0).getVectorScore(), 0.001);
        assertEquals(0.0, fused.get(0).getKeywordScore(), 0.001);
    }

    @Test
    void fuseResults_ShouldRespectTopKLimit() {
        UUID repoId = UUID.randomUUID();
        SearchResultDto v1 = SearchResultDto.builder().id(UUID.randomUUID()).repositoryId(repoId).filePath("A.java").score(0.9).build();
        SearchResultDto v2 = SearchResultDto.builder().id(UUID.randomUUID()).repositoryId(repoId).filePath("B.java").score(0.8).build();
        SearchResultDto v3 = SearchResultDto.builder().id(UUID.randomUUID()).repositoryId(repoId).filePath("C.java").score(0.7).build();

        List<SearchResultDto> fused = hybridSearchService.fuseResults(List.of(v1, v2, v3), Collections.emptyList(), 2);
        assertEquals(2, fused.size());
    }

    @Test
    void search_ShouldExecuteParallelSearchAndReturnFusedResults() {
        UUID repoId = UUID.randomUUID();
        String query = "jwt token";
        float[] fakeVector = new float[]{0.1f, 0.2f};

        UUID chunk1 = UUID.randomUUID();
        SearchResultDto vecResult = SearchResultDto.builder().id(chunk1).repositoryId(repoId).filePath("Jwt.java").score(0.88).build();
        SearchResultDto kwResult = SearchResultDto.builder().id(chunk1).repositoryId(repoId).filePath("Jwt.java").score(0.12).build();

        when(embeddingService.embed(query)).thenReturn(fakeVector);
        when(vectorStoreService.search(eq(repoId), any(float[].class), anyInt())).thenReturn(List.of(vecResult));
        when(keywordSearchService.search(eq(repoId), eq(query), anyInt())).thenReturn(List.of(kwResult));

        List<SearchResultDto> results = hybridSearchService.search(repoId, query, 5);

        assertNotNull(results);
        assertEquals(1, results.size());
        SearchResultDto top = results.get(0);
        assertEquals(chunk1, top.getId());
        assertEquals(1.0, top.getFinalScore(), 0.001); // Both normalized to 1.0 -> 0.7*1 + 0.3*1 = 1.0
        assertEquals(0.88, top.getVectorScore(), 0.001);
        assertEquals(0.12, top.getKeywordScore(), 0.001);
    }
}
