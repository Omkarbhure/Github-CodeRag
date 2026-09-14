package com.example.coderag.vector;

import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.model.CodeChunk;

import java.util.List;
import java.util.UUID;

public interface VectorStoreService {

    void ensureCollectionExists();

    void upsert(CodeChunk chunk, float[] vector);

    void upsertBatch(List<CodeChunk> chunks, List<float[]> vectors);

    void deleteByRepositoryId(UUID repositoryId);

    List<SearchResultDto> search(UUID repositoryId, float[] queryVector, int topK);

    long countByRepositoryId(UUID repositoryId);
}
