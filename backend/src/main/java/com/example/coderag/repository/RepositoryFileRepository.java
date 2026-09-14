package com.example.coderag.repository;

import com.example.coderag.model.RepositoryFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, UUID> {

    List<RepositoryFile> findByRepositoryId(UUID repositoryId);

    List<RepositoryFile> findByRepositoryIdAndSkipped(UUID repositoryId, Boolean skipped);

    Page<RepositoryFile> findByRepositoryId(UUID repositoryId, Pageable pageable);

    Page<RepositoryFile> findByRepositoryIdAndSkipped(UUID repositoryId, Boolean skipped, Pageable pageable);

    long countByRepositoryId(UUID repositoryId);

    long countByRepositoryIdAndSkipped(UUID repositoryId, Boolean skipped);

    Optional<RepositoryFile> findByRepositoryIdAndFilePath(UUID repositoryId, String filePath);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByRepositoryId(UUID repositoryId);
}
