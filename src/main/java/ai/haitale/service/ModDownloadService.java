package ai.haitale.service;

import ai.haitale.model.Mod;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Singleton
public class ModDownloadService {
    private static final Logger LOG = LoggerFactory.getLogger(ModDownloadService.class);
    private static final int BUFFER_SIZE = 8192;
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final HttpClient httpClient;

    public ModDownloadService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Download and verify a mod file
     */
    public File downloadMod(Mod mod, Path downloadDir) throws IOException {
        if (!mod.isFreeLicense()) {
            throw new SecurityException("Mod does not have a free/open-source license: " + mod.getLicense());
        }

        LOG.info("Downloading mod: {} v{}", mod.getName(), mod.getVersion());

        // Create download directory if it doesn't exist
        Files.createDirectories(downloadDir);

        // Generate filename
        String fileName = sanitizeFileName(mod.getName()) + "-" + mod.getVersion() + ".jar";
        Path targetFile = downloadDir.resolve(fileName);

        try {
            // Download the file
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mod.getDownloadUrl()))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to download mod: HTTP " + response.statusCode());
            }

            // Save to file with progress
            try (InputStream in = response.body();
                 FileOutputStream out = new FileOutputStream(targetFile.toFile())) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if (mod.getFileSize() > 0) {
                        int progress = (int) ((totalBytesRead * 100) / mod.getFileSize());
                        if (totalBytesRead % (1024 * 1024) == 0) { // Log every MB
                            LOG.debug("Download progress: {}%", progress);
                        }
                    }
                }
            }

            LOG.info("Download complete: {}", targetFile);

            // Verify checksum
            if (mod.getChecksum() != null && !mod.getChecksum().isEmpty()) {
                if (!verifyChecksum(targetFile.toFile(), mod.getChecksum(), mod.getChecksumAlgorithm())) {
                    Files.deleteIfExists(targetFile);
                    throw new SecurityException("Checksum verification failed! File may be corrupted or tampered with.");
                }
                LOG.info("Checksum verification passed");
            } else {
                LOG.warn("No checksum provided for mod: {}", mod.getName());
            }

            return targetFile.toFile();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    /**
     * Verify file checksum
     */
    public boolean verifyChecksum(File file, String expectedChecksum, String algorithm) {
        try {
            String actualChecksum = calculateChecksum(file, algorithm != null ? algorithm : "SHA-256");
            return actualChecksum.equalsIgnoreCase(expectedChecksum);
        } catch (Exception e) {
            LOG.error("Error verifying checksum", e);
            return false;
        }
    }

    /**
     * Calculate file checksum
     */
    public String calculateChecksum(File file, String algorithm) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Sanitize filename to remove unsafe characters
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
