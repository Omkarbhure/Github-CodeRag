package com.example.coderag.controller;

import com.example.coderag.dto.CodeChunkDto;
import com.example.coderag.dto.ImportRepositoryRequest;
import com.example.coderag.dto.PageResponse;
import com.example.coderag.dto.RepositoryDetailDto;
import com.example.coderag.dto.RepositoryFileDto;
import com.example.coderag.dto.RepositorySummaryDto;
import com.example.coderag.dto.SearchRequestDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.security.UserPrincipal;
import com.example.coderag.service.RepositoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final com.example.coderag.ratelimit.RateLimiterService rateLimiterService;

    public RepositoryController(
            RepositoryService repositoryService,
            com.example.coderag.ratelimit.RateLimiterService rateLimiterService
    ) {
        this.repositoryService = repositoryService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping
    public ResponseEntity<RepositorySummaryDto> importRepository(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ImportRepositoryRequest request
    ) {
        rateLimiterService.checkRateLimit(principal.getId(), com.example.coderag.ratelimit.RateLimiterService.ActionType.IMPORT);
        RepositorySummaryDto result = repositoryService.importRepository(principal.getId(), request);
        HttpStatus status = Boolean.TRUE.equals(result.getAlreadyIndexed()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping
    public ResponseEntity<List<RepositorySummaryDto>> getUserRepositories(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RepositorySummaryDto> repos = repositoryService.getUserRepositories(principal.getId());
        return ResponseEntity.ok(repos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepositoryDetailDto> getRepositoryDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        RepositoryDetailDto detail = repositoryService.getRepositoryDetail(principal.getId(), id);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<RepositorySummaryDto> reindexRepository(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        RepositorySummaryDto result = repositoryService.reindexRepository(principal.getId(), id);
        return ResponseEntity.accepted().body(result);
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<PageResponse<RepositoryFileDto>> getRepositoryFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean skipped,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "filePath") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        PageResponse<RepositoryFileDto> files = repositoryService.getRepositoryFiles(principal.getId(), id, skipped, pageable);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/files/content")
    public ResponseEntity<String> getFileContent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("path") String path
    ) {
        String content = repositoryService.getFileContent(principal.getId(), id, path);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<PageResponse<CodeChunkDto>> getRepositoryChunks(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeContent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "filePath") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        PageResponse<CodeChunkDto> chunks = repositoryService.getRepositoryChunks(principal.getId(), id, includeContent, pageable);
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/{id}/search")
    public ResponseEntity<List<SearchResultDto>> searchRepository(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SearchRequestDto request
    ) {
        List<SearchResultDto> results = repositoryService.searchRepository(principal.getId(), id, request);
        return ResponseEntity.ok(results);
    }
}
