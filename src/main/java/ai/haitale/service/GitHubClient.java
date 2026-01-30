package ai.haitale.service;

import ai.haitale.model.Mod;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class GitHubClient {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubClient.class);
    private static final String API_BASE = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${github.api.token:}")
    private String githubToken;

    public GitHubClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search GitHub repositories for Hytale mods
     */
    public List<Mod> search(String query, int limit) {
        try {
            // Search for repositories with "hytale" and the query term
            String searchTerm = "hytale";
            if (query != null && !query.isEmpty()) {
                searchTerm += " " + query;
            }
            searchTerm += " in:name,description,readme";

            String encodedQuery = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
            String uri = API_BASE + "/search/repositories?q=" + encodedQuery + "&sort=stars&order=desc&per_page=" + Math.min(limit, 30);

            LOG.info("Searching GitHub for Hytale mods: query='{}', limit={}", query, limit);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .GET();

            // Add GitHub token if available (increases rate limits)
            if (githubToken != null && !githubToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + githubToken);
                LOG.debug("Using GitHub token for authentication");
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.error("GitHub API returned HTTP {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            Map<String, Object> responseData = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            Object itemsObj = responseData.get("items");

            if (!(itemsObj instanceof List<?>)) {
                LOG.warn("No items found in GitHub search response");
                return List.of();
            }

            List<?> items = (List<?>) itemsObj;
            List<Mod> mods = new ArrayList<>();

            for (Object item : items) {
                if (!(item instanceof Map<?, ?>)) continue;
                Map<?, ?> repo = (Map<?, ?>) item;

                String fullName = asString(repo.get("full_name"));
                if (fullName == null) continue;

                // Fetch the latest release for this repository
                List<Mod> repoMods = fetchLatestRelease(fullName);
                mods.addAll(repoMods);

                if (mods.size() >= limit) break;
            }

            LOG.info("Found {} Hytale mods on GitHub", mods.size());
            return mods.stream().limit(limit).collect(Collectors.toList());

        } catch (IOException | InterruptedException e) {
            LOG.error("Error searching GitHub: {}", e.getMessage(), e);
            return List.of();
        }
    }


    /**
     * Fetch latest release for a repo (owner/repo) and map release assets to Mod objects.
     */
    public List<Mod> fetchLatestRelease(String repo) {
        if (repo == null || !repo.contains("/")) return List.of();

        try {
            String uri = API_BASE + "/repos/" + repo + "/releases/latest";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .GET();

            if (githubToken != null && !githubToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + githubToken);
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.debug("No releases found for repo={} (HTTP {})", repo, response.statusCode());
                return List.of();
            }

            Map<String, Object> json = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            String tagName = asString(json.get("tag_name"));
            String name = asString(json.get("name"));
            String body = asString(json.get("body"));

            String author = null;
            Object authorObj = json.get("author");
            if (authorObj instanceof Map<?,?> authorMap) {
                author = asString(authorMap.get("login"));
            }

            List<Mod> result = new ArrayList<>();
            Object assetsObj = json.get("assets");

            if (assetsObj instanceof List<?> assets) {
                for (Object a : assets) {
                    if (!(a instanceof Map<?,?> asset)) continue;

                    String url = asString(asset.get("browser_download_url"));
                    String filename = asString(asset.get("name"));

                    // Only include .jar files
                    if (filename == null || !filename.endsWith(".jar")) continue;

                    long size = 0L;
                    Object sizeObj = asset.get("size");
                    if (sizeObj instanceof Number) {
                        size = ((Number) sizeObj).longValue();
                    }

                    Mod mod = new Mod(
                        repo.replace("/", "-") + "-" + tagName,
                        name != null ? name : filename.replace(".jar", ""),
                        tagName != null ? tagName : "latest",
                        body != null ? body.substring(0, Math.min(body.length(), 200)) : "",
                        url != null ? url : "",
                        "", // GitHub doesn't provide checksums in release API
                        "",
                        "MIT", // Assume MIT for GitHub projects (most common for mods)
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

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
