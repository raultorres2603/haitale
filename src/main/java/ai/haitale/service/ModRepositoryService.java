package ai.haitale.service;

import ai.haitale.model.Mod;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ModRepositoryService {
    private static final Logger LOG = LoggerFactory.getLogger(ModRepositoryService.class);

    private final List<Mod> modCache = new ArrayList<>();

    private final CurseForgeClient curseForgeClient;
    private final GitHubClient gitHubClient;

    @Value("${mod.repository.modrinth.enabled}")
    private boolean modrinthEnabled;

    @Value("${mod.repository.curseforge.enabled}")
    private boolean curseforgeEnabled;

    @Value("${mod.repository.github.enabled}")
    private boolean githubEnabled;

    @Value("${mod.repository.github.example-repos:}")
    private String githubExampleReposCsv;

    public ModRepositoryService(
        CurseForgeClient curseForgeClient,
        GitHubClient gitHubClient
    ) {
        this.curseForgeClient = curseForgeClient;
        this.gitHubClient = gitHubClient;
    }

    @PostConstruct
    public void initialize() {
        // Initialize with some sample mods for demonstration (kept as fallback)
        initializeSampleMods();

        // Attempt to refresh cache from remote repositories on startup
        refreshModCache();
    }

    private void initializeSampleMods() {
        // Sample mods for demonstration - these use placeholder URLs and should only be used
        // when real repository integration is not available
        modCache.add(new Mod(
            "enhanced-building-1",
            "Enhanced Building Tools",
            "1.0.0",
            "Adds advanced building tools, templates, and blueprints for complex structures",
            "", // Empty URL indicates this is a sample mod
            "abc123def456",
            "SHA-256",
            "MIT",
            "BuilderPro",
            "sample",
            2048000
        ));

        modCache.add(new Mod(
            "magic-realms-2",
            "Magic Realms",
            "2.1.0",
            "Introduces magical spells, enchantments, and mystical creatures to your world",
            "", // Empty URL indicates this is a sample mod
            "def456ghi789",
            "SHA-256",
            "Apache-2.0",
            "MysticCoder",
            "sample",
            3145728
        ));

        modCache.add(new Mod(
            "tech-revolution-3",
            "Tech Revolution",
            "1.5.2",
            "Adds machinery, automation, and technological advancement systems",
            "", // Empty URL indicates this is a sample mod
            "ghi789jkl012",
            "SHA-256",
            "GPL-3.0",
            "TechWizard",
            "sample",
            4194304
        ));

        modCache.add(new Mod(
            "adventure-quests-4",
            "Adventure Quest Pack",
            "3.0.0",
            "Hundreds of quests, dungeons, and adventures with dynamic storytelling",
            "", // Empty URL indicates this is a sample mod
            "jkl012mno345",
            "SHA-256",
            "MIT",
            "QuestMaster",
            "sample",
            5242880
        ));

        modCache.add(new Mod(
            "medieval-pack-5",
            "Medieval Immersion",
            "1.2.3",
            "Medieval castles, knights, siege weapons, and historical immersion",
            "", // Empty URL indicates this is a sample mod
            "mno345pqr678",
            "SHA-256",
            "LGPL-2.1",
            "HistoryBuff",
            "sample",
            2621440
        ));

        modCache.add(new Mod(
            "fantasy-creatures-6",
            "Fantasy Creatures Expansion",
            "2.0.1",
            "Dragons, griffins, unicorns and other mythical creatures",
            "", // Empty URL indicates this is a sample mod
            "pqr678stu901",
            "SHA-256",
            "BSD-3-Clause",
            "CreatureDesigner",
            "sample",
            3670016
        ));

        LOG.info("Initialized mod cache with {} sample mods", modCache.size());
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
     */
    public void refreshModCache() {
        LOG.info("Refreshing mod cache from repositories...");
        LOG.info("Repository status - Modrinth: {}, CurseForge: {}, GitHub: {}",
            modrinthEnabled, curseforgeEnabled, githubEnabled);

        Set<String> seen = new HashSet<>();
        // Keep existing sample mods as fallback
        List<Mod> newCache = new ArrayList<>();

        // Try CurseForge
        LOG.debug("Checking CurseForge: enabled={}, client={}", curseforgeEnabled, curseForgeClient != null);
        if (curseforgeEnabled && curseForgeClient != null) {
            try {
                LOG.info("Fetching mods from CurseForge...");
                List<Mod> fromCurse = curseForgeClient.search("", 50);
                for (Mod m : fromCurse) {
                    if (m.getId() == null) continue;
                    if (seen.add(m.getId())) newCache.add(m);
                }
                LOG.info("Imported {} mods from CurseForge", fromCurse.size());
            } catch (Exception e) {
                LOG.warn("Failed to fetch from CurseForge: {}", e.getMessage(), e);
            }
        } else {
            LOG.warn("CurseForge integration disabled or client not available (enabled={}, client={})",
                curseforgeEnabled, curseForgeClient != null);
        }

        // Try GitHub - this is best-effort and requires repository identifiers to be known
        if (githubEnabled && gitHubClient != null) {
            try {
                LOG.info("Fetching some releases from GitHub (example repos)...");
                // Example repos can be configured via application properties
                List<String> exampleRepos = githubExampleReposCsv == null || githubExampleReposCsv.isBlank()
                    ? List.of()
                    : List.of(githubExampleReposCsv.split(","));
                for (String repo : exampleRepos) {
                    List<Mod> fromGit = gitHubClient.fetchLatestRelease(repo);
                    for (Mod m : fromGit) {
                        if (m.getId() == null) continue;
                        if (seen.add(m.getId())) newCache.add(m);
                    }
                }
                LOG.info("Imported {} mods from GitHub", newCache.size());
            } catch (Exception e) {
                LOG.warn("Failed to fetch from GitHub: {}", e.getMessage());
            }
        }

        // Only keep sample mods if no real mods were fetched
        if (newCache.isEmpty()) {
            LOG.info("No mods fetched from repositories, keeping sample mods");
            for (Mod m : new ArrayList<>(modCache)) {
                if (seen.add(m.getId())) newCache.add(m);
            }
        } else {
            LOG.info("Real mods fetched, removing sample mods from cache");
        }

        modCache.clear();
        modCache.addAll(newCache);

        LOG.info("Mod cache refresh complete. Total mods: {}", modCache.size());
    }
}
