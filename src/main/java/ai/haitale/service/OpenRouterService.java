package ai.haitale.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class OpenRouterService {
    private static final Logger LOG = LoggerFactory.getLogger(OpenRouterService.class);

    private final HttpClient httpClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final String siteUrl;
    private final String siteName;

    // Retry/cache configuration
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final double jitterFraction;

    // Caffeine cache
    private final boolean cacheEnabled;
    private final long cacheTtlMs;
    private final int cacheMaxEntries;
    private final Cache<String, String> caffeineCache;

    // Circuit breaker
    private final boolean circuitEnabled;
    private final int circuitFailureThreshold;
    private final long circuitResetTimeoutMs;
    private final AtomicInteger failureCounter = new AtomicInteger(0);
    private volatile long circuitOpenedAt = 0L; // timestamp when circuit opened

    public OpenRouterService(
        @Client("https://openrouter.ai") HttpClient httpClient,
        @Property(name = "openrouter.api.key") @Nullable String apiKey,
        @Property(name = "openrouter.api.url") String apiUrl,
        @Property(name = "openrouter.model") String model,
        @Property(name = "openrouter.site.url") String siteUrl,
        @Property(name = "openrouter.site.name") String siteName,
        @Property(name = "openrouter.retry.maxAttempts") int maxAttempts,
        @Property(name = "openrouter.retry.initialBackoffMs") long initialBackoffMs,
        @Property(name = "openrouter.retry.maxBackoffMs") long maxBackoffMs,
        @Property(name = "openrouter.retry.jitterFraction") double jitterFraction,
        @Property(name = "openrouter.cache.enabled") boolean cacheEnabled,
        @Property(name = "openrouter.cache.ttlSeconds") long cacheTtlSeconds,
        @Property(name = "openrouter.cache.maxEntries") int cacheMaxEntries,
        @Property(name = "openrouter.circuit.enabled") boolean circuitEnabled,
        @Property(name = "openrouter.circuit.failureThreshold") int circuitFailureThreshold,
        @Property(name = "openrouter.circuit.resetTimeoutSeconds") long circuitResetTimeoutSeconds
    ) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.siteUrl = siteUrl;
        this.siteName = siteName;

        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = Math.max(100L, initialBackoffMs);
        this.maxBackoffMs = Math.max(this.initialBackoffMs, maxBackoffMs);
        this.jitterFraction = Math.max(0.0, Math.min(1.0, jitterFraction));

        this.cacheEnabled = cacheEnabled;
        this.cacheTtlMs = Math.max(0L, cacheTtlSeconds) * 1000L;
        this.cacheMaxEntries = Math.max(1, cacheMaxEntries);

        this.circuitEnabled = circuitEnabled;
        this.circuitFailureThreshold = Math.max(1, circuitFailureThreshold);
        this.circuitResetTimeoutMs = Math.max(1L, circuitResetTimeoutSeconds) * 1000L;

        if (cacheEnabled) {
            this.caffeineCache = Caffeine.newBuilder()
                .expireAfterWrite(this.cacheTtlMs, TimeUnit.MILLISECONDS)
                .maximumSize(this.cacheMaxEntries)
                .build();
        } else {
            this.caffeineCache = null;
        }
    }

    /**
     * Generate mod recommendations using OpenRouter AI
     */
    public String generateModRecommendations(String worldDescription, List<String> availableModsList) {
        if (apiKey == null || apiKey.isEmpty()) {
            LOG.warn("OpenRouter API key not configured. Using fallback recommendation logic.");
            return null;
        }

        // Circuit breaker check
        if (isCircuitOpen()) {
            LOG.warn("OpenRouter circuit is OPEN. Short-circuiting AI call and falling back.");
            return null;
        }

        String cacheKey = buildCacheKey(worldDescription, availableModsList, model);
        if (cacheEnabled) {
            String cached = caffeineCache.getIfPresent(cacheKey);
            if (cached != null) {
                LOG.info("OpenRouter cache hit for key (len {}), returning cached response", cacheKey.length());
                return cached;
            }
        }

        String systemPrompt = buildSystemPrompt(availableModsList);
        String userPrompt = buildUserPrompt(worldDescription);

        OpenRouterRequest request = new OpenRouterRequest(
            model,
            List.of(
                new OpenRouterRequest.Message("system", systemPrompt),
                new OpenRouterRequest.Message("user", userPrompt)
            ),
            0.7,
            2000
        );

        HttpRequest<?> httpRequest = HttpRequest.POST(apiUrl, request)
            .header("Authorization", "Bearer " + apiKey)
            .header("HTTP-Referer", siteUrl)
            .header("X-Title", siteName)
            .header("Content-Type", "application/json");

        LOG.info("Calling OpenRouter API with model: {}", model);

        // Use a robust retry strategy for transient errors like 429
        OpenRouterResponse response = sendWithRetries(httpRequest, maxAttempts);
        if (response == null) {
            recordFailure();
            LOG.warn("OpenRouter request failed after retries; falling back to local recommendations");
            return null;
        }

        // success -> reset failure counter
        recordSuccess();

        if (response.choices != null && !response.choices.isEmpty()) {
            String content = response.choices.get(0).message.content;
            LOG.info("Received AI response ({} tokens)", response.usage != null ? response.usage.totalTokens : "unknown");
            if (cacheEnabled && content != null) caffeineCache.put(cacheKey, content);
            return content;
        }

        LOG.warn("Empty response from OpenRouter API");
        return null;
    }

    private OpenRouterResponse sendWithRetries(HttpRequest<?> request, int maxAttempts) {
        long backoffMillis = initialBackoffMs; // initial backoff
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<OpenRouterResponse> resp = httpClient.toBlocking().exchange(request, OpenRouterResponse.class);
                HttpStatus status = resp.getStatus();

                if (status.getCode() >= 200 && status.getCode() < 300) {
                    return resp.getBody().orElse(null);
                }

                // Handle 429 Too Many Requests specially
                if (status == HttpStatus.TOO_MANY_REQUESTS) {
                    String retryAfter = resp.getHeaders().get("Retry-After");
                    long waitMillis = computeRetryAfterMillis(retryAfter, backoffMillis);
                    LOG.warn("Received 429 Too Many Requests (attempt {}/{}). Retrying after {} ms", attempt, maxAttempts, waitMillis);
                    sleep(waitMillis + jitterMillis(waitMillis));
                    backoffMillis = Math.min(backoffMillis * 2, maxBackoffMs);
                    continue;
                }

                // For server errors (5xx) apply backoff and retry
                if (status.getCode() >= 500 && status.getCode() < 600) {
                    LOG.warn("Server error {} from OpenRouter (attempt {}/{}). Backing off {} ms and retrying", status.getCode(), attempt, maxAttempts, backoffMillis);
                    sleep(backoffMillis + jitterMillis(backoffMillis));
                    backoffMillis = Math.min(backoffMillis * 2, maxBackoffMs);
                    continue;
                }

                // For other non-success statuses do not retry
                LOG.error("OpenRouter returned non-retryable status {}: {}", status.getCode(), resp.getStatus().getReason());
                return null;

            } catch (Exception e) {
                // network/other client error - retry with backoff
                LOG.warn("Error calling OpenRouter API on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    LOG.error("Exhausted retries calling OpenRouter API: {}", e.getMessage(), e);
                    return null;
                }
                sleep(backoffMillis + jitterMillis(backoffMillis));
                backoffMillis = Math.min(backoffMillis * 2, maxBackoffMs);
            }
        }
        return null;
    }

    private long computeRetryAfterMillis(String headerValue, long defaultBackoff) {
        if (headerValue == null) return defaultBackoff;
        headerValue = headerValue.trim();
        try {
            // If it's an integer, it's seconds
            long seconds = Long.parseLong(headerValue);
            return Math.max(500L, seconds * 1000L);
        } catch (NumberFormatException ignore) {
            try {
                // Try parsing an HTTP-date
                ZonedDateTime date = ZonedDateTime.parse(headerValue, DateTimeFormatter.RFC_1123_DATE_TIME);
                long millis = Duration.between(Instant.now(), date.toInstant()).toMillis();
                return Math.max(500L, millis);
            } catch (Exception e) {
                LOG.debug("Unable to parse Retry-After header: {}", headerValue);
                return defaultBackoff;
            }
        }
    }

    private long jitterMillis(long base) {
        // up to jitterFraction of base
        return (long) (Math.random() * (base * jitterFraction));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildCacheKey(String worldDescription, List<String> availableModsList, String model) {
        StringJoiner joiner = new StringJoiner("|", "[", "]");
        joiner.add(model == null ? "" : model);
        joiner.add(worldDescription == null ? "" : worldDescription);
        if (availableModsList != null && !availableModsList.isEmpty()) {
            for (String s : availableModsList) joiner.add(Integer.toHexString(Objects.hashCode(s)));
        }
        return Integer.toHexString(joiner.toString().hashCode());
    }

    private String buildSystemPrompt(List<String> availableModsList) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful assistant that recommends HyTale mods based on user preferences. ");
        prompt.append("Given a description of the world a user wants to create, recommend the most suitable mods from the available list.\n\n");
        prompt.append("Available mods:\n");

        for (String mod : availableModsList) {
            prompt.append("- ").append(mod).append("\n");
        }

        prompt.append("\nRespond in this exact JSON format:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"modId\": \"mod-id-here\",\n");
        prompt.append("    \"relevanceScore\": 0.95,\n");
        prompt.append("    \"reasoning\": \"Brief explanation of why this mod fits\"\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");
        prompt.append("Only recommend mods that actually match the user's description. ");
        prompt.append("Score should be between 0.0 and 1.0 based on relevance. ");
        prompt.append("Return at most 5 recommendations, sorted by relevance.");

        return prompt.toString();
    }

    private String buildUserPrompt(String worldDescription) {
        return "I want to create: " + worldDescription;
    }

    // DTOs for OpenRouter API

    @Serdeable
    public record OpenRouterRequest(
        String model,
        List<Message> messages,
        double temperature,
        int maxTokens
    ) {
        @Serdeable
        public record Message(String role, String content) {}
    }

    @Serdeable
    public record OpenRouterResponse(
        String id,
        List<Choice> choices,
        Usage usage
    ) {
        @Serdeable
        public record Choice(Message message, String finishReason) {}

        @Serdeable
        public record Message(String role, String content) {}

        @Serdeable
        public record Usage(int promptTokens, int completionTokens, int totalTokens) {}
    }

    @Serdeable
    public static class AIRecommendation {
        private String modId;
        private double relevanceScore;
        private String reasoning;

        public AIRecommendation() {}

        public AIRecommendation(String modId, double relevanceScore, String reasoning) {
            this.modId = modId;
            this.relevanceScore = relevanceScore;
            this.reasoning = reasoning;
        }

        public String getModId() { return modId; }
        public void setModId(String modId) { this.modId = modId; }

        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }

        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    }

    // Circuit breaker helpers
    private boolean isCircuitOpen() {
        if (!circuitEnabled) return false;
        int failures = failureCounter.get();
        if (failures < circuitFailureThreshold) return false;
        // if reset timeout passed, allow retry and reset
        if (circuitOpenedAt == 0) {
            circuitOpenedAt = System.currentTimeMillis();
            LOG.warn("Circuit opened at {} after {} failures", circuitOpenedAt, failures);
            return true;
        }
        long elapsed = System.currentTimeMillis() - circuitOpenedAt;
        if (elapsed >= circuitResetTimeoutMs) {
            // reset
            LOG.info("Circuit reset timeout elapsed ({} ms). Resetting circuit.", elapsed);
            failureCounter.set(0);
            circuitOpenedAt = 0L;
            return false;
        }
        return true;
    }

    private void recordFailure() {
        if (!circuitEnabled) return;
        int failures = failureCounter.incrementAndGet();
        LOG.warn("OpenRouter failure count incremented: {} (threshold {})", failures, circuitFailureThreshold);
        if (failures >= circuitFailureThreshold && circuitOpenedAt == 0L) {
            circuitOpenedAt = System.currentTimeMillis();
            LOG.error("Circuit opened due to repeated failures at {}", circuitOpenedAt);
        }
    }

    private void recordSuccess() {
        if (!circuitEnabled) return;
        int prev = failureCounter.getAndSet(0);
        if (prev > 0) {
            LOG.info("OpenRouter success after {} failures, resetting failure counter", prev);
        }
        circuitOpenedAt = 0L;
    }

}
