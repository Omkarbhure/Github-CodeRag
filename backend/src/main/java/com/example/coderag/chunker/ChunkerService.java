package com.example.coderag.chunker;

import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkerService {

    private static final Logger log = LoggerFactory.getLogger(ChunkerService.class);

    public static final int DEFAULT_CHUNK_SIZE = 120;
    public static final int DEFAULT_OVERLAP = 20;

    private final LowValueDetectorService lowValueDetectorService;
    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;

    @Value("${coderag.repo-storage-path:./data/repos}")
    private String repoStoragePath;

    public ChunkerService(
            LowValueDetectorService lowValueDetectorService,
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository
    ) {
        this.lowValueDetectorService = lowValueDetectorService;
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
    }

    @Transactional
    public int chunkRepository(UUID repositoryId, String commitSha) {
        return chunkRepository(repositoryId, commitSha, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    @Transactional
    public int chunkRepository(UUID repositoryId, String commitSha, int chunkSize, int overlap) {
        log.info("Starting chunking for repositoryId={}, chunkSize={}, overlap={}", repositoryId, chunkSize, overlap);

        List<RepositoryFile> files = repositoryFileRepository.findByRepositoryId(repositoryId);
        Path repoDir = Paths.get(repoStoragePath, repositoryId.toString()).toAbsolutePath().normalize();

        List<CodeChunk> chunksToSave = new ArrayList<>();
        List<RepositoryFile> filesToUpdate = new ArrayList<>();

        for (RepositoryFile file : files) {
            // Only process files that were not skipped in Phase 1
            if (Boolean.TRUE.equals(file.getSkipped())) {
                continue;
            }

            Path filePath = repoDir.resolve(file.getFilePath()).normalize();
            if (!filePath.startsWith(repoDir) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("File not found on disk during chunking: {}", filePath);
                file.setSkipped(true);
                file.setSkipReason("File not found on disk");
                filesToUpdate.add(file);
                continue;
            }

            List<String> lines;
            try {
                lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Failed to read file as UTF-8: {}", filePath, e);
                file.setSkipped(true);
                file.setSkipReason("Unreadable file encoding");
                filesToUpdate.add(file);
                continue;
            }

            // Detect low-value or generated files
            LowValueDetectorService.DetectionResult detection = lowValueDetectorService.detect(lines, file.getSizeBytes());
            if (detection.isLowValue()) {
                log.info("Skipping low-value file {}: {}", file.getFilePath(), detection.reason());
                file.setSkipped(true);
                file.setSkipReason(detection.reason());
                filesToUpdate.add(file);
                continue;
            }

            // Generate chunks for kept file
            List<CodeChunk> fileChunks = generateChunksForFile(repositoryId, file, lines, commitSha, chunkSize, overlap);
            chunksToSave.addAll(fileChunks);
        }

        if (!filesToUpdate.isEmpty()) {
            repositoryFileRepository.saveAll(filesToUpdate);
        }

        // Idempotent re-chunking: clear old chunks for this repository and insert new ones
        codeChunkRepository.deleteByRepositoryId(repositoryId);
        if (!chunksToSave.isEmpty()) {
            codeChunkRepository.saveAll(chunksToSave);
        }

        log.info("Chunking completed for repositoryId={}. Total chunks generated: {}", repositoryId, chunksToSave.size());
        return chunksToSave.size();
    }

    public List<CodeChunk> generateChunksForFile(UUID repositoryId, RepositoryFile file, List<String> lines,
                                                 String commitSha, int chunkSize, int overlap) {
        List<CodeChunk> chunks = new ArrayList<>();
        int totalLines = lines.size();

        if (totalLines == 0) {
            return chunks;
        }

        int step = Math.max(1, chunkSize - overlap);

        if (totalLines <= chunkSize) {
            // Single chunk covering the entire file
            String content = String.join("\n", lines);
            CodeChunk chunk = CodeChunk.builder()
                    .repositoryId(repositoryId)
                    .repositoryFileId(file.getId())
                    .filePath(file.getFilePath())
                    .commitSha(commitSha)
                    .chunkIndex(0)
                    .startLine(1)
                    .endLine(totalLines)
                    .content(content)
                    .build();
            chunks.add(chunk);
            return chunks;
        }

        int chunkIndex = 0;
        int startIndex = 0;

        while (startIndex < totalLines) {
            int endIndex = Math.min(startIndex + chunkSize, totalLines);
            List<String> subList = lines.subList(startIndex, endIndex);
            String content = String.join("\n", subList);

            CodeChunk chunk = CodeChunk.builder()
                    .repositoryId(repositoryId)
                    .repositoryFileId(file.getId())
                    .filePath(file.getFilePath())
                    .commitSha(commitSha)
                    .chunkIndex(chunkIndex++)
                    .startLine(startIndex + 1) // 1-indexed
                    .endLine(endIndex)         // 1-indexed inclusive
                    .content(content)
                    .build();
            chunks.add(chunk);

            startIndex += step;
        }

        return chunks;
    }
}
