package ai.haitale.service;

import ai.haitale.model.Mod;
import ai.haitale.service.modrinth.dto.ModrinthProject;
import ai.haitale.service.modrinth.dto.ModrinthVersion;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ModrinthClient {
    private static final Logger LOG = LoggerFactory.getLogger(ModrinthClient.class);
    private static final String DEFAULT_BASE = "https://api.modrinth.com/v2";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String baseUrl;

    public ModrinthClient(ObjectMapper objectMapper, @Value("${modrinth.api.base:}") String configuredBase) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        if (configuredBase == null || configuredBase.isEmpty()) {
            this.baseUrl = DEFAULT_BASE;
        } else {
            this.baseUrl = configuredBase.endsWith("/") ? configuredBase.substring(0, configuredBase.length() - 1) : configuredBase;
        }
    }

    /**
     * Search projects on Modrinth and return mapped Mods. Limit capped by the API call.
     */
    public List<Mod> search(String query, int limit) {
        try {
            String encoded = java.net.URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
            String uri = baseUrl + "/search?query=" + encoded + "&limit=" + Math.max(1, Math.min(limit, 100));
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOG.warn("Modrinth search returned {} for query={}", resp.statusCode(), query);
                return List.of();
            }

            // Modrinth returns an object with 'hits' array
            Map<String, Object> body = objectMapper.readValue(resp.body().getBytes(StandardCharsets.UTF_8), Map.class);
            Object hitsObj = body.get("hits");
            if (!(hitsObj instanceof List)) {
                LOG.warn("Unexpected Modrinth search response format");
                return List.of();
            }

            List<?> hits = (List<?>) hitsObj;
            List<Mod> result = new ArrayList<>();

            for (Object h : hits) {
                try {
                    ModrinthProject proj = objectMapper.readValue(objectMapper.writeValueAsBytes(h), ModrinthProject.class);
                    // For each project, fetch best version info
                    Mod mod = projectToMod(proj, null);
                    if (mod != null) {
                        result.add(mod);
                    }
                } catch (Exception e) {
                    LOG.debug("Failed to map project hit: {}", e.getMessage());
                }
                if (result.size() >= limit) break;
            }

            return result;
        } catch (IOException | InterruptedException e) {
            LOG.warn("Error searching Modrinth: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Map a ModrinthProject to our Mod model by selecting an appropriate version/file.
     * If `preferredGameVersion` is non-null, try to select files matching that game version.
     */
    public Mod projectToMod(ModrinthProject project, String preferredGameVersion) {
        if (project == null) return null;
        String identifier = project.slug != null ? project.slug : project.id;
        if (identifier == null) return null;

        // Try to fetch latest (or specified) version details
        String versionId = project.latest_version;
        if ((versionId == null || versionId.isEmpty()) && project.versions != null && !project.versions.isEmpty()) {
            versionId = project.versions.get(0);
        }

        ModrinthVersion version = null;
        if (versionId != null && !versionId.isEmpty()) {
            try {
                String verUri = baseUrl + "/version/" + java.net.URLEncoder.encode(versionId, StandardCharsets.UTF_8);
                HttpRequest vReq = HttpRequest.newBuilder().uri(URI.create(verUri)).timeout(TIMEOUT).GET().build();
                HttpResponse<String> vResp = httpClient.send(vReq, HttpResponse.BodyHandlers.ofString());
                if (vResp.statusCode() == 200) {
                    version = objectMapper.readValue(vResp.body().getBytes(StandardCharsets.UTF_8), ModrinthVersion.class);
                }
            } catch (Exception e) {
                LOG.debug("Failed to fetch Modrinth version {}: {}", versionId, e.getMessage());
            }
        }

        // If version couldn't be fetched, skip creating a Mod (we want download URL & checksum)
        if (version == null || version.files == null || version.files.isEmpty()) {
            LOG.debug("No version/file available for project {}", identifier);
            return null;
        }

        // Choose best file: prefer those that include preferredGameVersion (if provided), else prefer largest file.
        Optional<ModrinthVersion.ModrinthFile> chosen = version.files.stream()
            .filter(f -> f.url != null && !f.url.isEmpty())
            .sorted(Comparator.comparingLong((ModrinthVersion.ModrinthFile f) -> f.size).reversed())
            .findFirst();

        // If we had a preferredGameVersion we could add extra filtering here by file filename or metadata.

        if (chosen.isEmpty()) {
            LOG.debug("No downloadable file found for {}", identifier);
            return null;
        }

        ModrinthVersion.ModrinthFile file = chosen.get();

        // Choose a checksum algorithm/value: prefer sha256, sha384, sha512, then sha1
        String checksumValue = null;
        String checksumAlg = null;
        if (file.hashes != null && !file.hashes.isEmpty()) {
            if (file.hashes.containsKey("sha256")) {
                checksumAlg = "SHA-256"; checksumValue = file.hashes.get("sha256");
            } else if (file.hashes.containsKey("sha512")) {
                checksumAlg = "SHA-512"; checksumValue = file.hashes.get("sha512");
            } else if (file.hashes.containsKey("sha384")) {
                checksumAlg = "SHA-384"; checksumValue = file.hashes.get("sha384");
            } else if (file.hashes.containsKey("sha1")) {
                checksumAlg = "SHA-1"; checksumValue = file.hashes.get("sha1");
            } else if (!file.hashes.isEmpty()) {
                Map.Entry<String,String> e = file.hashes.entrySet().iterator().next();
                checksumAlg = e.getKey(); checksumValue = e.getValue();
            }
        }

        String author = null;
        if (project.authors != null && !project.authors.isEmpty()) {
            author = project.authors.get(0).username;
        }

        Mod mod = new Mod(
            project.id != null ? project.id : project.slug,
            project.title != null && !project.title.isEmpty() ? project.title : (project.name != null ? project.name : project.slug),
            version.version_number != null ? version.version_number : version.name,
            project.description != null ? project.description : "",
            file.url != null ? file.url : "",
            checksumValue != null ? checksumValue : "",
            checksumAlg != null ? checksumAlg : "",
            "", // license not provided by this call (could call /project/:id for license)
            author != null ? author : "",
            "modrinth",
            file.size
        );

        return mod;
    }
}
