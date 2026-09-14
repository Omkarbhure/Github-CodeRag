package com.example.coderag.service;

import com.example.coderag.chunker.ChunkerService;
import com.example.coderag.exception.RepoTooLargeException;
import com.example.coderag.github.FileFilterService;
import com.example.coderag.github.ZipArchiveService;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.IndexingJob;
import com.example.coderag.model.IndexingStatus;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.IndexingJobRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import com.example.coderag.vector.EmbeddingService;
import com.example.coderag.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AsyncIndexingService {

    private static final Logger log = LoggerFactory.getLogger(AsyncIndexingService.class);

    private final IndexingJobRepository indexingJobRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;
    private final ZipArchiveService zipArchiveService;
    private final FileFilterService fileFilterService;
    private final ChunkerService chunkerService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final TransactionTemplate transactionTemplate;

    public AsyncIndexingService(
            IndexingJobRepository indexingJobRepository,
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository,
            ZipArchiveService zipArchiveService,
            FileFilterService fileFilterService,
            ChunkerService chunkerService,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            PlatformTransactionManager transactionManager
    ) {
        this.indexingJobRepository = indexingJobRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
        this.zipArchiveService = zipArchiveService;
        this.fileFilterService = fileFilterService;
        this.chunkerService = chunkerService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Async("indexingTaskExecutor")
    public void processIndexingAsync(UUID repositoryId, UUID jobId, String owner, String repoName, String commitSha) {
        long startTime = System.currentTimeMillis();
        String repoIdentifier = owner + "/" + repoName;
        log.info("Starting async indexing job [{}] for repo: {} (commit: {})", jobId, repoIdentifier, commitSha);

        IndexingJob job = indexingJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Indexing job [{}] not found for repo {}. Aborting async indexing.", jobId, repoIdentifier);
            return;
        }

        try {
            // 1. Download & Extract
            job.setStatus(IndexingStatus.DOWNLOADING);
            job = indexingJobRepository.save(job);
            log.info("Job [{}]: Downloading repository {}", jobId, repoIdentifier);

            Path extractedPath = zipArchiveService.downloadAndExtractRepository(owner, repoName, commitSha, repositoryId);

            // 2. Scan & Filter
            job.setStatus(IndexingStatus.SCANNING);
            job = indexingJobRepository.save(job);
            log.info("Job [{}]: Scanning and filtering files for {}", jobId, repoIdentifier);

            List<RepositoryFile> scannedFiles = fileFilterService.scanAndFilterRepository(extractedPath, repositoryId);
            saveRepositoryFiles(repositoryId, scannedFiles);

            // 3. Chunk
            job.setStatus(IndexingStatus.CHUNKING);
            job = indexingJobRepository.save(job);
            log.info("Job [{}]: Chunking source code for {}", jobId, repoIdentifier);

            chunkerService.chunkRepository(repositoryId, commitSha);

            // 4. Embed & Store in Vector DB
            job.setStatus(IndexingStatus.EMBEDDING);
            job = indexingJobRepository.save(job);
            log.info("Job [{}]: Generating embeddings and uploading to Qdrant for {}", jobId, repoIdentifier);

            embedRepositoryChunks(repositoryId, repoIdentifier);

            // 5. Complete
            job.setStatus(IndexingStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            job.setErrorMessage(null);
            indexingJobRepository.save(job);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Job [{}] COMPLETED successfully for {} in {}ms", jobId, repoIdentifier, elapsed);

        } catch (RepoTooLargeException e) {
            log.warn("Job [{}] REJECTED_TOO_LARGE for {}: {}", jobId, repoIdentifier, e.getMessage());
            job.setStatus(IndexingStatus.REJECTED_TOO_LARGE);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            indexingJobRepository.save(job);
        } catch (Exception e) {
            log.error("Job [{}] FAILED for {}: {}", jobId, repoIdentifier, e.getMessage(), e);
            job.setStatus(IndexingStatus.FAILED);
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown indexing error occurred");
            job.setCompletedAt(OffsetDateTime.now());
            indexingJobRepository.save(job);
        }
    }

    private void embedRepositoryChunks(UUID repositoryId, String repoIdentifier) {
        List<CodeChunk> chunks = codeChunkRepository.findByRepositoryId(repositoryId, Pageable.unpaged()).getContent();
        int total = chunks.size();
        log.info("Embedding {} chunks for repository: {}", total, repoIdentifier);

        // Clear previous vectors for this repository
        vectorStoreService.deleteByRepositoryId(repositoryId);

        for (int i = 0; i < total; i++) {
            CodeChunk chunk = chunks.get(i);
            float[] vector = embeddingService.embed(chunk.getContent());
            vectorStoreService.upsert(chunk, vector);

            if ((i + 1) % 10 == 0 || i + 1 == total) {
                log.info("Embedded {}/{} chunks for repo {}", i + 1, total, repoIdentifier);
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Embedding job interrupted", e);
            }
        }
    }

    public void saveRepositoryFiles(UUID repositoryId, List<RepositoryFile> files) {
        transactionTemplate.executeWithoutResult(status -> {
            repositoryFileRepository.deleteByRepositoryId(repositoryId);
            repositoryFileRepository.saveAll(files);
        });
    }
}
