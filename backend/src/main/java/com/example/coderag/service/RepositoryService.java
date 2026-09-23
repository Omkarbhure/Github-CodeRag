package com.example.coderag.service;

import com.example.coderag.chunker.ChunkerService;
import com.example.coderag.dto.CodeChunkDto;
import com.example.coderag.dto.ImportRepositoryRequest;
import com.example.coderag.dto.PageResponse;
import com.example.coderag.dto.RepositoryDetailDto;
import com.example.coderag.dto.RepositoryFileDto;
import com.example.coderag.dto.RepositorySummaryDto;
import com.example.coderag.dto.SearchRequestDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.exception.RepoTooLargeException;
import com.example.coderag.exception.ResourceNotFoundException;
import com.example.coderag.github.FileFilterService;
import com.example.coderag.github.GitHubApiClient;
import com.example.coderag.github.GitHubConfig;
import com.example.coderag.github.ZipArchiveService;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.IndexingJob;
import com.example.coderag.model.IndexingStatus;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.IndexingJobRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import com.example.coderag.vector.EmbeddingService;
import com.example.coderag.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    private final GitHubRepositoryRepository repositoryRepository;
    private final IndexingJobRepository indexingJobRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;
    private final GitHubApiClient gitHubApiClient;
    private final AsyncIndexingService asyncIndexingService;
    private final VectorStoreService vectorStoreService;
    private final HybridSearchService hybridSearchService;
    private final GitHubConfig gitHubConfig;

    public RepositoryService(
            GitHubRepositoryRepository repositoryRepository,
            IndexingJobRepository indexingJobRepository,
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository,
            GitHubApiClient gitHubApiClient,
            AsyncIndexingService asyncIndexingService,
            VectorStoreService vectorStoreService,
            HybridSearchService hybridSearchService,
            GitHubConfig gitHubConfig
    ) {
        this.repositoryRepository = repositoryRepository;
        this.indexingJobRepository = indexingJobRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
        this.gitHubApiClient = gitHubApiClient;
        this.asyncIndexingService = asyncIndexingService;
        this.vectorStoreService = vectorStoreService;
        this.hybridSearchService = hybridSearchService;
        this.gitHubConfig = gitHubConfig;
    }

    public RepositorySummaryDto importRepository(UUID userId, ImportRepositoryRequest request) {
        GitHubApiClient.ParsedRepoUrl parsedUrl = gitHubApiClient.parseUrl(request.getGithubUrl());
        log.info("Importing repository: {} for user: {}", parsedUrl.fullName(), userId);

        // 1. Fetch metadata from GitHub REST API
        GitHubApiClient.GitHubRepoMetadata metadata = gitHubApiClient.getRepoMetadata(parsedUrl.owner(), parsedUrl.name());

        // 2. Check reported size against MAX_REPO_SIZE_MB
        long maxRepoSizeKb = gitHubConfig.getMaxRepoSizeMb() * 1024;
        if (metadata.sizeKb() > maxRepoSizeKb) {
            double sizeMb = (double) metadata.sizeKb() / 1024;
            String errorMsg = String.format("Repository exceeds the %dMB limit (reported size: %.2f MB)",
                    gitHubConfig.getMaxRepoSizeMb(), sizeMb);
            log.warn("Repository {}/{} rejected: {}", parsedUrl.owner(), parsedUrl.name(), errorMsg);
            throw new RepoTooLargeException(errorMsg);
        }

        // 3. Get default branch + latest commit SHA
        String commitSha = gitHubApiClient.getLatestCommitSha(parsedUrl.owner(), parsedUrl.name(), metadata.defaultBranch());

        // 4. Check if exact (user, repo, commitSha) is already indexed and COMPLETED
        Optional<GitHubRepository> existingRepoOpt = repositoryRepository
                .findByOwnerIdAndFullNameAndLatestCommitSha(userId, metadata.fullName(), commitSha);

        if (existingRepoOpt.isPresent()) {
            GitHubRepository existingRepo = existingRepoOpt.get();
            Optional<IndexingJob> latestJob = indexingJobRepository.findTopByRepositoryIdOrderByStartedAtDesc(existingRepo.getId());
            if (latestJob.isPresent() && latestJob.get().getStatus() == IndexingStatus.COMPLETED) {
                log.info("Repository {}/{} at commit {} already indexed for user {}",
                        parsedUrl.owner(), parsedUrl.name(), commitSha, userId);
                return buildSummaryDto(existingRepo, latestJob.get(), true);
            }
        }

        // 5. Create or update repository entity
        GitHubRepository repository = existingRepoOpt.orElseGet(() ->
                GitHubRepository.builder()
                        .owner(metadata.owner())
                        .name(metadata.name())
                        .fullName(metadata.fullName())
                        .defaultBranch(metadata.defaultBranch())
                        .latestCommitSha(commitSha)
                        .url(metadata.htmlUrl())
                        .sizeKb(metadata.sizeKb())
                        .ownerId(userId)
                        .build()
        );
        repository.setLatestCommitSha(commitSha);
        repository.setSizeKb(metadata.sizeKb());
        repository.setDefaultBranch(metadata.defaultBranch());
        repository = repositoryRepository.save(repository);

        // 6. Create IndexingJob in PENDING status
        IndexingJob job = IndexingJob.builder()
                .repositoryId(repository.getId())
                .status(IndexingStatus.PENDING)
                .startedAt(OffsetDateTime.now())
                .build();
        job = indexingJobRepository.save(job);

        // 7. Dispatch asynchronous non-blocking indexing pipeline
        asyncIndexingService.processIndexingAsync(
                repository.getId(),
                job.getId(),
                metadata.owner(),
                metadata.name(),
                commitSha
        );

        log.info("Dispatched async indexing job [{}] for repo: {}", job.getId(), repository.getFullName());
        return buildSummaryDto(repository, job, false);
    }

    public RepositorySummaryDto reindexRepository(UUID userId, UUID repositoryId) {
        GitHubRepository repository = repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        String commitSha = repository.getLatestCommitSha();
        try {
            commitSha = gitHubApiClient.getLatestCommitSha(repository.getOwner(), repository.getName(), repository.getDefaultBranch());
            repository.setLatestCommitSha(commitSha);
            repository = repositoryRepository.save(repository);
        } catch (Exception e) {
            log.warn("Could not fetch latest commit from GitHub for re-index, using existing commit {}", commitSha);
        }

        IndexingJob job = IndexingJob.builder()
                .repositoryId(repository.getId())
                .status(IndexingStatus.PENDING)
                .startedAt(OffsetDateTime.now())
                .build();
        job = indexingJobRepository.save(job);

        asyncIndexingService.processIndexingAsync(
                repository.getId(),
                job.getId(),
                repository.getOwner(),
                repository.getName(),
                commitSha
        );

        log.info("Dispatched re-indexing job [{}] for repo: {}", job.getId(), repository.getFullName());
        return buildSummaryDto(repository, job, false);
    }

    @Transactional(readOnly = true)
    public List<RepositorySummaryDto> getUserRepositories(UUID userId) {
        List<GitHubRepository> repositories = repositoryRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        List<RepositorySummaryDto> dtos = new ArrayList<>();

        for (GitHubRepository repo : repositories) {
            IndexingJob job = indexingJobRepository.findTopByRepositoryIdOrderByStartedAtDesc(repo.getId()).orElse(null);
            dtos.add(buildSummaryDto(repo, job, false));
        }

        return dtos;
    }

    @Transactional(readOnly = true)
    public RepositoryDetailDto getRepositoryDetail(UUID userId, UUID repositoryId) {
        GitHubRepository repo = repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        IndexingJob job = indexingJobRepository.findTopByRepositoryIdOrderByStartedAtDesc(repo.getId()).orElse(null);
        List<RepositoryFile> files = repositoryFileRepository.findByRepositoryId(repo.getId());
        long totalFiles = files.size();
        long skippedFiles = files.stream().filter(f -> Boolean.TRUE.equals(f.getSkipped())).count();
        long keptFiles = totalFiles - skippedFiles;

        long lowValueSkippedCount = files.stream().filter(f -> Boolean.TRUE.equals(f.getSkipped()) && f.getSkipReason() != null && (
                f.getSkipReason().startsWith("Likely minified") ||
                f.getSkipReason().startsWith("Generated code") ||
                f.getSkipReason().startsWith("Empty file") ||
                f.getSkipReason().startsWith("File contains only whitespace") ||
                f.getSkipReason().startsWith("Unreadable") ||
                f.getSkipReason().startsWith("File not found")
        )).count();

        long totalChunks = codeChunkRepository.countByRepositoryId(repo.getId());
        long embeddedChunkCount = vectorStoreService.countByRepositoryId(repo.getId());

        return RepositoryDetailDto.builder()
                .id(repo.getId())
                .owner(repo.getOwner())
                .name(repo.getName())
                .fullName(repo.getFullName())
                .defaultBranch(repo.getDefaultBranch())
                .latestCommitSha(repo.getLatestCommitSha())
                .url(repo.getUrl())
                .sizeKb(repo.getSizeKb())
                .totalFiles(totalFiles)
                .skippedFiles(skippedFiles)
                .keptFiles(keptFiles)
                .lowValueSkippedCount(lowValueSkippedCount)
                .totalChunks(totalChunks)
                .embeddedChunkCount(embeddedChunkCount)
                .status(job != null ? job.getStatus() : IndexingStatus.PENDING)
                .errorMessage(job != null ? job.getErrorMessage() : null)
                .startedAt(job != null ? job.getStartedAt() : null)
                .completedAt(job != null ? job.getCompletedAt() : null)
                .createdAt(repo.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<RepositoryFileDto> getRepositoryFiles(UUID userId, UUID repositoryId, Boolean skipped, Pageable pageable) {
        repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        Page<RepositoryFile> filePage;
        if (skipped != null) {
            filePage = repositoryFileRepository.findByRepositoryIdAndSkipped(repositoryId, skipped, pageable);
        } else {
            filePage = repositoryFileRepository.findByRepositoryId(repositoryId, pageable);
        }

        Page<RepositoryFileDto> dtoPage = filePage.map(RepositoryFileDto::fromEntity);
        return PageResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CodeChunkDto> getRepositoryChunks(UUID userId, UUID repositoryId, boolean includeContent, Pageable pageable) {
        repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        Page<CodeChunk> chunkPage = codeChunkRepository.findByRepositoryId(repositoryId, pageable);
        Page<CodeChunkDto> dtoPage = chunkPage.map(chunk -> CodeChunkDto.builder()
                .id(chunk.getId())
                .repositoryId(chunk.getRepositoryId())
                .repositoryFileId(chunk.getRepositoryFileId())
                .filePath(chunk.getFilePath())
                .commitSha(chunk.getCommitSha())
                .chunkIndex(chunk.getChunkIndex())
                .startLine(chunk.getStartLine())
                .endLine(chunk.getEndLine())
                .content(includeContent ? chunk.getContent() : null)
                .createdAt(chunk.getCreatedAt())
                .build()
        );
        return PageResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public List<SearchResultDto> searchRepository(UUID userId, UUID repositoryId, SearchRequestDto request) {
        repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        int topK = request.getTopK() != null ? request.getTopK() : 10;
        return hybridSearchService.search(repositoryId, request.getQuery(), topK);
    }

    @Transactional(readOnly = true)
    public String getFileContent(UUID userId, UUID repositoryId, String filePath) {
        repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        if (filePath == null || filePath.isBlank() || filePath.contains("..")) {
            throw new ResourceNotFoundException("Invalid file path: " + filePath);
        }

        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        normalizedPath = normalizedPath.replace('\\', '/');

        Optional<RepositoryFile> fileOpt = repositoryFileRepository.findByRepositoryIdAndFilePath(repositoryId, normalizedPath);
        if (fileOpt.isEmpty() || Boolean.TRUE.equals(fileOpt.get().getSkipped())) {
            throw new ResourceNotFoundException("File not found or was skipped during indexing: " + normalizedPath);
        }

        Path repoDir = Path.of(gitHubConfig.getRepoBasePath(), repositoryId.toString()).toAbsolutePath().normalize();
        Path targetFile = repoDir.resolve(normalizedPath).normalize();

        if (!targetFile.startsWith(repoDir) || !java.nio.file.Files.exists(targetFile) || java.nio.file.Files.isDirectory(targetFile)) {
            throw new ResourceNotFoundException("File not found on disk: " + normalizedPath);
        }

        try {
            return java.nio.file.Files.readString(targetFile);
        } catch (Exception e) {
            log.error("Failed to read file {} for repository {}", normalizedPath, repositoryId, e);
            throw new RuntimeException("Could not read file content: " + e.getMessage(), e);
        }
    }

    private RepositorySummaryDto buildSummaryDto(GitHubRepository repo, IndexingJob job, boolean alreadyIndexed) {
        long totalFiles = repositoryFileRepository.countByRepositoryId(repo.getId());
        long skippedFiles = repositoryFileRepository.countByRepositoryIdAndSkipped(repo.getId(), true);

        return RepositorySummaryDto.builder()
                .id(repo.getId())
                .owner(repo.getOwner())
                .name(repo.getName())
                .fullName(repo.getFullName())
                .defaultBranch(repo.getDefaultBranch())
                .latestCommitSha(repo.getLatestCommitSha())
                .url(repo.getUrl())
                .sizeKb(repo.getSizeKb())
                .totalFiles(totalFiles)
                .skippedFiles(skippedFiles)
                .status(job != null ? job.getStatus() : IndexingStatus.PENDING)
                .errorMessage(job != null ? job.getErrorMessage() : null)
                .createdAt(repo.getCreatedAt())
                .alreadyIndexed(alreadyIndexed)
                .build();
    }
}
