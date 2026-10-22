package vip.zaaac.blog.content;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 前台读取接口的响应模型，字段见 docs/api.md 第 5 节 */
public final class Dtos {

    private Dtos() {
    }

    public record TagRef(long id, String name) {
    }

    public record Article(
            long id,
            String title,
            String summary,
            String coverUrl,
            @JsonInclude(JsonInclude.Include.NON_NULL) String contentMarkdown,
            @JsonInclude(JsonInclude.Include.NON_NULL) String contentHtml,
            @JsonInclude(JsonInclude.Include.NON_NULL) String contentType,
            Long categoryId,
            String categoryName,
            List<TagRef> tags,
            boolean top,
            boolean essay,
            String status,
            long views,
            long commentCount,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<FileInfo> files) {
    }

    public record Category(long id, String name, long articleCount) {
    }

    public record Tag(long id, String name, long articleCount) {
    }

    public record Notice(long id, String title, String content, OffsetDateTime createdAt) {
    }

    public record FileInfo(long id, String name, String url, long size, OffsetDateTime createdAt) {
    }

    public record ArchiveEntry(long id, String title, OffsetDateTime createdAt) {
    }

    public record ArchiveMonth(int year, int month, List<ArchiveEntry> articles) {
    }

    public record SiteStats(long articleCount, long categoryCount, long tagCount, long commentCount,
            long messageCount, long totalViews) {
    }

    public record About(String content) {
    }
}
