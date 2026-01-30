package ai.haitale.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Mod {
    private String id;
    private String name;
    private String version;
    private String description;
    private String downloadUrl;
    private String checksum;
    private String checksumAlgorithm; // SHA-256, SHA-512, etc.
    private String license;
    private String author;
    private String source; // modrinth, curseforge, github
    private long fileSize;

    public Mod() {
    }

    public Mod(String id, String name, String version, String description,
               String downloadUrl, String checksum, String checksumAlgorithm,
               String license, String author, String source, long fileSize) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.downloadUrl = downloadUrl;
        this.checksum = checksum;
        this.checksumAlgorithm = checksumAlgorithm;
        this.license = license;
        this.author = author;
        this.source = source;
        this.fileSize = fileSize;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isFreeLicense() {
        if (license == null) return false;
        String lowerLicense = license.toLowerCase();
        return lowerLicense.contains("mit") ||
               lowerLicense.contains("apache") ||
               lowerLicense.contains("gpl") ||
               lowerLicense.contains("lgpl") ||
               lowerLicense.contains("bsd") ||
               lowerLicense.contains("mpl") ||
               lowerLicense.contains("cc0") ||
               lowerLicense.contains("public domain") ||
               lowerLicense.contains("unlicense");
    }

    @Override
    public String toString() {
        return String.format("%s v%s by %s [%s] - %s",
            name, version, author, license, description);
    }
}
