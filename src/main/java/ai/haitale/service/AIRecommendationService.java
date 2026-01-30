package ai.haitale.service;

import ai.haitale.model.Mod;
import ai.haitale.model.ModRecommendation;
import ai.haitale.model.WorldPreferences;
import io.micronaut.context.annotation.Value;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class AIRecommendationService {
    private static final Logger LOG = LoggerFactory.getLogger(AIRecommendationService.class);

    private final ModRepositoryService modRepositoryService;
    private final OpenRouterService openRouterService;
    private final ObjectMapper objectMapper;

    @Value("${ai.recommendation.prefilter.enabled:true}")
    private boolean preFilterEnabled;

    @Value("${ai.recommendation.prefilter.maxMods:50}")
    private int maxModsToAI;

    @Value("${ai.recommendation.prefilter.threshold:0.15}")
    private double preFilterThreshold;

    @Value("${ai.recommendation.description.maxLength:100}")
    private int maxDescriptionLength;

    public AIRecommendationService(
        ModRepositoryService modRepositoryService,
        OpenRouterService openRouterService,
        ObjectMapper objectMapper
    ) {
        this.modRepositoryService = modRepositoryService;
        this.openRouterService = openRouterService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get AI-powered mod recommendations based on world preferences
     */
    public List<ModRecommendation> getRecommendations(WorldPreferences preferences) {
        LOG.info("Generating recommendations for world description: {}", preferences.getDescription());

        List<Mod> allMods = modRepositoryService.getFreeMods(); // Only free mods

        // Try AI-powered recommendations first
        List<ModRecommendation> aiRecommendations = getAIBasedRecommendations(preferences.getDescription(), allMods);
        if (aiRecommendations != null && !aiRecommendations.isEmpty()) {
            LOG.info("Using AI-powered recommendations ({} mods)", aiRecommendations.size());
            return aiRecommendations;
        }

        // Fallback to rule-based recommendations
        LOG.info("Using fallback rule-based recommendations");
        return getRuleBasedRecommendations(allMods, preferences);
    }

    /**
     * Get AI-based recommendations using OpenRouter
     */
    private List<ModRecommendation> getAIBasedRecommendations(String worldDescription, List<Mod> availableMods) {
        List<Mod> modsToSend = availableMods;

        // PRE-FILTER: Use keyword matching to reduce the list before sending to AI
        // This significantly reduces API costs and improves response time
        if (preFilterEnabled) {
            modsToSend = preFilterModsByKeywords(worldDescription, availableMods);
            LOG.info("Pre-filtered {} mods down to {} candidates for AI evaluation",
                     availableMods.size(), modsToSend.size());
        } else {
            LOG.info("Pre-filtering disabled, sending all {} mods to AI", availableMods.size());
        }

        if (modsToSend.isEmpty()) {
            LOG.warn("Pre-filtering eliminated all mods, falling back to rule-based");
            return null;
        }

        // Prepare mod list for AI - use concise descriptions to reduce token usage
        List<String> modDescriptions = modsToSend.stream()
            .map(mod -> String.format("%s: %s - %s",
                mod.getId(), mod.getName(), truncateDescription(mod.getDescription())))
            .collect(Collectors.toList());

        String aiResponse = openRouterService.generateModRecommendations(worldDescription, modDescriptions);

        if (aiResponse == null || aiResponse.isEmpty()) {
            return null;
        }

        // Parse AI response
        try {
            return parseAIResponse(aiResponse, modsToSend);
        } catch (Exception e) {
            LOG.error("Failed to parse AI response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Pre-filter mods using keyword matching to reduce the list sent to AI.
     * This significantly reduces API costs and response time.
     */
    private List<Mod> preFilterModsByKeywords(String worldDescription, List<Mod> allMods) {
        // If the list is already small, no need to filter
        if (allMods.size() <= 20) {
            return allMods;
        }

        WorldPreferences tempPrefs = new WorldPreferences(worldDescription);

        // Score all mods and keep those above the configured threshold
        List<Mod> filteredMods = allMods.stream()
            .filter(mod -> calculateRelevanceScore(mod, tempPrefs) > preFilterThreshold)
            .sorted((m1, m2) -> Double.compare(
                calculateRelevanceScore(m2, tempPrefs),
                calculateRelevanceScore(m1, tempPrefs)))
            .limit(maxModsToAI)  // Send at most N mods to AI for final ranking
            .collect(Collectors.toList());

        // If filtering was too aggressive, return top 20 mods by generic criteria
        if (filteredMods.size() < 5) {
            LOG.warn("Pre-filtering too aggressive, using top {} mods instead", Math.min(20, allMods.size()));
            return allMods.stream().limit(20).collect(Collectors.toList());
        }

        return filteredMods;
    }

    /**
     * Truncate description to reduce token usage
     */
    private String truncateDescription(String description) {
        if (description == null) return "";
        if (description.length() <= maxDescriptionLength) return description;
        return description.substring(0, maxDescriptionLength) + "...";
    }

    /**
     * Parse AI response into ModRecommendation objects
     */
    private List<ModRecommendation> parseAIResponse(String aiResponse, List<Mod> availableMods) {
        try {
            // Extract JSON array from response (in case there's extra text)
            String jsonArray = extractJsonArray(aiResponse);

            // Parse JSON array
            OpenRouterService.AIRecommendation[] recommendations =
                objectMapper.readValue(jsonArray.getBytes(), OpenRouterService.AIRecommendation[].class);

            List<ModRecommendation> result = new ArrayList<>();

            for (OpenRouterService.AIRecommendation aiRec : recommendations) {
                availableMods.stream()
                    .filter(m -> m.getId().equals(aiRec.getModId()))
                    .findFirst()
                    .ifPresent(mod -> result.add(new ModRecommendation(mod, aiRec.getReasoning(), aiRec.getRelevanceScore())));
             }

             return result;
        } catch (IOException e) {
            LOG.error("Failed to parse AI recommendations", e);
            return null;
        }
    }

    /**
     * Extract JSON array from response text
     */
    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * Rule-based fallback recommendations
     */
    private List<ModRecommendation> getRuleBasedRecommendations(List<Mod> allMods, WorldPreferences preferences) {
        List<ModRecommendation> recommendations = new ArrayList<>();

        for (Mod mod : allMods) {
            double score = calculateRelevanceScore(mod, preferences);
            if (score > 0.3) { // Threshold for relevance
                String reasoning = generateReasoning(mod, preferences);
                recommendations.add(new ModRecommendation(mod, reasoning, score));
            }
        }

        // Sort by relevance score (highest first)
        recommendations.sort(Comparator.comparingDouble(ModRecommendation::getRelevanceScore).reversed());

        LOG.info("Generated {} recommendations", recommendations.size());
        return recommendations;
    }

    /**
     * Calculate relevance score based on preferences
     * Uses keyword matching from the description
     */
    private double calculateRelevanceScore(Mod mod, WorldPreferences preferences) {
        double score = 0.0;
        String description = preferences.getDescription().toLowerCase();
        String modName = mod.getName().toLowerCase();
        String modDesc = mod.getDescription().toLowerCase();

        // Keyword matching
        String[] keywords = description.split("\\s+");
        for (String keyword : keywords) {
            if (keyword.length() < 3) continue;

            if (modName.contains(keyword)) {
                score += 0.2;
            }
            if (modDesc.contains(keyword)) {
                score += 0.1;
            }
        }

        // Category-based matching from description
        if ((description.contains("build") || description.contains("construct") || description.contains("creat")) &&
            (modName.contains("build") || modDesc.contains("build") || modDesc.contains("construction"))) {
            score += 0.3;
        }

        if ((description.contains("adventure") || description.contains("quest") || description.contains("explore")) &&
            (modName.contains("adventure") || modName.contains("quest") ||
             modDesc.contains("adventure") || modDesc.contains("quest") || modDesc.contains("dungeon"))) {
            score += 0.3;
        }

        if ((description.contains("tech") || description.contains("machine") || description.contains("automat")) &&
            (modName.contains("tech") || modName.contains("machine") ||
             modDesc.contains("tech") || modDesc.contains("machine") || modDesc.contains("automation"))) {
            score += 0.3;
        }

        if ((description.contains("magic") || description.contains("spell") || description.contains("wizard")) &&
            (modName.contains("magic") || modName.contains("spell") || modName.contains("mystic") ||
             modDesc.contains("magic") || modDesc.contains("spell") || modDesc.contains("enchant"))) {
            score += 0.3;
        }

        // Theme matching from description
        if ((description.contains("medieval") || description.contains("castle") || description.contains("knight")) &&
            (modName.contains("medieval") || modDesc.contains("medieval") || modDesc.contains("castle"))) {
            score += 0.2;
        }

        if ((description.contains("fantasy") || description.contains("dragon") || description.contains("mythical")) &&
            (modName.contains("fantasy") || modDesc.contains("fantasy") || modDesc.contains("dragon"))) {
            score += 0.2;
        }

        if ((description.contains("sci-fi") || description.contains("futuristic") || description.contains("space")) &&
            (modName.contains("tech") || modDesc.contains("futuristic") || modDesc.contains("space"))) {
            score += 0.2;
        }

        return Math.min(score, 1.0); // Cap at 1.0
    }

    /**
     * Generate reasoning for why a mod was recommended
     */
    private String generateReasoning(Mod mod, WorldPreferences preferences) {
        StringBuilder reasoning = new StringBuilder();
        String description = preferences.getDescription().toLowerCase();
        String modDesc = mod.getDescription().toLowerCase();

        if ((description.contains("build") || description.contains("construct")) && modDesc.contains("build")) {
            reasoning.append("Enhances building capabilities. ");
        }
        if ((description.contains("adventure") || description.contains("quest")) &&
            (modDesc.contains("adventure") || modDesc.contains("quest"))) {
            reasoning.append("Adds adventure and quest content. ");
        }
        if ((description.contains("tech") || description.contains("machine")) &&
            (modDesc.contains("tech") || modDesc.contains("machine"))) {
            reasoning.append("Introduces technological elements. ");
        }
        if ((description.contains("magic") || description.contains("spell")) &&
            (modDesc.contains("magic") || modDesc.contains("spell"))) {
            reasoning.append("Brings magical gameplay. ");
        }

        if (reasoning.isEmpty()) {
            reasoning.append("Matches your world description keywords.");
        }

        return reasoning.toString().trim();
    }

    /**
     * Get recommended mods using AI with natural language description
     */
    public List<ModRecommendation> getAIRecommendations(String worldDescription) {
        LOG.info("Getting AI recommendations for: {}", worldDescription);

        // Create preferences with just the description - let AI handle the interpretation
        WorldPreferences preferences = new WorldPreferences(worldDescription);

        return getRecommendations(preferences);
    }
}
