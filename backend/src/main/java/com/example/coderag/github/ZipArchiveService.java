package com.example.coderag.github;

import com.example.coderag.exception.GitHubApiException;
import com.example.coderag.exception.RepoTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ZipArchiveService.class);

    private final GitHubConfig gitHubConfig;

    public ZipArchiveService(GitHubConfig gitHubConfig) {
        this.gitHubConfig = gitHubConfig;
    }

    public Path downloadAndExtractRepository(String owner, String name, String commitSha, UUID repositoryId) {
        long maxSizeBytes = gitHubConfig.getMaxRepoSizeMb() * 1024 * 1024;
        Path targetDir = Paths.get(gitHubConfig.getRepoBasePath(), repositoryId.toString()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory for repository: " + targetDir, e);
        }

        Path tempZipFile = null;
        try {
            tempZipFile = Files.createTempFile("repo-" + repositoryId, ".zip");
            String downloadUrl = String.format("https://github.com/%s/%s/archive/%s.zip", owner, name, commitSha);
            log.info("Downloading repository archive from: {}", downloadUrl);

            downloadZipWithLimit(downloadUrl, tempZipFile, maxSizeBytes);
            log.info("Repository zip downloaded successfully (size: {} bytes). Extracting...", Files.size(tempZipFile));

            extractZipSafely(tempZipFile, targetDir);
            log.info("Repository extracted successfully to {}", targetDir);

            return targetDir;

        } catch (RepoTooLargeException e) {
            cleanUpDirectory(targetDir);
            throw e;
        } catch (Exception e) {
            cleanUpDirectory(targetDir);
            log.error("Failed to download or extract repository: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download or extract repository: " + e.getMessage(), e);
        } finally {
            if (tempZipFile != null) {
                try {
                    Files.deleteIfExists(tempZipFile);
                } catch (IOException ignored) {}
            }
        }
    }

    private void downloadZipWithLimit(String downloadUrl, Path destination, long maxSizeBytes) throws IOException {
        URL url = URI.create(downloadUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "GitHub-CodeRAG-Application");
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        if (StringUtils.hasText(gitHubConfig.getToken())) {
            connection.setRequestProperty("Authorization", "Bearer " + gitHubConfig.getToken().trim());
        }

        int responseCode = connection.getResponseCode();
        // Follow redirects manually if needed (GitHub redirects /archive/ to codeload)
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 308) {
            String redirectUrl = connection.getHeaderField("Location");
            if (redirectUrl != null) {
                connection.disconnect();
                downloadZipWithLimit(redirectUrl, destination, maxSizeBytes);
                return;
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new GitHubApiException("Failed to download repository archive from GitHub (HTTP " + responseCode + ")", responseCode);
        }

        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream out = new FileOutputStream(destination.toFile())) {

            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxSizeBytes) {
                    throw new RepoTooLargeException(
                            String.format("Downloaded repository archive exceeds the %dMB limit (downloaded > %.2f MB)",
                                    gitHubConfig.getMaxRepoSizeMb(), (double) totalBytesRead / (1024 * 1024))
                    );
                }
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void extractZipSafely(Path zipFilePath, Path targetDir) throws IOException {
        String rootPrefix = null;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().replace('\\', '/');

                // Determine root folder prefix added by GitHub (e.g. repo-name-sha/)
                if (rootPrefix == null) {
                    int slashIdx = entryName.indexOf('/');
                    if (slashIdx != -1) {
                        rootPrefix = entryName.substring(0, slashIdx + 1);
                    }
                }

                String relativePath = entryName;
                if (rootPrefix != null && entryName.startsWith(rootPrefix)) {
                    relativePath = entryName.substring(rootPrefix.length());
                }

                if (!StringUtils.hasText(relativePath)) {
                    zis.closeEntry();
                    continue;
                }

                Path resolvedPath = targetDir.resolve(relativePath).normalize();

                // Zip Slip protection
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new SecurityException("Zip Slip vulnerability detected: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    if (resolvedPath.getParent() != null) {
                        Files.createDirectories(resolvedPath.getParent());
                    }
                    try (FileOutputStream fos = new FileOutputStream(resolvedPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public void cleanUpDirectory(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try {
                FileSystemUtils.deleteRecursively(dir);
            } catch (IOException e) {
                log.warn("Failed to clean up directory {}: {}", dir, e.getMessage());
            }
        }
    }
}
