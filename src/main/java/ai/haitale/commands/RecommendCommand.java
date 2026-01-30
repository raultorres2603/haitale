package ai.haitale.commands;

import ai.haitale.model.ModRecommendation;
import ai.haitale.service.AIRecommendationService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = "recommend",
         description = "Get AI-powered mod recommendations based on your world description",
         mixinStandardHelpOptions = true)
public class RecommendCommand implements Runnable {

    @Inject
    private AIRecommendationService aiService;

    @Parameters(index = "0..*",
                description = "Describe the world you want to create (e.g., 'medieval fantasy with magic and dragons')")
    private String[] descriptionWords;

    @Override
    public void run() {
        if (descriptionWords == null || descriptionWords.length == 0) {
            System.err.println("Error: Please provide a world description");
            System.err.println("Example: haitale recommend I want a medieval world with magic and building tools");
            return;
        }

        String worldDescription = String.join(" ", descriptionWords);

        System.out.println("Analyzing your world description...");
        System.out.println("Description: " + worldDescription);
        System.out.println();

        List<ModRecommendation> recommendations = aiService.getAIRecommendations(worldDescription);

        if (recommendations.isEmpty()) {
            System.out.println("No mods found matching your description.");
            System.out.println("Try being more specific or use different keywords.");
            return;
        }

        System.out.println("Found " + recommendations.size() + " recommended mods:");
        System.out.println("================================================");
        System.out.println();

        int count = 1;
        for (ModRecommendation rec : recommendations) {
            System.out.printf("%d. %s v%s [Score: %.0f%%]%n",
                count++,
                rec.getMod().getName(),
                rec.getMod().getVersion(),
                rec.getRelevanceScore() * 100);
            System.out.println("   Author: " + rec.getMod().getAuthor());
            System.out.println("   License: " + rec.getMod().getLicense());
            System.out.println("   Description: " + rec.getMod().getDescription());
            System.out.println("   Why recommended: " + rec.getReasoning());
            System.out.println("   ID: " + rec.getMod().getId());
            System.out.println();
        }

        System.out.println("To install mods, use: haitale install <mod-id-1> <mod-id-2> ...");
    }
}
