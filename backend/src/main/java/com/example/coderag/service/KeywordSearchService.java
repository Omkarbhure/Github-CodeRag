package com.example.coderag.service;

import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.CodeChunkSearchProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * KeywordSearchService executes PostgreSQL full-text search against code chunk contents.
 *
 * Note on Tokenization:
 * PostgreSQL's default 'english' text search configuration stems and tokenizes standard English text,
 * which may treat camelCase identifiers (e.g. 'getUserById' or 'jwtTokenService') as single tokens
 * or apply language stemming rules that differ from programming language tokenization.
 * This is a known limitation acceptable for this phase; code-aware tokenization and custom lexers
 * are planned as a future improvement.
 */
@Service
public class KeywordSearchService {

    private static final Logger log = LoggerFactory.getLogger(KeywordSearchService.class);

    private final CodeChunkRepository codeChunkRepository;

    public KeywordSearchService(CodeChunkRepository codeChunkRepository) {
        this.codeChunkRepository = codeChunkRepository;
    }

    @Transactional(readOnly = true)
    public List<SearchResultDto> search(UUID repositoryId, String query, int topK) {
        if (repositoryId == null || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        int limit = Math.max(1, topK);
        String cleanedQuery = query.trim();

        try {
            List<CodeChunkSearchProjection> projections = codeChunkRepository.searchByKeyword(
                    repositoryId,
                    cleanedQuery,
                    limit
            );

            if (projections == null || projections.isEmpty()) {
                return Collections.emptyList();
            }

            List<SearchResultDto> results = new ArrayList<>(projections.size());
            for (CodeChunkSearchProjection p : projections) {
                double score = p.getScore() != null ? p.getScore() : 0.0;
                results.add(SearchResultDto.builder()
                        .id(p.getId())
                        .repositoryId(p.getRepositoryId())
                        .repositoryFileId(p.getRepositoryFileId())
                        .filePath(p.getFilePath())
                        .startLine(p.getStartLine())
                        .endLine(p.getEndLine())
                        .chunkIndex(p.getChunkIndex())
                        .commitSha(p.getCommitSha())
                        .content(p.getContent())
                        .score(score)
                        .keywordScore(score)
                        .build());
            }

            return results;
        } catch (Exception e) {
            log.warn("Keyword search failed for repo {} query '{}': {}", repositoryId, cleanedQuery, e.getMessage());
            return Collections.emptyList();
        }
    }
}
