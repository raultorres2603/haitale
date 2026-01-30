package ai.haitale.model;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Serdeable
public class InstallationManifest {
    private List<InstalledMod> installedMods = new ArrayList<>();
    private Instant lastUpdated;
    private String hytaleVersion;

    public InstallationManifest() {
        this.lastUpdated = Instant.now();
    }

    public List<InstalledMod> getInstalledMods() {
        return installedMods;
    }

    public void setInstalledMods(List<InstalledMod> installedMods) {
        this.installedMods = installedMods;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getHytaleVersion() {
        return hytaleVersion;
    }

    public void setHytaleVersion(String hytaleVersion) {
        this.hytaleVersion = hytaleVersion;
    }

    @Serdeable
    public static class InstalledMod {
        private String modId;
        private String name;
        private String version;
        private String checksum;
        private Instant installedAt;
        private String installedPath;

        public InstalledMod() {
        }

        public InstalledMod(String modId, String name, String version, String checksum, String installedPath) {
            this.modId = modId;
            this.name = name;
            this.version = version;
            this.checksum = checksum;
            this.installedPath = installedPath;
            this.installedAt = Instant.now();
        }

        // Getters and Setters
        public String getModId() {
            return modId;
        }

        public void setModId(String modId) {
            this.modId = modId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getChecksum() {
            return checksum;
        }

        public void setChecksum(String checksum) {
            this.checksum = checksum;
        }

        public Instant getInstalledAt() {
            return installedAt;
        }

        public void setInstalledAt(Instant installedAt) {
            this.installedAt = installedAt;
        }

        public String getInstalledPath() {
            return installedPath;
        }

        public void setInstalledPath(String installedPath) {
            this.installedPath = installedPath;
        }
    }
}
