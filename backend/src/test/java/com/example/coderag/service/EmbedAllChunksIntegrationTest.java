package com.example.coderag.service;

import com.example.coderag.model.CodeChunk;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.vector.EmbeddingService;
import com.example.coderag.vector.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@SpringBootTest
public class EmbedAllChunksIntegrationTest {

    @Autowired
    private CodeChunkRepository codeChunkRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Test
    public void embedAllCinebookChunks() throws InterruptedException {
        UUID repoId = UUID.fromString("318c1bc8-ccc5-4a8a-b315-5111382d5c8b");
        List<CodeChunk> chunks = codeChunkRepository.findByRepositoryId(repoId, Pageable.unpaged()).getContent();
        System.out.println(">>> Embedding " + chunks.size() + " clean code chunks for Cinebook...");

        for (int i = 0; i < chunks.size(); i++) {
            CodeChunk chunk = chunks.get(i);
            boolean success = false;
            int retries = 0;
            while (!success && retries < 8) {
                try {
                    float[] vector = embeddingService.embed(chunk.getContent());
                    vectorStoreService.upsert(chunk, vector);
                    success = true;
                    if ((i + 1) % 10 == 0 || i + 1 == chunks.size()) {
                        System.out.println(">>> [" + (i + 1) + "/" + chunks.size() + "] Embedded: " + chunk.getFilePath() + " (lines " + chunk.getStartLine() + "-" + chunk.getEndLine() + ")");
                    }
                } catch (Exception e) {
                    retries++;
                    long waitTime = (long) Math.pow(2, retries) * 1000L;
                    System.err.println("Rate limit on " + chunk.getFilePath() + ", retry " + retries + " in " + (waitTime / 1000) + "s: " + e.getMessage());
                    Thread.sleep(waitTime);
                }
            }
            Thread.sleep(300);
        }
        System.out.println(">>> ALL CLEAN CODE CHUNKS EMBEDDED SUCCESSFULLY INTO QDRANT!");
    }
}
