package ai.haitale.service;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Singleton
public class OpenRouterService {
    private static final Logger LOG = LoggerFactory.getLogger(OpenRouterService.class);

    private final HttpClient httpClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final String siteUrl;
    private final String siteName;

    public OpenRouterService(
        @Client("https://openrouter.ai") HttpClient httpClient,
        @Property(name = "openrouter.api.key") @Nullable String apiKey,
        @Property(name = "openrouter.api.url") String apiUrl,
        @Property(name = "openrouter.model") String model,
        @Property(name = "openrouter.site.url") String siteUrl,
        @Property(name = "openrouter.site.name") String siteName
    ) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.siteUrl = siteUrl;
        this.siteName = siteName;
    }

    /**
     * Generate mod recommendations using OpenRouter AI
     */
    public String generateModRecommendations(String worldDescription, List<String> availableModsList) {
        if (apiKey == null || apiKey.isEmpty()) {
            LOG.warn("OpenRouter API key not configured. Using fallback recommendation logic.");
            return null;
        }

        String systemPrompt = buildSystemPrompt(availableModsList);
        String userPrompt = buildUserPrompt(worldDescription);

        try {
            OpenRouterRequest request = new OpenRouterRequest(
                model,
                List.of(
                    new OpenRouterRequest.Message("system", systemPrompt),
                    new OpenRouterRequest.Message("user", userPrompt)
                ),
                0.7,
                2000
            );

            HttpRequest<?> httpRequest = HttpRequest.POST("/api/v1/chat/completions", request)
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", siteUrl)
                .header("X-Title", siteName)
                .header("Content-Type", "application/json");

            LOG.info("Calling OpenRouter API with model: {}", model);
            OpenRouterResponse response = httpClient.toBlocking().retrieve(httpRequest, OpenRouterResponse.class);

            if (response.choices != null && !response.choices.isEmpty()) {
                String content = response.choices.get(0).message.content;
                LOG.info("Received AI response ({} tokens)", response.usage != null ? response.usage.totalTokens : "unknown");
                return content;
            }

            LOG.warn("Empty response from OpenRouter API");
            return null;

        } catch (Exception e) {
            LOG.error("Error calling OpenRouter API: {}", e.getMessage(), e);
            return null;
        }
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
}
