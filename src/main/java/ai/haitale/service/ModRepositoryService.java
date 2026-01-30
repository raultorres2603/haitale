package ai.haitale.service;

import ai.haitale.model.Mod;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ModRepositoryService {
    private static final Logger LOG = LoggerFactory.getLogger(ModRepositoryService.class);

    private final List<Mod> modCache = new ArrayList<>();

    public ModRepositoryService() {
        // Initialize with some sample mods for demonstration
        // In production, this would fetch from real APIs
        initializeSampleMods();
    }

    private void initializeSampleMods() {
        // Sample mods for demonstration
        modCache.add(new Mod(
            "enhanced-building-1",
            "Enhanced Building Tools",
            "1.0.0",
            "Adds advanced building tools, templates, and blueprints for complex structures",
            "https://example.com/mods/enhanced-building-1.0.0.jar",
            "abc123def456",
            "SHA-256",
            "MIT",
            "BuilderPro",
            "modrinth",
            2048000
        ));

        modCache.add(new Mod(
            "magic-realms-2",
            "Magic Realms",
            "2.1.0",
            "Introduces magical spells, enchantments, and mystical creatures to your world",
            "https://example.com/mods/magic-realms-2.1.0.jar",
            "def456ghi789",
            "SHA-256",
            "Apache-2.0",
            "MysticCoder",
            "curseforge",
            3145728
        ));

        modCache.add(new Mod(
            "tech-revolution-3",
            "Tech Revolution",
            "1.5.2",
            "Adds machinery, automation, and technological advancement systems",
            "https://example.com/mods/tech-revolution-1.5.2.jar",
            "ghi789jkl012",
            "SHA-256",
            "GPL-3.0",
            "TechWizard",
            "modrinth",
            4194304
        ));

        modCache.add(new Mod(
            "adventure-quests-4",
            "Adventure Quest Pack",
            "3.0.0",
            "Hundreds of quests, dungeons, and adventures with dynamic storytelling",
            "https://example.com/mods/adventure-quests-3.0.0.jar",
            "jkl012mno345",
            "SHA-256",
            "MIT",
            "QuestMaster",
            "curseforge",
            5242880
        ));

        modCache.add(new Mod(
            "medieval-pack-5",
            "Medieval Immersion",
            "1.2.3",
            "Medieval castles, knights, siege weapons, and historical immersion",
            "https://example.com/mods/medieval-pack-1.2.3.jar",
            "mno345pqr678",
            "SHA-256",
            "LGPL-2.1",
            "HistoryBuff",
            "github",
            2621440
        ));

        modCache.add(new Mod(
            "fantasy-creatures-6",
            "Fantasy Creatures Expansion",
            "2.0.1",
            "Dragons, griffins, unicorns and other mythical creatures",
            "https://example.com/mods/fantasy-creatures-2.0.1.jar",
            "pqr678stu901",
            "SHA-256",
            "BSD-3-Clause",
            "CreatureDesigner",
            "modrinth",
            3670016
        ));

        LOG.info("Initialized mod cache with {} mods", modCache.size());
    }

    /**
     * Search mods by keyword
     */
    public List<Mod> searchMods(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return modCache.stream()
            .filter(mod -> mod.getName().toLowerCase().contains(lowerKeyword) ||
                          mod.getDescription().toLowerCase().contains(lowerKeyword))
            .collect(Collectors.toList());
    }

    /**
     * Get all available mods
     */
    public List<Mod> getAllMods() {
        return new ArrayList<>(modCache);
    }

    /**
     * Get mods by category/tags
     */
    public List<Mod> getModsByCategory(String category) {
        String lowerCategory = category.toLowerCase();
        return modCache.stream()
            .filter(mod -> mod.getDescription().toLowerCase().contains(lowerCategory) ||
                          mod.getName().toLowerCase().contains(lowerCategory))
            .collect(Collectors.toList());
    }

    /**
     * Get mod by ID
     */
    public Mod getModById(String id) {
        return modCache.stream()
            .filter(mod -> mod.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Filter mods by free license only
     */
    public List<Mod> getFreeMods() {
        return modCache.stream()
            .filter(Mod::isFreeLicense)
            .collect(Collectors.toList());
    }

    /**
     * Refresh mod cache from remote repositories
     * In production, this would call actual APIs
     */
    public void refreshModCache() {
        LOG.info("Refreshing mod cache from repositories...");
        // TODO: Implement API calls to Modrinth, CurseForge, etc.
        LOG.info("Mod cache refresh complete. Total mods: {}", modCache.size());
    }
}
