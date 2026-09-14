package com.example.coderag.intelligence;

import com.example.coderag.chat.AnswerGenerationService;
import com.example.coderag.chat.ContextBuilder;
import com.example.coderag.dto.ArchitectureOverviewDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.exception.ResourceNotFoundException;
import com.example.coderag.model.ArchitectureOverview;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.ArchitectureOverviewRepository;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ArchitectureOverviewService {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureOverviewService.class);

    private static final int MAX_SAMPLED_FILES = 18;
    private static final int ARCHITECTURE_CHAR_BUDGET = 14000;

    private final GitHubRepositoryRepository repositoryRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;
    private final ArchitectureOverviewRepository architectureOverviewRepository;
    private final ContextBuilder contextBuilder;
    private final AnswerGenerationService answerGenerationService;

    public ArchitectureOverviewService(
            GitHubRepositoryRepository repositoryRepository,
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository,
            ArchitectureOverviewRepository architectureOverviewRepository,
            ContextBuilder contextBuilder,
            AnswerGenerationService answerGenerationService
    ) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
        this.architectureOverviewRepository = architectureOverviewRepository;
        this.contextBuilder = contextBuilder;
        this.answerGenerationService = answerGenerationService;
    }

    @Transactional
    public ArchitectureOverviewDto getOrGenerateOverview(UUID userId, UUID repositoryId, boolean force) {
        GitHubRepository repo = repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        String commitSha = repo.getLatestCommitSha();

        // 1. Check cache unless force is requested
        if (!force && commitSha != null) {
            Optional<ArchitectureOverview> cachedOpt = architectureOverviewRepository
                    .findTopByRepositoryIdAndCommitShaOrderByCreatedAtDesc(repo.getId(), commitSha);
            if (cachedOpt.isPresent()) {
                log.info("Returning cached architecture overview for repo {} commit {}", repo.getFullName(), commitSha);
                return ArchitectureOverviewDto.fromEntity(cachedOpt.get());
            }
        }

        log.info("Generating new architecture overview for repo {} (force={})", repo.getFullName(), force);

        // 2. Broad Sampling across representative files
        List<RepositoryFile> files = repositoryFileRepository.findByRepositoryId(repo.getId());
        List<RepositoryFile> sampledFiles = selectRepresentativeFiles(files);

        // 3. Retrieve chunks for sampled files
        List<SearchResultDto> sampledChunks = retrieveSampledChunks(repo.getId(), sampledFiles);

        // 4. Assemble context
        String codeContext = contextBuilder.buildContext(sampledChunks, MAX_SAMPLED_FILES, ARCHITECTURE_CHAR_BUDGET);

        // 5. Query LLM
        String prompt = """
                Analyze the provided representative files, configuration manifests, and module structure of this codebase.
                Produce a comprehensive, well-structured Architecture Overview with the following sections:
                
                ### 1. High-Level Summary
                A clear explanation of what this project does, its primary goal, and its high-level domain.
                
                ### 2. Key Technologies & Frameworks
                Identify all detected programming languages, frameworks (e.g. Spring Boot, Next.js, React), databases, tools, and libraries.
                
                ### 3. Core Architecture & Modules
                Describe the key packages/directories, their distinct responsibilities, and how they interact.
                
                ### 4. Data Flow & Notable Patterns
                Explain key end-to-end workflows (e.g. request routing, authentication, processing pipelines) and architectural design patterns used.
                
                Always cite relevant files and line ranges inline using [filePath:startLine-endLine] when referencing specific components.
                """;

        String overviewText = answerGenerationService.generateAnswer(prompt, codeContext, Collections.emptyList());

        // 6. Extract structured tags (technologies & modules)
        List<String> detectedTechnologies = extractTechnologies(overviewText, sampledFiles);
        List<String> detectedModules = extractModules(sampledFiles);

        // 7. Save to database
        ArchitectureOverview overview = ArchitectureOverview.builder()
                .repositoryId(repo.getId())
                .commitSha(commitSha)
                .overviewText(overviewText)
                .technologies(String.join(", ", detectedTechnologies))
                .modules(String.join("\n", detectedModules))
                .createdAt(OffsetDateTime.now())
                .build();

        overview = architectureOverviewRepository.save(overview);

        return ArchitectureOverviewDto.fromEntity(overview);
    }

    public List<RepositoryFile> selectRepresentativeFiles(List<RepositoryFile> allFiles) {
        if (allFiles == null || allFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<RepositoryFile> validFiles = allFiles.stream()
                .filter(f -> !Boolean.TRUE.equals(f.getSkipped()))
                .collect(Collectors.toList());

        if (validFiles.isEmpty()) {
            validFiles = allFiles;
        }

        Set<RepositoryFile> selected = new LinkedHashSet<>();

        // Priority 1: README and documentation files
        for (RepositoryFile f : validFiles) {
            String lower = f.getFilePath().toLowerCase();
            if (lower.equals("readme.md") || lower.equals("readme") || lower.startsWith("readme.") || lower.contains("architecture")) {
                selected.add(f);
            }
        }

        // Priority 2: Root project descriptors & build configs
        for (RepositoryFile f : validFiles) {
            String lower = f.getFilePath().toLowerCase();
            if (lower.equals("pom.xml") || lower.equals("package.json") || lower.equals("build.gradle") ||
                lower.equals("go.mod") || lower.equals("cargo.toml") || lower.equals("requirements.txt") ||
                lower.equals("docker-compose.yml") || lower.contains("application.yml") || lower.contains("application.properties") ||
                lower.equals("tsconfig.json") || lower.equals("next.config.js") || lower.equals("next.config.mjs")) {
                selected.add(f);
            }
        }

        // Priority 3: Application entry points & main classes
        for (RepositoryFile f : validFiles) {
            String path = f.getFilePath();
            if (path.endsWith("Application.java") || path.endsWith("Main.java") ||
                path.endsWith("index.ts") || path.endsWith("index.tsx") || path.endsWith("App.tsx") ||
                path.endsWith("main.go") || path.endsWith("main.py") || path.endsWith("server.ts") || path.endsWith("app.py")) {
                selected.add(f);
            }
        }

        // Priority 4: Group remaining files by top-level directory and pick representative files
        Map<String, List<RepositoryFile>> byDirectory = new HashMap<>();
        for (RepositoryFile f : validFiles) {
            if (selected.contains(f)) continue;
            String dir = getTopLevelDirectory(f.getFilePath());
            byDirectory.computeIfAbsent(dir, k -> new ArrayList<>()).add(f);
        }

        for (Map.Entry<String, List<RepositoryFile>> entry : byDirectory.entrySet()) {
            List<RepositoryFile> dirFiles = entry.getValue();
            // Sort by smaller size / likely high-level interfaces or controllers
            dirFiles.sort(Comparator.comparingLong(f -> f.getSizeBytes() != null ? f.getSizeBytes() : 0L));
            int countToTake = Math.min(2, dirFiles.size());
            for (int i = 0; i < countToTake; i++) {
                if (selected.size() < MAX_SAMPLED_FILES) {
                    selected.add(dirFiles.get(i));
                }
            }
        }

        // If still under limit, fill up with remaining files
        for (RepositoryFile f : validFiles) {
            if (selected.size() >= MAX_SAMPLED_FILES) break;
            selected.add(f);
        }

        return new ArrayList<>(selected);
    }

    private List<SearchResultDto> retrieveSampledChunks(UUID repositoryId, List<RepositoryFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<CodeChunk> allChunks = codeChunkRepository.findByRepositoryId(repositoryId, Pageable.unpaged()).getContent();
        Map<UUID, List<CodeChunk>> chunksByFileId = allChunks.stream()
                .collect(Collectors.groupingBy(CodeChunk::getRepositoryFileId));

        List<SearchResultDto> results = new ArrayList<>();
        for (RepositoryFile file : files) {
            List<CodeChunk> fileChunks = chunksByFileId.get(file.getId());
            if (fileChunks != null && !fileChunks.isEmpty()) {
                // Sort by chunk index to pick top chunk (entry point / class declaration)
                fileChunks.sort(Comparator.comparingInt(CodeChunk::getChunkIndex));
                CodeChunk topChunk = fileChunks.get(0);
                results.add(SearchResultDto.builder()
                        .id(topChunk.getId())
                        .repositoryId(repositoryId)
                        .repositoryFileId(file.getId())
                        .filePath(topChunk.getFilePath())
                        .language(file.getLanguage())
                        .startLine(topChunk.getStartLine())
                        .endLine(topChunk.getEndLine())
                        .chunkIndex(topChunk.getChunkIndex())
                        .commitSha(topChunk.getCommitSha())
                        .content(topChunk.getContent())
                        .score(1.0)
                        .build());
            }
        }

        return results;
    }

    private String getTopLevelDirectory(String filePath) {
        if (filePath == null || filePath.isBlank()) return "root";
        String normalized = filePath.replace('\\', '/');
        int firstSlash = normalized.indexOf('/');
        if (firstSlash == -1) return "root";
        return normalized.substring(0, firstSlash);
    }

    private List<String> extractTechnologies(String overviewText, List<RepositoryFile> sampledFiles) {
        Set<String> techs = new LinkedHashSet<>();

        // Detect from file extensions
        for (RepositoryFile f : sampledFiles) {
            String path = f.getFilePath().toLowerCase();
            if (path.endsWith(".java")) techs.add("Java");
            if (path.endsWith(".ts") || path.endsWith(".tsx")) techs.add("TypeScript");
            if (path.endsWith(".js") || path.endsWith(".jsx")) techs.add("JavaScript");
            if (path.endsWith(".py")) techs.add("Python");
            if (path.endsWith(".go")) techs.add("Go");
            if (path.endsWith(".rs")) techs.add("Rust");
            if (path.contains("pom.xml")) {
                techs.add("Maven");
                techs.add("Spring Boot");
            }
            if (path.contains("package.json")) techs.add("Node.js");
            if (path.contains("docker-compose.yml") || path.contains("dockerfile")) techs.add("Docker");
        }

        // Detect from overview text keywords
        if (overviewText != null) {
            String lower = overviewText.toLowerCase();
            if (lower.contains("postgresql") || lower.contains("postgres")) techs.add("PostgreSQL");
            if (lower.contains("qdrant")) techs.add("Qdrant");
            if (lower.contains("redis")) techs.add("Redis");
            if (lower.contains("next.js") || lower.contains("nextjs")) techs.add("Next.js");
            if (lower.contains("react")) techs.add("React");
            if (lower.contains("tailwind")) techs.add("Tailwind CSS");
            if (lower.contains("flyway")) techs.add("Flyway");
            if (lower.contains("hibernate") || lower.contains("jpa")) techs.add("Hibernate JPA");
            if (lower.contains("gemini")) techs.add("Google Gemini");
        }

        return new ArrayList<>(techs);
    }

    private List<String> extractModules(List<RepositoryFile> files) {
        Set<String> modules = new LinkedHashSet<>();
        for (RepositoryFile f : files) {
            String path = f.getFilePath().replace('\\', '/');
            String[] parts = path.split("/");
            if (parts.length > 1) {
                modules.add(parts[0] + " (" + parts[1] + ")");
            } else {
                modules.add(parts[0]);
            }
        }
        return modules.stream().limit(10).collect(Collectors.toList());
    }
}
