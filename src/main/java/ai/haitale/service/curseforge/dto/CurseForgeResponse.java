package ai.haitale.service.curseforge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurseForgeResponse<T> {
    public T data;
    public CurseForgePagination pagination;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurseForgePagination {
        public int index;
        public int pageSize;
        public int resultCount;
        public int totalCount;
    }
}
