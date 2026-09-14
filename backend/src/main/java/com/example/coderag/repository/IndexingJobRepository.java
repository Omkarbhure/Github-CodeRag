package com.example.coderag.repository;

import com.example.coderag.model.IndexingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndexingJobRepository extends JpaRepository<IndexingJob, UUID> {

    Optional<IndexingJob> findTopByRepositoryIdOrderByStartedAtDesc(UUID repositoryId);

    List<IndexingJob> findByRepositoryIdOrderByStartedAtDesc(UUID repositoryId);

    List<IndexingJob> findByStatusIn(List<com.example.coderag.model.IndexingStatus> statuses);
}
