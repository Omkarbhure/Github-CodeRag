package com.example.coderag.intelligence;

import com.example.coderag.chat.AnswerGenerationService;
import com.example.coderag.chat.ContextBuilder;
import com.example.coderag.dto.BugInvestigationRequestDto;
import com.example.coderag.dto.BugInvestigationResponseDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.exception.ResourceNotFoundException;
import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import com.example.coderag.service.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BugInvestigationService {

    private static final Logger log = LoggerFactory.getLogger(BugInvestigationService.class);

    private final GitHubRepositoryRepository repositoryRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;
    private final HybridSearchService hybridSearchService;
    private final ContextBuilder contextBuilder;
    private final AnswerGenerationService answerGenerationService;

    // Regex patterns for parsing stack traces and error messages
    private static final Pattern JAVA_STACK_PATTERN = Pattern.compile("(?:at\\s+)?([a-zA-Z0-9_$.]+)\\.([a-zA-Z0-9_$]+)\\(([a-zA-Z0-9_$.]+\\.java):(\\d+)\\)");
    private static final Pattern PYTHON_STACK_PATTERN = Pattern.compile("File \"([^\"]+)\", line (\\d+), in ([a-zA-Z0-9_]+)");
    private static final Pattern JS_STACK_PATTERN = Pattern.compile("(?:at\\s+)?(?:async\\s+)?(?:[a-zA-Z0-9_$<>.]+\\s+\\()?([a-zA-Z0-9_./\\\\-]+(?:\\.[a-zA-Z0-9]+)):(\\d+):(\\d+)\\)?");
    private static final Pattern FILE_LINE_PATTERN = Pattern.compile("([a-zA-Z0-9_\\-./\\\\]+\\.(?:java|ts|tsx|js|jsx|py|go|rs|cpp|c|cs)):(\\d+)");

    public BugInvestigationService(
            GitHubRepositoryRepository repositoryRepository,
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository,
            HybridSearchService hybridSearchService,
            ContextBuilder contextBuilder,
            AnswerGenerationService answerGenerationService
    ) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
        this.hybridSearchService = hybridSearchService;
        this.contextBuilder = contextBuilder;
        this.answerGenerationService = answerGenerationService;
    }

    @Transactional(readOnly = true)
    public BugInvestigationResponseDto investigateBug(UUID userId, UUID repositoryId, BugInvestigationRequestDto request) {
        GitHubRepository repo = repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        String errorText = request.getErrorText() != null ? request.getErrorText().trim() : "";
        if (errorText.isBlank()) {
            return BugInvestigationResponseDto.builder()
                    .analysis("Please provide an error message or stack trace to investigate.")
                    .identifiedFiles(Collections.emptyList())
                    .relevantChunks(Collections.emptyList())
                    .build();
        }

        log.info("Investigating bug for repository: {}", repo.getFullName());

        // 1. Parse error text for explicit stack trace signals
        Set<ParsedSignal> signals = parseStackTraceSignals(errorText);
        List<String> identifiedFiles = signals.stream().map(ParsedSignal::fileName).distinct().collect(Collectors.toList());

        // 2. Direct lookup for identified file signals
        List<SearchResultDto> directMatches = findDirectMatches(repo.getId(), signals);

        // 3. Hybrid search for surrounding context and error terms
        List<SearchResultDto> hybridMatches = hybridSearchService.search(repo.getId(), errorText, 8);

        // 4. Merge and deduplicate candidates (prioritizing direct matches)
        List<SearchResultDto> combinedCandidates = mergeCandidates(directMatches, hybridMatches);

        // 5. Build context
        String codeContext = contextBuilder.buildContext(combinedCandidates, 10, 10000);

        // 6. Prompt LLM for grounded diagnosis
        String prompt = String.format("""
                You are an expert software engineer and debugging specialist.
                A developer has reported the following error message / stack trace from their application:
                
                ```
                %s
                ```
                
                Analyze the provided code snippets to investigate the root cause and provide clear guidance:
                
                ### 1. Root Cause Analysis
                Explain why the error is occurring based on the code logic and stack trace.
                
                ### 2. Culprit Code & Direct Citations
                Cite the exact files and line numbers responsible for the issue using [filePath:startLine-endLine].
                
                ### 3. Step-by-Step Fix
                Provide code snippets showing how to fix or prevent the issue.
                
                ### 4. Diagnostic Confidence & Missing Information
                If the error trace or code context does not contain sufficient details to localize the bug with high confidence, state what additional log lines or variables are needed rather than guessing.
                """, errorText);

        String analysis = answerGenerationService.generateAnswer(prompt, codeContext, Collections.emptyList());

        return BugInvestigationResponseDto.builder()
                .analysis(analysis)
                .identifiedFiles(identifiedFiles)
                .relevantChunks(combinedCandidates)
                .build();
    }

    public Set<ParsedSignal> parseStackTraceSignals(String errorText) {
        Set<ParsedSignal> signals = new LinkedHashSet<>();
        if (errorText == null || errorText.isBlank()) {
            return signals;
        }

        // 1. Java stack trace lines: at com.example.Auth.login(Auth.java:42)
        Matcher javaMatcher = JAVA_STACK_PATTERN.matcher(errorText);
        while (javaMatcher.find()) {
            String className = javaMatcher.group(1);
            String methodName = javaMatcher.group(2);
            String fileName = javaMatcher.group(3);
            int line = Integer.parseInt(javaMatcher.group(4));
            signals.add(new ParsedSignal(fileName, className, methodName, line));
        }

        // 2. Python stack trace lines: File "app.py", line 42, in login
        Matcher pyMatcher = PYTHON_STACK_PATTERN.matcher(errorText);
        while (pyMatcher.find()) {
            String filePath = pyMatcher.group(1);
            int line = Integer.parseInt(pyMatcher.group(2));
            String funcName = pyMatcher.group(3);
            String fileName = extractBaseName(filePath);
            signals.add(new ParsedSignal(fileName, null, funcName, line));
        }

        // 3. JS / TS stack trace lines: at Auth.login (src/auth.ts:42:10)
        Matcher jsMatcher = JS_STACK_PATTERN.matcher(errorText);
        while (jsMatcher.find()) {
            String filePath = jsMatcher.group(1);
            int line = Integer.parseInt(jsMatcher.group(2));
            String fileName = extractBaseName(filePath);
            signals.add(new ParsedSignal(fileName, null, null, line));
        }

        // 4. File line pattern: src/AuthService.java:42
        Matcher fileLineMatcher = FILE_LINE_PATTERN.matcher(errorText);
        while (fileLineMatcher.find()) {
            String path = fileLineMatcher.group(1);
            int line = Integer.parseInt(fileLineMatcher.group(2));
            String baseName = extractBaseName(path);
            boolean alreadyPresent = signals.stream().anyMatch(s -> s.fileName().equals(baseName) && s.line() == line);
            if (!alreadyPresent) {
                signals.add(new ParsedSignal(baseName, null, null, line));
            }
        }

        return signals;
    }

    private List<SearchResultDto> findDirectMatches(UUID repositoryId, Set<ParsedSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return Collections.emptyList();
        }

        List<RepositoryFile> files = repositoryFileRepository.findByRepositoryId(repositoryId);
        List<CodeChunk> allChunks = codeChunkRepository.findByRepositoryId(repositoryId, Pageable.unpaged()).getContent();
        Map<UUID, List<CodeChunk>> chunksByFileId = allChunks.stream().collect(Collectors.groupingBy(CodeChunk::getRepositoryFileId));

        List<SearchResultDto> directMatches = new ArrayList<>();
        Set<UUID> matchedChunkIds = new HashSet<>();

        for (ParsedSignal signal : signals) {
            for (RepositoryFile f : files) {
                String path = f.getFilePath().replace('\\', '/');
                if (path.endsWith("/" + signal.fileName) || path.equals(signal.fileName)) {
                    List<CodeChunk> fileChunks = chunksByFileId.get(f.getId());
                    if (fileChunks != null) {
                        for (CodeChunk chunk : fileChunks) {
                            if (matchedChunkIds.contains(chunk.getId())) continue;

                            // Check if chunk covers the line number
                            boolean coversLine = signal.line > 0 && chunk.getStartLine() <= signal.line && chunk.getEndLine() >= signal.line;
                            if (coversLine || fileChunks.size() == 1) {
                                matchedChunkIds.add(chunk.getId());
                                directMatches.add(SearchResultDto.builder()
                                        .id(chunk.getId())
                                        .repositoryId(repositoryId)
                                        .repositoryFileId(f.getId())
                                        .filePath(chunk.getFilePath())
                                        .language(f.getLanguage())
                                        .startLine(chunk.getStartLine())
                                        .endLine(chunk.getEndLine())
                                        .chunkIndex(chunk.getChunkIndex())
                                        .commitSha(chunk.getCommitSha())
                                        .content(chunk.getContent())
                                        .score(1.0)
                                        .finalScore(1.0)
                                        .keywordScore(1.0)
                                        .vectorScore(1.0)
                                        .build());
                            }
                        }
                    }
                }
            }
        }

        return directMatches;
    }

    private List<SearchResultDto> mergeCandidates(List<SearchResultDto> directMatches, List<SearchResultDto> hybridMatches) {
        Map<UUID, SearchResultDto> map = new HashMap<>();
        List<SearchResultDto> merged = new ArrayList<>();

        if (directMatches != null) {
            for (SearchResultDto d : directMatches) {
                if (d.getId() != null && !map.containsKey(d.getId())) {
                    map.put(d.getId(), d);
                    merged.add(d);
                }
            }
        }

        if (hybridMatches != null) {
            for (SearchResultDto h : hybridMatches) {
                if (h.getId() != null && !map.containsKey(h.getId())) {
                    map.put(h.getId(), h);
                    merged.add(h);
                }
            }
        }

        return merged;
    }

    private String extractBaseName(String filePath) {
        if (filePath == null) return "";
        String normalized = filePath.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        return idx != -1 ? normalized.substring(idx + 1) : normalized;
    }

    public record ParsedSignal(String fileName, String className, String methodName, int line) {}
}
