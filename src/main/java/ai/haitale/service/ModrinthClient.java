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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class ModrinthClient {
    private static final Logger LOG = LoggerFactory.getLogger(ModrinthClient.class);
    private static final String DEFAULT_BASE = "https://api.modrinth.com/v2";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ModrinthClient(ObjectMapper objectMapper, @Value("${modrinth.api.base:}") String configuredBase) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        if (configuredBase == null || configuredBase.isEmpty()) {
            this.baseUrl = DEFAULT_BASE;
        } else {
            this.baseUrl = configuredBase.endsWith("/") ? configuredBase.substring(0, configuredBase.length() - 1) : configuredBase;
        }
    }

    // Public search remains simple: map hits to Mod via projectToMod
    @SuppressWarnings("unchecked")
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

            Map<String, Object> body = objectMapper.readValue(resp.body().getBytes(StandardCharsets.UTF_8), Map.class);
            Object hitsObj = body.get("hits");
            if (!(hitsObj instanceof List<?> hitsList)) {
                LOG.warn("Unexpected Modrinth search response format");
                return List.of();
            }

            List<Mod> result = new ArrayList<>();

            for (Object h : hitsList) {
                try {
                    ModrinthProject proj = objectMapper.readValue(objectMapper.writeValueAsBytes(h), ModrinthProject.class);
                    Mod mod = projectToMod(proj, null);
                    if (mod != null) result.add(mod);
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
     * Top-level mapping from project -> Mod.
     * This method is now small and delegates responsibilities to helpers for clarity.
     */
    public Mod projectToMod(ModrinthProject project, String preferredGameVersion) {
        if (project == null) return null;
        String identifier = project.slug != null ? project.slug : project.id;
        if (identifier == null) return null;

        String versionId = pickVersionId(project);
        if (versionId == null) {
            LOG.debug("No version id for project {}", identifier);
            return null;
        }

        ModrinthVersion version = fetchVersion(versionId);
        if (version == null) return null;

        List<ModrinthVersion.ModrinthFile> files = filterDownloadableFiles(version);
        if (files.isEmpty()) return null;

        List<ModrinthVersion.ModrinthFile> candidateFiles = selectCandidateFiles(files, version, preferredGameVersion);
        ModrinthVersion.ModrinthFile chosen = chooseBestFile(candidateFiles);
        if (chosen == null) return null;

        String[] checksum = extractChecksum(chosen);

        return buildMod(project, version, chosen, checksum[0], checksum[1]);
    }

    // ------------ Helper methods (refactored) ------------

    private String pickVersionId(ModrinthProject project) {
        String versionId = project.latest_version;
        if ((versionId == null || versionId.isEmpty()) && project.versions != null && !project.versions.isEmpty()) {
            // prefer stream().findFirst() to avoid index-based access warnings
            versionId = project.versions.stream().findFirst().orElse(null);
        }
        return (versionId == null || versionId.isEmpty()) ? null : versionId;
    }

    private ModrinthVersion fetchVersion(String versionId) {
        try {
            String verUri = baseUrl + "/version/" + java.net.URLEncoder.encode(versionId, StandardCharsets.UTF_8);
            HttpRequest vReq = HttpRequest.newBuilder().uri(URI.create(verUri)).timeout(TIMEOUT).GET().build();
            HttpResponse<String> vResp = httpClient.send(vReq, HttpResponse.BodyHandlers.ofString());
            if (vResp.statusCode() != 200) {
                LOG.warn("Failed to fetch Modrinth version {}: HTTP {}", versionId, vResp.statusCode());
                return null;
            }

            // Typed DTO deserialization using injected ObjectMapper
            ModrinthVersion version;
            try {
                version = objectMapper.readValue(vResp.body().getBytes(StandardCharsets.UTF_8), ModrinthVersion.class);
            } catch (Exception ex) {
                LOG.warn("Failed to deserialize Modrinth version JSON for {}: {}", versionId, ex.getMessage());
                version = null;
            }

            // If typed deserialization produced no usable files, try a robust map-based fallback
            if (version == null || version.files == null || version.files.isEmpty()) {
                ModrinthVersion parsed = parseVersionFromJson(vResp.body());
                if (parsed != null && parsed.files != null && !parsed.files.isEmpty()) {
                    version = parsed;
                }
            }

            if (version == null || version.files == null || version.files.isEmpty()) return null;
            return version;

        } catch (IOException | InterruptedException e) {
            LOG.warn("Error fetching Modrinth version {}: {}", versionId, e.getMessage());
            return null;
        }
    }

    private ModrinthVersion parseVersionFromJson(String json) {
        try {
            // Use Jackson databind for robust generic parsing into a typed map
            com.fasterxml.jackson.databind.ObjectMapper jackson = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = jackson.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            ModrinthVersion tmp = new ModrinthVersion();
            Object verNum = map.get("version_number");
            tmp.version_number = verNum != null ? String.valueOf(verNum) : null;

            Object gameVersionsObj = map.get("game_versions");
            if (gameVersionsObj instanceof List<?> gvList) {
                tmp.game_versions = gvList.stream().map(Object::toString).collect(Collectors.toList());
            }

            Object filesObj = map.get("files");
            if (filesObj instanceof List<?> filesList) {
                List<ModrinthVersion.ModrinthFile> fList = new ArrayList<>();
                for (Object fo : filesList) {
                    if (!(fo instanceof Map<?, ?> fmap)) continue;
                    ModrinthVersion.ModrinthFile mf = new ModrinthVersion.ModrinthFile();
                    mf.url = fmap.get("url") != null ? String.valueOf(fmap.get("url")) : null;
                    Object sizeObj = fmap.get("size");
                    if (sizeObj instanceof Number) mf.size = ((Number) sizeObj).longValue();
                    Object hashesObj = fmap.get("hashes");
                    if (hashesObj instanceof Map<?, ?> hashMap) {
                        Map<String, String> hm = new HashMap<>();
                        for (Map.Entry<?, ?> e : hashMap.entrySet()) {
                            if (e.getKey() != null && e.getValue() != null) {
                                hm.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                            }
                        }
                        mf.hashes = hm;
                    }
                    mf.filename = fmap.get("filename") != null ? String.valueOf(fmap.get("filename")) : null;
                    fList.add(mf);
                }
                tmp.files = fList;
            }
            return tmp;
        } catch (Exception e) {
            LOG.debug("parseVersionFromJson failed: {}", e.getMessage());
            return null;
        }
    }

    private List<ModrinthVersion.ModrinthFile> filterDownloadableFiles(ModrinthVersion version) {
        return version.files.stream()
            .filter(f -> f.url != null && !f.url.isEmpty())
            .collect(Collectors.toList());
    }

    private List<ModrinthVersion.ModrinthFile> selectCandidateFiles(
        List<ModrinthVersion.ModrinthFile> files,
        ModrinthVersion version,
        String preferredGameVersion
    ) {
        if (preferredGameVersion == null || preferredGameVersion.isEmpty()) return files;

        boolean versionMatches = version.game_versions != null && version.game_versions.stream()
            .anyMatch(gv -> gv.equalsIgnoreCase(preferredGameVersion));
        if (versionMatches) return files;

        List<ModrinthVersion.ModrinthFile> byName = files.stream()
            .filter(f -> f.filename != null && f.filename.toLowerCase().contains(preferredGameVersion.toLowerCase()))
            .collect(Collectors.toList());
        return byName.isEmpty() ? files : byName;
    }

    private ModrinthVersion.ModrinthFile chooseBestFile(List<ModrinthVersion.ModrinthFile> candidateFiles) {
        if (candidateFiles == null || candidateFiles.isEmpty()) return null;

        Comparator<ModrinthVersion.ModrinthFile> bestComparator = Comparator
            .comparingInt((ModrinthVersion.ModrinthFile f) -> {
                if (f.hashes == null) return 0;
                if (f.hashes.containsKey("sha256")) return 3;
                if (f.hashes.containsKey("sha512")) return 3;
                if (f.hashes.containsKey("sha384")) return 2;
                if (f.hashes.containsKey("sha1")) return 1;
                return 0;
            }).thenComparingLong(f -> f.size);

        return candidateFiles.stream().max(bestComparator).orElse(null);
    }

    /**
     * Returns [algorithm, value] (algorithm may be a readable string like "SHA-256").
     */
    private String[] extractChecksum(ModrinthVersion.ModrinthFile file) {
        if (file == null || file.hashes == null || file.hashes.isEmpty()) return new String[] {"", ""};
        if (file.hashes.containsKey("sha256")) return new String[] {"SHA-256", file.hashes.get("sha256")};
        if (file.hashes.containsKey("sha512")) return new String[] {"SHA-512", file.hashes.get("sha512")};
        if (file.hashes.containsKey("sha384")) return new String[] {"SHA-384", file.hashes.get("sha384")};
        if (file.hashes.containsKey("sha1")) return new String[] {"SHA-1", file.hashes.get("sha1")};
        Map.Entry<String, String> e = file.hashes.entrySet().iterator().next();
        return new String[] {e.getKey(), e.getValue()};
    }

    private Mod buildMod(ModrinthProject project, ModrinthVersion version, ModrinthVersion.ModrinthFile file, String checksumAlg, String checksumValue) {
        String author = null;
        if (project.authors != null && !project.authors.isEmpty()) {
            author = project.authors.stream().findFirst().map(a -> a.username).orElse(null);
        }

        return new Mod(
            project.id != null ? project.id : project.slug,
            project.title != null && !project.title.isEmpty() ? project.title : (project.name != null ? project.name : project.slug),
            version.version_number != null ? version.version_number : version.name,
            project.description != null ? project.description : "",
            file.url != null ? file.url : "",
            checksumValue != null ? checksumValue : "",
            checksumAlg != null ? checksumAlg : "",
            "",
            author != null ? author : "",
            "modrinth",
            file.size
        );
    }
}
