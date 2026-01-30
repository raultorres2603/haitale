package ai.haitale.service.curseforge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurseForgeProject {
    public int id;
    public String name;
    public String slug;
    public String summary;

    @JsonProperty("downloadCount")
    public long downloadCount;

    public List<CurseForgeAuthor> authors;
    public List<CurseForgeFile> latestFiles;
    public List<CurseForgeCategory> categories;

    public CurseForgeLinks links;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurseForgeAuthor {
        public String name;
        public String url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurseForgeFile {
        public int id;
        public String displayName;
        public String fileName;
        public long fileLength;
        public String downloadUrl;
        public List<CurseForgeHash> hashes;
        public List<String> gameVersions;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CurseForgeHash {
            public String value;
            public int algo; // 1=SHA-1, 2=MD5
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurseForgeCategory {
        public int id;
        public String name;
        public String slug;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurseForgeLinks {
        public String websiteUrl;
        public String wikiUrl;
        public String issuesUrl;
        public String sourceUrl;
    }
}
