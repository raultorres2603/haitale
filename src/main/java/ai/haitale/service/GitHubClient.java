package ai.haitale.service;

import ai.haitale.model.Mod;
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
import java.util.List;
import java.util.Map;

@Singleton
public class GitHubClient {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubClient.class);
    private static final String API_BASE = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;

    public GitHubClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    /**
     * Fetch latest release for a repo (owner/repo) and map release assets to Mod objects.
     * This method expects `repo` in the form "owner/repo".
     */
    public List<Mod> fetchLatestRelease(String repo) {
        if (repo == null || !repo.contains("/")) return List.of();
        try {
            String uri = API_BASE + "/repos/" + java.net.URLEncoder.encode(repo, StandardCharsets.UTF_8) + "/releases/latest";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(uri)).timeout(TIMEOUT).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOG.warn("GitHub releases returned {} for repo={}", resp.statusCode(), repo);
                return List.of();
            }

            com.fasterxml.jackson.databind.ObjectMapper jackson = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> json = jackson.readValue(resp.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            String tagName = asString(json.get("tag_name"));
            String name = asString(json.get("name"));
            String body = asString(json.get("body"));
            String author = null;
            Object authorObj = json.get("author");
            if (authorObj instanceof Map<?,?> authorMap) author = asString(authorMap.get("login"));

            List<Mod> result = new ArrayList<>();
            Object assetsObj = json.get("assets");
            if (assetsObj instanceof List<?> assets) {
                for (Object a : assets) {
                    if (!(a instanceof Map<?,?> asset)) continue;
                    String url = asString(asset.get("browser_download_url"));
                    String filename = asString(asset.get("name"));
                    long size = 0L;
                    Object sizeObj = asset.get("size");
                    if (sizeObj instanceof Number) size = ((Number) sizeObj).longValue();

                    Mod mod = new Mod(
                        repo + ":" + filename,
                        name != null ? name : filename,
                        tagName != null ? tagName : "",
                        body != null ? body : "",
                        url != null ? url : "",
                        "", // no checksum
                        "",
                        "", // license unknown
                        author != null ? author : "",
                        "github",
                        size
                    );
                    result.add(mod);
                }
            }

            return result;
        } catch (IOException | InterruptedException e) {
            LOG.warn("Error fetching GitHub release for {}: {}", repo, e.getMessage());
            return List.of();
        }
    }

    private String asString(Object o) { return o == null ? null : String.valueOf(o); }
}
