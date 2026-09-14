package com.example.coderag.repository;

import com.example.coderag.model.ArchitectureOverview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchitectureOverviewRepository extends JpaRepository<ArchitectureOverview, UUID> {
    Optional<ArchitectureOverview> findTopByRepositoryIdAndCommitShaOrderByCreatedAtDesc(UUID repositoryId, String commitSha);
    Optional<ArchitectureOverview> findTopByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
    void deleteByRepositoryId(UUID repositoryId);
}
