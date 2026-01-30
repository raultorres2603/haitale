package ai.haitale.service;

import ai.haitale.model.Mod;
import ai.haitale.service.curseforge.dto.CurseForgeProject;
import ai.haitale.service.curseforge.dto.CurseForgeResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Singleton
public class CurseForgeClient {
    private static final Logger LOG = LoggerFactory.getLogger(CurseForgeClient.class);
    private static final String API_BASE = "https://api.curseforge.com/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    // Hytale Game ID on CurseForge
    private static final int HYTALE_GAME_ID = 61428;

    // License mapping for common CurseForge licenses
    private static final String DEFAULT_LICENSE = "All Rights Reserved";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${curseforge.api.key:}")
    private String apiKey;

    public CurseForgeClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search for Hytale mods on CurseForge
     */
    public List<Mod> search(String query, int limit) {
        LOG.debug("CurseForge search called - API key present: {}", apiKey != null && !apiKey.isBlank());

        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("No CurseForge API key configured. Set curseforge.api.key in application.properties or CURSEFORGE_API_KEY environment variable");
            return List.of();
        }

        try {
            LOG.info("Searching CurseForge for Hytale mods: query='{}', limit={}", query, limit);
            LOG.debug("Using API key: {}...", apiKey != null && apiKey.length() > 4 ? apiKey.substring(0, 4) : "invalid");

            String searchTerm = query == null || query.isEmpty() ? "" : URLEncoder.encode(query, StandardCharsets.UTF_8);
            String uri = API_BASE + "/mods/search?gameId=" + HYTALE_GAME_ID
                + "&searchFilter=" + searchTerm
                + "&pageSize=" + Math.max(1, Math.min(limit, 50))
                + "&sortField=2&sortOrder=desc"; // Sort by popularity

            LOG.debug("CurseForge API URL: {}", uri);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(TIMEOUT)
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.error("CurseForge API returned HTTP {}: {}", response.statusCode(), response.body());
                LOG.debug("Response headers: {}", response.headers().map());
                return List.of();
            }

            CurseForgeResponse<List<CurseForgeProject>> cfResponse = objectMapper.readValue(
                response.body(),
                new TypeReference<CurseForgeResponse<List<CurseForgeProject>>>() {}
            );

            if (cfResponse.data == null || cfResponse.data.isEmpty()) {
                LOG.info("No Hytale mods found on CurseForge for query: {}", query);
                return List.of();
            }

            List<Mod> result = new ArrayList<>();
            for (CurseForgeProject project : cfResponse.data) {
                Mod mod = projectToMod(project);
                if (mod != null) {
                    result.add(mod);
                }
                if (result.size() >= limit) break;
            }

            LOG.info("Found {} Hytale mods on CurseForge", result.size());
            return result;

        } catch (IOException | InterruptedException e) {
            LOG.error("Error searching CurseForge: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get detailed mod information by ID
     */
    public Mod getModById(int modId) {
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("No CurseForge API key configured");
            return null;
        }

        try {
            String uri = API_BASE + "/mods/" + modId;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(TIMEOUT)
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.error("CurseForge API returned HTTP {} for mod {}", response.statusCode(), modId);
                return null;
            }

            CurseForgeResponse<CurseForgeProject> cfResponse = objectMapper.readValue(
                response.body(),
                new TypeReference<CurseForgeResponse<CurseForgeProject>>() {}
            );

            return projectToMod(cfResponse.data);

        } catch (IOException | InterruptedException e) {
            LOG.error("Error fetching mod {} from CurseForge: {}", modId, e.getMessage());
            return null;
        }
    }

    /**
     * Convert CurseForge project to our Mod model
     */
    private Mod projectToMod(CurseForgeProject project) {
        if (project == null) return null;

        // Get the latest file
        CurseForgeProject.CurseForgeFile latestFile = getLatestFile(project);
        if (latestFile == null) {
            LOG.debug("No downloadable file found for project: {}", project.name);
            return null;
        }

        // Extract author
        String author = "";
        if (project.authors != null && !project.authors.isEmpty()) {
            author = project.authors.get(0).name;
        }

        // Extract checksum
        String checksum = "";
        String checksumAlgo = "SHA-1";
        if (latestFile.hashes != null && !latestFile.hashes.isEmpty()) {
            CurseForgeProject.CurseForgeFile.CurseForgeHash hash = latestFile.hashes.stream()
                .filter(h -> h.algo == 1) // Prefer SHA-1
                .findFirst()
                .orElse(latestFile.hashes.get(0));
            checksum = hash.value;
            checksumAlgo = hash.algo == 1 ? "SHA-1" : "MD5";
        }

        // Determine license - CurseForge doesn't always provide this in API
        String license = determineLicense(project);

        return new Mod(
            String.valueOf(project.id),
            project.name,
            latestFile.displayName != null ? latestFile.displayName : "latest",
            project.summary != null ? project.summary : "",
            latestFile.downloadUrl != null ? latestFile.downloadUrl : "",
            checksum,
            checksumAlgo,
            license,
            author,
            "curseforge",
            latestFile.fileLength
        );
    }

    /**
     * Get the latest/best file from a project
     */
    private CurseForgeProject.CurseForgeFile getLatestFile(CurseForgeProject project) {
        if (project.latestFiles == null || project.latestFiles.isEmpty()) {
            return null;
        }

        // Filter for files with download URLs
        List<CurseForgeProject.CurseForgeFile> downloadableFiles = project.latestFiles.stream()
            .filter(f -> f.downloadUrl != null && !f.downloadUrl.isEmpty())
            .toList();

        if (downloadableFiles.isEmpty()) {
            return null;
        }

        // Return the file with the largest fileLength (usually the most complete)
        return downloadableFiles.stream()
            .max(Comparator.comparingLong(f -> f.fileLength))
            .orElse(null);
    }

    /**
     * Determine license for a project
     * CurseForge API doesn't always include license info, so we need to make assumptions
     */
    private String determineLicense(CurseForgeProject project) {
        // Check if there's a source link (usually indicates open source)
        if (project.links != null && project.links.sourceUrl != null && !project.links.sourceUrl.isEmpty()) {
            // If they have source code, assume it's at least somewhat open
            return "MIT"; // Default to MIT for open source projects
        }

        // For CurseForge mods without clear license info, they typically follow CurseForge ToS
        // which allows redistribution for modding purposes
        return "CurseForge-Standard";
    }
}
