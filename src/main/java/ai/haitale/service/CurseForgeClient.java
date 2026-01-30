package ai.haitale.service;

import ai.haitale.model.Mod;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.context.annotation.Property;
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
public class CurseForgeClient {
    private static final Logger LOG = LoggerFactory.getLogger(CurseForgeClient.class);
    private static final String API_BASE = "https://api.curseforge.com/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public CurseForgeClient(ObjectMapper objectMapper, @Property(name = "mod.repository.curseforge.api-key") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    /**
     * Very small wrapper to search projects on CurseForge. The official API requires an API key for most endpoints.
     */
    public List<Mod> search(String query, int limit) {
        LOG.info("CurseForge search called (query={}), but API key support is minimal in this implementation", query);
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("No CurseForge API key configured; skipping CurseForge search");
            return List.of();
        }

        try {
            String encoded = java.net.URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
            String uri = API_BASE + "/mods/search?gameId=605&search=" + encoded + "&pageSize=" + Math.max(1, Math.min(limit, 100));
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(uri)).timeout(TIMEOUT).GET();
            reqBuilder.header("x-api-key", apiKey);
            HttpRequest req = reqBuilder.build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOG.warn("CurseForge search returned {} for query={}", resp.statusCode(), query);
                return List.of();
            }

            // Use Jackson for generic parsing to avoid unchecked casts
            com.fasterxml.jackson.databind.ObjectMapper jackson = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> json = jackson.readValue(resp.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            Object dataObj = json.get("data");
            if (!(dataObj instanceof List<?> dataList)) return List.of();

            List<Mod> result = new ArrayList<>();
            for (Object o : dataList) {
                if (!(o instanceof Map<?,?> entryMap)) continue;
                Map<?,?> entry = entryMap;
                String id = asString(entry.get("id"));
                String name = asString(entry.get("name"));
                String summary = asString(entry.get("summary"));
                String author = null;
                Object authorObj = entry.get("author");
                if (authorObj instanceof Map<?,?> authorMap) author = asString(authorMap.get("name"));

                Mod mod = new Mod(
                    id != null ? id : java.util.UUID.randomUUID().toString(),
                    name != null ? name : "",
                    "", // version unknown
                    summary != null ? summary : "",
                    "", // download url requires extra step
                    "",
                    "",
                    "",
                    author != null ? author : "",
                    "curseforge",
                    0L
                );

                result.add(mod);
                if (result.size() >= limit) break;
            }

            return result;
        } catch (IOException | InterruptedException e) {
            LOG.warn("Error searching CurseForge: {}", e.getMessage());
            return List.of();
        }
    }

    private String asString(Object o) { return o == null ? null : String.valueOf(o); }
}
