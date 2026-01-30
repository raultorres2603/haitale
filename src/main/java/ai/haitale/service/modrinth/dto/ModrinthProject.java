package ai.haitale.service.modrinth.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public class ModrinthProject {
    public String id;
    public String slug;
    public String title;
    public String name;
    public String description;
    public List<String> versions;
    public String latest_version;
    public List<ModrinthAuthor> authors;
    public String license;

    @Serdeable
    public static class ModrinthAuthor {
        public String username;
        public String id;
    }
}
