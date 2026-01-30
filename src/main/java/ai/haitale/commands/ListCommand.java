package ai.haitale.commands;

import ai.haitale.model.InstallationManifest.InstalledMod;
import ai.haitale.service.ModInstallationService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Command(name = "list",
         description = "List all installed mods",
         mixinStandardHelpOptions = true)
public class ListCommand implements Runnable {

    @Inject
    private ModInstallationService installationService;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void run() {
        try {
            List<InstalledMod> installedMods = installationService.listInstalledMods();

            if (installedMods.isEmpty()) {
                System.out.println("No mods are currently installed.");
                System.out.println("Use 'haitale recommend' to get mod suggestions.");
                return;
            }

            System.out.println("Installed Mods:");
            System.out.println("===============");
            System.out.println();

            for (InstalledMod mod : installedMods) {
                System.out.println("Name: " + mod.getName() + " v" + mod.getVersion());
                System.out.println("ID: " + mod.getModId());
                System.out.println("Installed: " + DATE_FORMATTER.format(mod.getInstalledAt()));
                System.out.println("Path: " + mod.getInstalledPath());
                System.out.println("Checksum: " + mod.getChecksum());
                System.out.println();
            }

            System.out.println("Total: " + installedMods.size() + " mod(s) installed");

        } catch (IOException e) {
            System.err.println("Error reading installation manifest: " + e.getMessage());
        }
    }
}
