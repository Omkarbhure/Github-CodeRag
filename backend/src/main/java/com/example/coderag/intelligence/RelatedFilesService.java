package com.example.coderag.intelligence;

import com.example.coderag.dto.RelatedFileDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.exception.ResourceNotFoundException;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import com.example.coderag.service.KeywordSearchService;
import com.example.coderag.vector.EmbeddingService;
import com.example.coderag.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RelatedFilesService {

    private static final Logger log = LoggerFactory.getLogger(RelatedFilesService.class);

    private final GitHubRepositoryRepository repositoryRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final KeywordSearchService keywordSearchService;

    public RelatedFilesService(
            GitHubRepositoryRepository repositoryRepository,
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            KeywordSearchService keywordSearchService
    ) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.keywordSearchService = keywordSearchService;
    }

    @Transactional(readOnly = true)
    public List<RelatedFileDto> findRelatedFiles(UUID userId, UUID repositoryId, String targetFilePath, int topK) {
        GitHubRepository repo = repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        if (targetFilePath == null || targetFilePath.isBlank()) {
            throw new ResourceNotFoundException("Target file path must not be blank.");
        }

        String normalizedTarget = targetFilePath.startsWith("/") ? targetFilePath.substring(1) : targetFilePath;
        normalizedTarget = normalizedTarget.replace('\\', '/');

        log.info("Finding related files for repo: {}, file: {}", repo.getFullName(), normalizedTarget);

        // 1. Fetch file record and chunks for the target file
        Optional<RepositoryFile> targetFileOpt = repositoryFileRepository.findByRepositoryIdAndFilePath(repo.getId(), normalizedTarget);
        if (targetFileOpt.isEmpty()) {
            throw new ResourceNotFoundException("Target file not found in repository: " + normalizedTarget);
        }

        RepositoryFile targetFile = targetFileOpt.get();
        List<CodeChunk> targetChunks = codeChunkRepository.findByRepositoryFileId(targetFile.getId(), Pageable.unpaged()).getContent();

        if (targetChunks.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Extract representative query text from target file (first chunk or combination)
        String representativeText = targetChunks.get(0).getContent();
        float[] queryVector = embeddingService.embed(representativeText);

        // 3. Search vector store for semantically similar chunks across the repo
        int candidatePool = Math.max(topK * 4, 30);
        List<SearchResultDto> vectorMatches = vectorStoreService.search(repo.getId(), queryVector, candidatePool);

        // 4. Structural Reference Heuristic (check if other files import/reference target class/symbol)
        String symbolName = extractSymbolName(normalizedTarget);
        Set<String> referencingFiles = new HashSet<>();

        if (symbolName.length() >= 3) {
            List<SearchResultDto> keywordMatches = keywordSearchService.search(repo.getId(), symbolName, 30);
            for (SearchResultDto kw : keywordMatches) {
                if (kw.getFilePath() != null && !kw.getFilePath().equals(normalizedTarget)) {
                    // Check if content actually contains the symbol as word or import
                    if (kw.getContent() != null && kw.getContent().contains(symbolName)) {
                        referencingFiles.add(kw.getFilePath());
                    }
                }
            }
        }

        // 5. Aggregate candidate scores per distinct file
        Map<String, CandidateFileAggregate> aggregatedFiles = new HashMap<>();

        // Process vector matches
        for (SearchResultDto result : vectorMatches) {
            String path = result.getFilePath();
            if (path == null || path.equals(normalizedTarget)) {
                continue;
            }

            double score = result.getScore() != null ? result.getScore() : 0.0;
            CandidateFileAggregate agg = aggregatedFiles.computeIfAbsent(path, p -> new CandidateFileAggregate(p, result.getLanguage()));
            if (score > agg.maxVectorScore) {
                agg.maxVectorScore = score;
            }
        }

        // Include any files found via direct symbol reference that might have had low vector similarity
        for (String refPath : referencingFiles) {
            if (!refPath.equals(normalizedTarget)) {
                aggregatedFiles.computeIfAbsent(refPath, p -> new CandidateFileAggregate(p, detectLanguage(p)));
            }
        }

        // 6. Compute final scores and reasons
        List<RelatedFileDto> results = new ArrayList<>();

        for (CandidateFileAggregate agg : aggregatedFiles.values()) {
            boolean hasDirectReference = referencingFiles.contains(agg.filePath);
            double baseScore = agg.maxVectorScore;
            double finalScore = baseScore;
            String reason;

            if (hasDirectReference) {
                // Boost score by +0.3 for direct structural dependency
                finalScore = Math.min(1.0, baseScore + 0.35);
                reason = "Directly references / imports " + symbolName;
            } else if (baseScore >= 0.80) {
                reason = "Strong semantic similarity and shared domain logic";
            } else if (baseScore >= 0.60) {
                reason = "Related functionality in the same module";
            } else {
                reason = "Shared conceptual patterns and types";
            }

            results.add(RelatedFileDto.builder()
                    .filePath(agg.filePath)
                    .language(agg.language)
                    .relevanceScore(Math.round(finalScore * 100.0) / 100.0)
                    .reason(reason)
                    .build());
        }

        // 7. Sort descending by score
        results.sort(Comparator.comparingDouble(RelatedFileDto::getRelevanceScore).reversed());

        int limit = Math.max(1, topK);
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    private String extractSymbolName(String filePath) {
        String baseName = filePath;
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash != -1) {
            baseName = filePath.substring(lastSlash + 1);
        }
        int dotIdx = baseName.indexOf('.');
        return dotIdx != -1 ? baseName.substring(0, dotIdx) : baseName;
    }

    private String detectLanguage(String filePath) {
        String ext = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase() : "";
        return switch (ext) {
            case "java" -> "java";
            case "ts", "tsx" -> "typescript";
            case "js", "jsx" -> "javascript";
            case "py" -> "python";
            case "go" -> "go";
            default -> ext;
        };
    }

    private static class CandidateFileAggregate {
        final String filePath;
        final String language;
        double maxVectorScore = 0.0;

        CandidateFileAggregate(String filePath, String language) {
            this.filePath = filePath;
            this.language = language;
        }
    }
}
