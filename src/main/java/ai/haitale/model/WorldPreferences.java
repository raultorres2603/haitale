package ai.haitale.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class WorldPreferences {
    private String description;
    private String theme; // fantasy, sci-fi, medieval, etc.
    private String difficulty; // easy, normal, hard
    private boolean includeBuilding;
    private boolean includeAdventure;
    private boolean includeTechnology;
    private boolean includeMagic;

    public WorldPreferences() {
    }

    public WorldPreferences(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isIncludeBuilding() {
        return includeBuilding;
    }

    public void setIncludeBuilding(boolean includeBuilding) {
        this.includeBuilding = includeBuilding;
    }

    public boolean isIncludeAdventure() {
        return includeAdventure;
    }

    public void setIncludeAdventure(boolean includeAdventure) {
        this.includeAdventure = includeAdventure;
    }

    public boolean isIncludeTechnology() {
        return includeTechnology;
    }

    public void setIncludeTechnology(boolean includeTechnology) {
        this.includeTechnology = includeTechnology;
    }

    public boolean isIncludeMagic() {
        return includeMagic;
    }

    public void setIncludeMagic(boolean includeMagic) {
        this.includeMagic = includeMagic;
    }
}
