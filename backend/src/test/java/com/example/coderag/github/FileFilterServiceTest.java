package com.example.coderag.github;

import com.example.coderag.model.RepositoryFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileFilterServiceTest {

    private FileFilterService fileFilterService;
    private GitHubConfig gitHubConfig;

    @BeforeEach
    void setUp() {
        gitHubConfig = new GitHubConfig();
        ReflectionTestUtils.setField(gitHubConfig, "maxFileSizeMb", 10L);
        fileFilterService = new FileFilterService(gitHubConfig);
    }

    @Test
    void isIgnoredPath_ShouldDetectIgnoredDirectories() {
        assertTrue(fileFilterService.isIgnoredPath("node_modules/react/index.js"));
        assertTrue(fileFilterService.isIgnoredPath(".git/HEAD"));
        assertTrue(fileFilterService.isIgnoredPath("src/.next/cache/data.json"));
        assertTrue(fileFilterService.isIgnoredPath("target/classes/App.class"));
        assertFalse(fileFilterService.isIgnoredPath("src/main/java/App.java"));
    }

    @Test
    void inferLanguage_ShouldCorrectlyMapExtensions() {
        assertEquals("Java", fileFilterService.inferLanguage("App.java"));
        assertEquals("TypeScript", fileFilterService.inferLanguage("index.ts"));
        assertEquals("Python", fileFilterService.inferLanguage("script.py"));
        assertEquals("YAML", fileFilterService.inferLanguage("config.yml"));
        assertEquals("Unknown", fileFilterService.inferLanguage("unknown.xyz"));
    }

    @Test
    void scanAndFilterRepository_ShouldScanValidFilesAndSkipOversizedFiles(@TempDir Path tempDir) throws IOException {
        UUID repoId = UUID.randomUUID();

        // 1. Valid Java file
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Main.java"), "public class Main {}");

        // 2. Ignored node_modules file
        Path nmDir = tempDir.resolve("node_modules/pkg");
        Files.createDirectories(nmDir);
        Files.writeString(nmDir.resolve("index.js"), "console.log('hi');");

        // 3. Ignored binary file
        Path assetDir = tempDir.resolve("assets");
        Files.createDirectories(assetDir);
        Files.write(assetDir.resolve("logo.png"), new byte[]{1, 2, 3});

        // 4. Oversized file (simulate by setting maxFileSizeMb low)
        ReflectionTestUtils.setField(gitHubConfig, "maxFileSizeMb", 0L); // 0MB forces skip
        Files.writeString(srcDir.resolve("Large.java"), "public class Large {}");

        List<RepositoryFile> files = fileFilterService.scanAndFilterRepository(tempDir, repoId);

        assertEquals(2, files.size()); // Main.java and Large.java

        RepositoryFile mainFile = files.stream().filter(f -> f.getFilePath().endsWith("Main.java")).findFirst().orElseThrow();
        assertTrue(mainFile.getSkipped()); // skipped because maxFileSizeMb = 0

        assertTrue(files.stream().noneMatch(f -> f.getFilePath().contains("node_modules")));
        assertTrue(files.stream().noneMatch(f -> f.getFilePath().endsWith(".png")));
    }
}
