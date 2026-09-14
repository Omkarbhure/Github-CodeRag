package com.example.coderag.chunker;

import com.example.coderag.model.CodeChunk;
import com.example.coderag.model.RepositoryFile;
import com.example.coderag.repository.CodeChunkRepository;
import com.example.coderag.repository.RepositoryFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ChunkerServiceTest {

    @Mock
    private LowValueDetectorService lowValueDetectorService;

    @Mock
    private RepositoryFileRepository repositoryFileRepository;

    @Mock
    private CodeChunkRepository codeChunkRepository;

    private ChunkerService chunkerService;

    @BeforeEach
    void setUp() {
        chunkerService = new ChunkerService(lowValueDetectorService, repositoryFileRepository, codeChunkRepository);
    }

    @Test
    void generateChunksForFile_SingleChunkForShortFile() {
        UUID repoId = UUID.randomUUID();
        RepositoryFile file = RepositoryFile.builder()
                .id(UUID.randomUUID())
                .repositoryId(repoId)
                .filePath("src/main/App.java")
                .language("Java")
                .sizeBytes(500L)
                .build();

        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            lines.add("line " + i);
        }

        List<CodeChunk> chunks = chunkerService.generateChunksForFile(repoId, file, lines, "commit123", 120, 20);

        assertEquals(1, chunks.size());
        CodeChunk chunk = chunks.get(0);
        assertEquals(0, chunk.getChunkIndex());
        assertEquals(1, chunk.getStartLine());
        assertEquals(45, chunk.getEndLine());
        assertEquals(repoId, chunk.getRepositoryId());
        assertEquals("src/main/App.java", chunk.getFilePath());
        assertEquals("commit123", chunk.getCommitSha());
        assertTrue(chunk.getContent().startsWith("line 1"));
        assertTrue(chunk.getContent().endsWith("line 45"));
    }

    @Test
    void generateChunksForFile_MultiChunkWithOverlap() {
        UUID repoId = UUID.randomUUID();
        RepositoryFile file = RepositoryFile.builder()
                .id(UUID.randomUUID())
                .repositoryId(repoId)
                .filePath("src/large.ts")
                .language("TypeScript")
                .sizeBytes(2500L)
                .build();

        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 250; i++) {
            lines.add("line " + i);
        }

        // Window = 120, Overlap = 20 -> Step = 100
        // Chunk 0: index 0..120 -> lines 1..120
        // Chunk 1: index 100..220 -> lines 101..220
        // Chunk 2: index 200..250 -> lines 201..250
        List<CodeChunk> chunks = chunkerService.generateChunksForFile(repoId, file, lines, "commit456", 120, 20);

        assertEquals(3, chunks.size());

        // Chunk 0
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(1, chunks.get(0).getStartLine());
        assertEquals(120, chunks.get(0).getEndLine());
        assertTrue(chunks.get(0).getContent().startsWith("line 1\n"));
        assertTrue(chunks.get(0).getContent().endsWith("\nline 120"));

        // Chunk 1
        assertEquals(1, chunks.get(1).getChunkIndex());
        assertEquals(101, chunks.get(1).getStartLine());
        assertEquals(220, chunks.get(1).getEndLine());
        assertTrue(chunks.get(1).getContent().startsWith("line 101\n"));
        assertTrue(chunks.get(1).getContent().endsWith("\nline 220"));

        // Chunk 2
        assertEquals(2, chunks.get(2).getChunkIndex());
        assertEquals(201, chunks.get(2).getStartLine());
        assertEquals(250, chunks.get(2).getEndLine());
        assertTrue(chunks.get(2).getContent().startsWith("line 201\n"));
        assertTrue(chunks.get(2).getContent().endsWith("line 250"));
    }
}
