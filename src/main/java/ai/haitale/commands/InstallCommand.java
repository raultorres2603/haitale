package ai.haitale.commands;

import ai.haitale.model.Mod;
import ai.haitale.service.ModInstallationService;
import ai.haitale.service.ModRepositoryService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Command(name = "install",
         description = "Install one or more mods by their ID",
         mixinStandardHelpOptions = true)
public class InstallCommand implements Runnable {

    @Inject
    private ModRepositoryService repositoryService;

    @Inject
    private ModInstallationService installationService;

    @Parameters(index = "0..*",
                description = "Mod IDs to install (space-separated)")
    private List<String> modIds;

    @Option(names = {"-y", "--yes"},
            description = "Skip confirmation prompts")
    private boolean skipConfirmation;

    @Override
    public void run() {
        if (modIds == null || modIds.isEmpty()) {
            System.err.println("Error: Please provide at least one mod ID");
            System.err.println("Example: haitale install enhanced-building-1 magic-realms-2");
            System.err.println("Use 'haitale search' or 'haitale recommend' to find mod IDs");
            return;
        }

        System.out.println("Preparing to install " + modIds.size() + " mod(s)...");
        System.out.println();

        List<Mod> modsToInstall = new ArrayList<>();

        // Validate all mod IDs first
        for (String modId : modIds) {
            Mod mod = repositoryService.getModById(modId);
            if (mod == null) {
                System.err.println("Error: Mod not found: " + modId);
                System.err.println("Use 'haitale search' to find available mods");
                return;
            }

            if (!mod.isFreeLicense()) {
                System.err.println("Error: Mod '" + mod.getName() + "' does not have a free/open-source license: " + mod.getLicense());
                System.err.println("For security reasons, only free and open-source mods can be installed.");
                return;
            }

            modsToInstall.add(mod);
        }

        // Display what will be installed
        System.out.println("The following mods will be installed:");
        System.out.println("=====================================");
        for (Mod mod : modsToInstall) {
            System.out.println("  • " + mod.getName() + " v" + mod.getVersion());
            System.out.println("    License: " + mod.getLicense());
            System.out.println("    Size: " + formatFileSize(mod.getFileSize()));
            System.out.println();
        }

        if (!skipConfirmation) {
            System.out.print("Proceed with installation? (y/N): ");
            String response = System.console() != null ? System.console().readLine() : "N";
            if (!response.trim().equalsIgnoreCase("y")) {
                System.out.println("Installation cancelled.");
                return;
            }
        }

        // Install each mod
        int successCount = 0;
        for (Mod mod : modsToInstall) {
            try {
                System.out.println();
                System.out.println("Installing " + mod.getName() + "...");
                installationService.installMod(mod);
                System.out.println("✓ Successfully installed " + mod.getName());
                successCount++;
            } catch (IOException e) {
                System.err.println("✗ Failed to install " + mod.getName() + ": " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("Installation complete!");
        System.out.println("Successfully installed: " + successCount + "/" + modsToInstall.size() + " mods");

        if (successCount > 0) {
            System.out.println();
            System.out.println("Restart HyTale to load the new mods.");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}

