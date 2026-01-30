package ai.haitale.commands;

import ai.haitale.model.Mod;
import ai.haitale.service.ModRepositoryService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = "search",
         description = "Search for mods by keyword",
         mixinStandardHelpOptions = true)
public class SearchCommand implements Runnable {

    @Inject
    private ModRepositoryService repositoryService;

    @Parameters(index = "0..*",
                description = "Search keywords (e.g., 'building', 'magic', 'adventure')")
    private String[] keywords;

    @Override
    public void run() {
        if (keywords == null || keywords.length == 0) {
            System.err.println("Error: Please provide a search keyword");
            System.err.println("Example: haitale search magic");
            return;
        }

        String searchTerm = String.join(" ", keywords);
        System.out.println("Searching for mods matching: " + searchTerm);
        System.out.println();

        List<Mod> results = repositoryService.searchMods(searchTerm);

        if (results.isEmpty()) {
            System.out.println("No mods found matching your search.");
            System.out.println("Try different keywords or use 'haitale recommend' for AI-powered suggestions.");
            return;
        }

        System.out.println("Found " + results.size() + " mod(s):");
        System.out.println("======================");
        System.out.println();

        for (Mod mod : results) {
            System.out.println("Name: " + mod.getName() + " v" + mod.getVersion());
            System.out.println("ID: " + mod.getId());
            System.out.println("Author: " + mod.getAuthor());
            System.out.println("License: " + mod.getLicense() + (mod.isFreeLicense() ? " ✓" : " ✗"));
            System.out.println("Description: " + mod.getDescription());
            System.out.println("Source: " + mod.getSource());
            System.out.println();
        }

        System.out.println("To install a mod, use: haitale install <mod-id>");
    }
}
