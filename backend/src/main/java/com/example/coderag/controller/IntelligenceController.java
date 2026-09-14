package com.example.coderag.controller;

import com.example.coderag.dto.ArchitectureOverviewDto;
import com.example.coderag.dto.BugInvestigationRequestDto;
import com.example.coderag.dto.BugInvestigationResponseDto;
import com.example.coderag.dto.RelatedFileDto;
import com.example.coderag.intelligence.ArchitectureOverviewService;
import com.example.coderag.intelligence.BugInvestigationService;
import com.example.coderag.intelligence.RelatedFilesService;
import com.example.coderag.security.UserPrincipal;
import jakarta.validation.Valid;
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
public class IntelligenceController {

    private final ArchitectureOverviewService architectureOverviewService;
    private final BugInvestigationService bugInvestigationService;
    private final RelatedFilesService relatedFilesService;
    private final com.example.coderag.ratelimit.RateLimiterService rateLimiterService;

    public IntelligenceController(
            ArchitectureOverviewService architectureOverviewService,
            BugInvestigationService bugInvestigationService,
            RelatedFilesService relatedFilesService,
            com.example.coderag.ratelimit.RateLimiterService rateLimiterService
    ) {
        this.architectureOverviewService = architectureOverviewService;
        this.bugInvestigationService = bugInvestigationService;
        this.relatedFilesService = relatedFilesService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/{id}/architecture-overview")
    public ResponseEntity<ArchitectureOverviewDto> getArchitectureOverview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        rateLimiterService.checkRateLimit(principal.getId(), com.example.coderag.ratelimit.RateLimiterService.ActionType.INTELLIGENCE);
        ArchitectureOverviewDto overview = architectureOverviewService.getOrGenerateOverview(principal.getId(), id, force);
        return ResponseEntity.ok(overview);
    }

    @PostMapping("/{id}/investigate-bug")
    public ResponseEntity<BugInvestigationResponseDto> investigateBug(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody BugInvestigationRequestDto request
    ) {
        rateLimiterService.checkRateLimit(principal.getId(), com.example.coderag.ratelimit.RateLimiterService.ActionType.INTELLIGENCE);
        BugInvestigationResponseDto response = bugInvestigationService.investigateBug(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/files/related")
    public ResponseEntity<List<RelatedFileDto>> getRelatedFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("path") String path,
            @RequestParam(defaultValue = "10") int topK
    ) {
        List<RelatedFileDto> relatedFiles = relatedFilesService.findRelatedFiles(principal.getId(), id, path, topK);
        return ResponseEntity.ok(relatedFiles);
    }
}
