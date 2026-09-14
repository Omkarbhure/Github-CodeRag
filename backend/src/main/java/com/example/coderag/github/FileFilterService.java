package com.example.coderag.github;

import com.example.coderag.model.RepositoryFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileFilterService {

    private static final Logger log = LoggerFactory.getLogger(FileFilterService.class);

    private final GitHubConfig gitHubConfig;

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            "node_modules",
            "target",
            "build",
            "dist",
            ".next",
            "coverage",
            "vendor",
            ".idea",
            ".vscode"
    );

    private static final Set<String> IGNORED_BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".mp4", ".zip", ".jar", ".exe",
            ".pdf", ".ico", ".svg", ".lock", ".wasm", ".class", ".pyc", ".tar",
            ".gz", ".bin", ".woff", ".woff2", ".ttf", ".eot", ".mp3", ".wav",
            ".avi", ".mov", ".7z", ".rar", ".iso", ".dll", ".so", ".dylib"
    );

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = new HashMap<>();

    static {
        EXTENSION_TO_LANGUAGE.put(".java", "Java");
        EXTENSION_TO_LANGUAGE.put(".js", "JavaScript");
        EXTENSION_TO_LANGUAGE.put(".jsx", "JavaScript (JSX)");
        EXTENSION_TO_LANGUAGE.put(".ts", "TypeScript");
        EXTENSION_TO_LANGUAGE.put(".tsx", "TypeScript (TSX)");
        EXTENSION_TO_LANGUAGE.put(".py", "Python");
        EXTENSION_TO_LANGUAGE.put(".go", "Go");
        EXTENSION_TO_LANGUAGE.put(".cpp", "C++");
        EXTENSION_TO_LANGUAGE.put(".c", "C");
        EXTENSION_TO_LANGUAGE.put(".cs", "C#");
        EXTENSION_TO_LANGUAGE.put(".php", "PHP");
        EXTENSION_TO_LANGUAGE.put(".rb", "Ruby");
        EXTENSION_TO_LANGUAGE.put(".html", "HTML");
        EXTENSION_TO_LANGUAGE.put(".css", "CSS");
        EXTENSION_TO_LANGUAGE.put(".scss", "SCSS");
        EXTENSION_TO_LANGUAGE.put(".md", "Markdown");
        EXTENSION_TO_LANGUAGE.put(".json", "JSON");
        EXTENSION_TO_LANGUAGE.put(".xml", "XML");
        EXTENSION_TO_LANGUAGE.put(".yml", "YAML");
        EXTENSION_TO_LANGUAGE.put(".yaml", "YAML");
        EXTENSION_TO_LANGUAGE.put(".properties", "Properties");
        EXTENSION_TO_LANGUAGE.put(".sql", "SQL");
    }

    public FileFilterService(GitHubConfig gitHubConfig) {
        this.gitHubConfig = gitHubConfig;
    }

    public List<RepositoryFile> scanAndFilterRepository(Path repoRoot, UUID repositoryId) {
        log.info("Scanning repository files in {}", repoRoot);
        List<RepositoryFile> files = new ArrayList<>();
        long maxFileSizeBytes = gitHubConfig.getMaxFileSizeMb() * 1024 * 1024;

        try (Stream<Path> stream = Files.walk(repoRoot)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                Path relativePath = repoRoot.relativize(path);
                String normalizedPath = relativePath.toString().replace('\\', '/');

                if (isIgnoredPath(normalizedPath)) {
                    return;
                }

                String extension = getFileExtension(normalizedPath);
                if (IGNORED_BINARY_EXTENSIONS.contains(extension)) {
                    return;
                }

                if (!EXTENSION_TO_LANGUAGE.containsKey(extension)) {
                    return;
                }

                try {
                    long fileSize = Files.size(path);
                    String language = EXTENSION_TO_LANGUAGE.get(extension);

                    if (fileSize > maxFileSizeBytes) {
                        double sizeMb = (double) fileSize / (1024 * 1024);
                        RepositoryFile oversizedFile = RepositoryFile.builder()
                                .repositoryId(repositoryId)
                                .filePath(normalizedPath)
                                .language(language)
                                .sizeBytes(fileSize)
                                .skipped(true)
                                .skipReason(String.format("File size exceeds limit of %dMB (actual: %.2f MB)", gitHubConfig.getMaxFileSizeMb(), sizeMb))
                                .build();
                        files.add(oversizedFile);
                    } else {
                        RepositoryFile validFile = RepositoryFile.builder()
                                .repositoryId(repositoryId)
                                .filePath(normalizedPath)
                                .language(language)
                                .sizeBytes(fileSize)
                                .skipped(false)
                                .skipReason(null)
                                .build();
                        files.add(validFile);
                    }
                } catch (IOException e) {
                    log.warn("Failed to read file size for {}: {}", path, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.error("Failed to walk repository directory {}: {}", repoRoot, e.getMessage());
            throw new RuntimeException("Failed to scan repository files: " + e.getMessage(), e);
        }

        log.info("Scanned {} files for repository {}", files.size(), repositoryId);
        return files;
    }

    public boolean isIgnoredPath(String relativePath) {
        String[] segments = relativePath.split("/");
        for (String segment : segments) {
            if (IGNORED_DIRECTORIES.contains(segment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public String inferLanguage(String filePath) {
        String ext = getFileExtension(filePath);
        return EXTENSION_TO_LANGUAGE.getOrDefault(ext, "Unknown");
    }

    public boolean isSupportedExtension(String filePath) {
        String ext = getFileExtension(filePath);
        return EXTENSION_TO_LANGUAGE.containsKey(ext);
    }

    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1 || lastDot == path.length() - 1) {
            return "";
        }
        return path.substring(lastDot).toLowerCase(Locale.ROOT);
    }
}
