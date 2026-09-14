package com.example.coderag.repository;

import com.example.coderag.model.CodeChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, UUID> {
    Page<CodeChunk> findByRepositoryId(UUID repositoryId, Pageable pageable);
    Page<CodeChunk> findByRepositoryFileId(UUID repositoryFileId, Pageable pageable);
    long countByRepositoryId(UUID repositoryId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByRepositoryId(UUID repositoryId);

    @Query(value = """
            SELECT c.id AS id,
                   c.repository_id AS repositoryId,
                   c.repository_file_id AS repositoryFileId,
                   c.file_path AS filePath,
                   c.start_line AS startLine,
                   c.end_line AS endLine,
                   c.chunk_index AS chunkIndex,
                   c.commit_sha AS commitSha,
                   c.content AS content,
                   CAST(ts_rank(c.content_tsv, plainto_tsquery('english', :query)) AS float8) AS score
            FROM code_chunks c
            WHERE c.repository_id = :repositoryId
              AND c.content_tsv @@ plainto_tsquery('english', :query)
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<CodeChunkSearchProjection> searchByKeyword(
            @Param("repositoryId") UUID repositoryId,
            @Param("query") String query,
            @Param("limit") int limit
    );
}

