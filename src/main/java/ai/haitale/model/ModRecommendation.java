package ai.haitale.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class ModRecommendation {
    private Mod mod;
    private String reasoning;
    private double relevanceScore; // 0.0 to 1.0

    public ModRecommendation() {
    }

    public ModRecommendation(Mod mod, String reasoning, double relevanceScore) {
        this.mod = mod;
        this.reasoning = reasoning;
        this.relevanceScore = relevanceScore;
    }

    public Mod getMod() {
        return mod;
    }

    public void setMod(Mod mod) {
        this.mod = mod;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    @Override
    public String toString() {
        return String.format("[%.0f%%] %s\n  Reason: %s",
            relevanceScore * 100, mod.getName(), reasoning);
    }
}
