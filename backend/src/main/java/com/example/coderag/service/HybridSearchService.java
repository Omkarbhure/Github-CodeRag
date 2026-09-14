package com.example.coderag.service;

import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.vector.EmbeddingService;
import com.example.coderag.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HybridSearchService combines vector semantic search (Qdrant) and full-text keyword search (PostgreSQL)
 * using score normalization and weighted reciprocal score fusion.
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final KeywordSearchService keywordSearchService;
    private final double vectorWeight;
    private final double keywordWeight;

    public HybridSearchService(
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            KeywordSearchService keywordSearchService,
            @Value("${app.hybrid-search.vector-weight:0.7}") double vectorWeight,
            @Value("${app.hybrid-search.keyword-weight:0.3}") double keywordWeight
    ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.keywordSearchService = keywordSearchService;
        this.vectorWeight = vectorWeight;
        this.keywordWeight = keywordWeight;
        log.info("Initialized HybridSearchService with vectorWeight={}, keywordWeight={}", vectorWeight, keywordWeight);
    }

    public List<SearchResultDto> search(UUID repositoryId, String query, int topK) {
        if (repositoryId == null || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        int targetLimit = Math.max(1, topK);
        int candidatePoolSize = Math.max(targetLimit * 2, 20);

        // Run vector search and keyword search in parallel
        CompletableFuture<List<SearchResultDto>> vectorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                float[] queryVector = embeddingService.embed(query);
                return vectorStoreService.search(repositoryId, queryVector, candidatePoolSize);
            } catch (Exception e) {
                log.warn("Vector search failed for repo {} query '{}': {}", repositoryId, query, e.getMessage());
                return Collections.emptyList();
            }
        });

        CompletableFuture<List<SearchResultDto>> keywordFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return keywordSearchService.search(repositoryId, query, candidatePoolSize);
            } catch (Exception e) {
                log.warn("Keyword search failed for repo {} query '{}': {}", repositoryId, query, e.getMessage());
                return Collections.emptyList();
            }
        });

        List<SearchResultDto> vectorResults;
        List<SearchResultDto> keywordResults;

        try {
            CompletableFuture.allOf(vectorFuture, keywordFuture).get(10, TimeUnit.SECONDS);
            vectorResults = vectorFuture.join();
            keywordResults = keywordFuture.join();
        } catch (Exception e) {
            log.warn("Parallel hybrid search timed out or was interrupted, collecting available results: {}", e.getMessage());
            vectorResults = vectorFuture.isDone() && !vectorFuture.isCompletedExceptionally() ? vectorFuture.join() : Collections.emptyList();
            keywordResults = keywordFuture.isDone() && !keywordFuture.isCompletedExceptionally() ? keywordFuture.join() : Collections.emptyList();
        }

        return fuseResults(vectorResults, keywordResults, targetLimit);
    }

    public List<SearchResultDto> fuseResults(List<SearchResultDto> vectorResults, List<SearchResultDto> keywordResults, int topK) {
        if ((vectorResults == null || vectorResults.isEmpty()) && (keywordResults == null || keywordResults.isEmpty())) {
            return Collections.emptyList();
        }

        // 1. Min-max normalization for vector scores
        Map<UUID, Double> normalizedVectorScores = normalizeScores(vectorResults);

        // 2. Min-max normalization for keyword scores
        Map<UUID, Double> normalizedKeywordScores = normalizeScores(keywordResults);

        // 3. Merge and compute weighted scores
        Map<UUID, SearchResultDto> mergedMap = new HashMap<>();

        if (vectorResults != null) {
            for (SearchResultDto item : vectorResults) {
                if (item.getId() == null) continue;
                mergedMap.put(item.getId(), SearchResultDto.builder()
                        .id(item.getId())
                        .repositoryId(item.getRepositoryId())
                        .repositoryFileId(item.getRepositoryFileId())
                        .filePath(item.getFilePath())
                        .language(item.getLanguage())
                        .startLine(item.getStartLine())
                        .endLine(item.getEndLine())
                        .chunkIndex(item.getChunkIndex())
                        .commitSha(item.getCommitSha())
                        .content(item.getContent())
                        .vectorScore(item.getScore() != null ? item.getScore() : 0.0)
                        .keywordScore(0.0)
                        .build());
            }
        }

        if (keywordResults != null) {
            for (SearchResultDto item : keywordResults) {
                if (item.getId() == null) continue;
                SearchResultDto existing = mergedMap.get(item.getId());
                if (existing == null) {
                    mergedMap.put(item.getId(), SearchResultDto.builder()
                            .id(item.getId())
                            .repositoryId(item.getRepositoryId())
                            .repositoryFileId(item.getRepositoryFileId())
                            .filePath(item.getFilePath())
                            .language(item.getLanguage())
                            .startLine(item.getStartLine())
                            .endLine(item.getEndLine())
                            .chunkIndex(item.getChunkIndex())
                            .commitSha(item.getCommitSha())
                            .content(item.getContent())
                            .vectorScore(0.0)
                            .keywordScore(item.getScore() != null ? item.getScore() : 0.0)
                            .build());
                } else {
                    existing.setKeywordScore(item.getScore() != null ? item.getScore() : 0.0);
                    if (existing.getContent() == null && item.getContent() != null) {
                        existing.setContent(item.getContent());
                    }
                }
            }
        }

        // 4. Calculate final weighted scores
        List<SearchResultDto> mergedList = new ArrayList<>(mergedMap.values());
        for (SearchResultDto dto : mergedList) {
            double normVec = normalizedVectorScores.getOrDefault(dto.getId(), 0.0);
            double normKw = normalizedKeywordScores.getOrDefault(dto.getId(), 0.0);
            double finalScore = (vectorWeight * normVec) + (keywordWeight * normKw);

            dto.setFinalScore(finalScore);
            dto.setScore(finalScore);
        }

        // 5. Sort by finalScore descending
        mergedList.sort((a, b) -> {
            int cmp = Double.compare(
                    b.getFinalScore() != null ? b.getFinalScore() : 0.0,
                    a.getFinalScore() != null ? a.getFinalScore() : 0.0
            );
            if (cmp != 0) {
                return cmp;
            }
            return a.getId().compareTo(b.getId());
        });

        // 6. Return topK results
        if (mergedList.size() > topK) {
            return new ArrayList<>(mergedList.subList(0, topK));
        }

        return mergedList;
    }

    private Map<UUID, Double> normalizeScores(List<SearchResultDto> results) {
        Map<UUID, Double> normalized = new HashMap<>();
        if (results == null || results.isEmpty()) {
            return normalized;
        }

        double minScore = Double.MAX_VALUE;
        double maxScore = -Double.MAX_VALUE;

        for (SearchResultDto r : results) {
            if (r.getId() == null) continue;
            double score = r.getScore() != null ? r.getScore() : 0.0;
            if (score < minScore) minScore = score;
            if (score > maxScore) maxScore = score;
        }

        if (maxScore == -Double.MAX_VALUE) {
            return normalized;
        }

        double range = maxScore - minScore;
        for (SearchResultDto r : results) {
            if (r.getId() == null) continue;
            double score = r.getScore() != null ? r.getScore() : 0.0;
            if (range <= 1e-9) {
                // If all items have the same score or single item
                normalized.put(r.getId(), 1.0);
            } else {
                normalized.put(r.getId(), (score - minScore) / range);
            }
        }

        return normalized;
    }

    public double getVectorWeight() {
        return vectorWeight;
    }

    public double getKeywordWeight() {
        return keywordWeight;
    }
}
