package ai.haitale.service.modrinth.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.Map;

@Serdeable
public class ModrinthVersion {
    public String id;
    public String name;
    public String version_number;
    public List<ModrinthFile> files;

    @Serdeable
    public static class ModrinthFile {
        public String url;
        public long size;
        public Map<String, String> hashes; // key: algorithm name ("sha1","sha384"...), value: hex
        public String filename;
    }
}
