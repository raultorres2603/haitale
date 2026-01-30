package ai.haitale.service;

import ai.haitale.model.InstallationManifest;
import ai.haitale.model.InstallationManifest.InstalledMod;
import ai.haitale.model.Mod;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

@Singleton
public class ModInstallationService {
    private static final Logger LOG = LoggerFactory.getLogger(ModInstallationService.class);
    private static final String MANIFEST_FILE = "haitale-manifest.json";

    private final ModDownloadService downloadService;
    private final ObjectMapper objectMapper;

    public ModInstallationService(ModDownloadService downloadService, ObjectMapper objectMapper) {
        this.downloadService = downloadService;
        this.objectMapper = objectMapper;
    }

    /**
     * Detect HyTale installation directory
     */
    public Path detectHyTaleDirectory() {
        // Common installation paths
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        Path[] possiblePaths = getPossiblePaths(home, os);

        for (Path path : possiblePaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                LOG.info("Found HyTale directory: {}", path);
                return path;
            }
        }

        // Default fallback
        Path fallback = Path.of(home, ".haitale");
        LOG.warn("HyTale directory not found, using default: {}", fallback);
        return fallback;
    }

    private static Path @NonNull [] getPossiblePaths(String home, String os) {
        Path[] possiblePaths;
        Path path = Path.of(home, "Documents", "HyTale");
        if (os.contains("win")) {
            possiblePaths = new Path[]{
                Path.of(home, "AppData", "Roaming", "HyTale"),
                    path,
                Path.of("C:", "Program Files", "HyTale")
            };
        } else if (os.contains("mac")) {
            possiblePaths = new Path[]{
                Path.of(home, "Library", "Application Support", "HyTale"),
                    path
            };
        } else { // Linux
            possiblePaths = new Path[]{
                Path.of(home, ".hytale"),
                Path.of(home, ".local", "share", "HyTale"),
                Path.of(home, "HyTale")
            };
        }
        return possiblePaths;
    }

    /**
     * Get mods directory
     */
    public Path getModsDirectory() throws IOException {
        Path hyTaleDir = detectHyTaleDirectory();
        Path modsDir = hyTaleDir.resolve("mods");
        Files.createDirectories(modsDir);
        return modsDir;
    }

    /**
     * Install a mod
     */
    public void installMod(Mod mod) throws IOException {
        LOG.info("Installing mod: {} v{}", mod.getName(), mod.getVersion());

        Path modsDir = getModsDirectory();
        Path tempDir = Files.createTempDirectory("haitale-download-");

        try {
            // Download mod
            File downloadedFile = downloadService.downloadMod(mod, tempDir);

            // Backup existing mods directory
            backupModsDirectory(modsDir);

            // Copy to mods directory
            String targetFileName = downloadedFile.getName();
            Path targetPath = modsDir.resolve(targetFileName);

            Files.copy(downloadedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Mod installed to: {}", targetPath);

            // Update manifest
            updateManifest(mod, targetPath);

            LOG.info("Successfully installed mod: {}", mod.getName());

        } finally {
            // Cleanup temp directory
            try {
                Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            LOG.warn("Failed to delete temp file: {}", path);
                        }
                    });
            } catch (IOException e) {
                LOG.warn("Failed to cleanup temp directory", e);
            }
        }
    }

    /**
     * Backup mods directory
     */
    private void backupModsDirectory(Path modsDir) throws IOException {
        if (!Files.exists(modsDir)) {
            return;
        }

        Path backupDir = modsDir.getParent().resolve("mods-backup-" + Instant.now().getEpochSecond());
        LOG.info("Creating backup: {}", backupDir);

        Files.createDirectories(backupDir);

        Files.walk(modsDir)
            .filter(Files::isRegularFile)
            .forEach(source -> {
                try {
                    Path target = backupDir.resolve(modsDir.relativize(source));
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOG.warn("Failed to backup file: {}", source, e);
                }
            });
    }

    /**
     * Update installation manifest
     */
    private void updateManifest(Mod mod, Path installedPath) throws IOException {
        Path modsDir = getModsDirectory();
        Path manifestPath = modsDir.getParent().resolve(MANIFEST_FILE);

        InstallationManifest manifest;
        if (Files.exists(manifestPath)) {
            try (var in = Files.newInputStream(manifestPath)) {
                manifest = objectMapper.readValue(in, InstallationManifest.class);
            }
        } else {
            manifest = new InstallationManifest();
        }

        InstalledMod installedMod = new InstalledMod(
            mod.getId(),
            mod.getName(),
            mod.getVersion(),
            mod.getChecksum(),
            installedPath.toString()
        );

        manifest.getInstalledMods().add(installedMod);
        manifest.setLastUpdated(Instant.now());

        String json = objectMapper.writeValueAsString(manifest);
        Files.writeString(manifestPath, json);
        LOG.info("Updated installation manifest");
    }

    /**
     * List installed mods
     */
    public List<InstalledMod> listInstalledMods() throws IOException {
        Path modsDir = getModsDirectory();
        Path manifestPath = modsDir.getParent().resolve(MANIFEST_FILE);

        if (!Files.exists(manifestPath)) {
            return List.of();
        }

        try (var in = Files.newInputStream(manifestPath)) {
            InstallationManifest manifest = objectMapper.readValue(in, InstallationManifest.class);
            return manifest.getInstalledMods();
        }
    }

    /**
     * Get installation manifest
     */
    public InstallationManifest getManifest() throws IOException {
        Path modsDir = getModsDirectory();
        Path manifestPath = modsDir.getParent().resolve(MANIFEST_FILE);

        if (!Files.exists(manifestPath)) {
            return new InstallationManifest();
        }

        try (var in = Files.newInputStream(manifestPath)) {
            return objectMapper.readValue(in, InstallationManifest.class);
        }
    }
}
