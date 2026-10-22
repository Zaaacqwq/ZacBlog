package vip.zaaac.blog.content;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.common.RateLimiter;
import vip.zaaac.blog.common.Sql;
import vip.zaaac.blog.common.Times;
import vip.zaaac.blog.content.Dtos.Article;
import vip.zaaac.blog.content.Dtos.FileInfo;
import vip.zaaac.blog.content.Dtos.TagRef;

/**
 * 前台文章读取。旧库约定：cms_blog.status '1' 发布 / '0' 草稿；type '1' 文章 / '2' 随笔；
 * blog_pic_type '0' 用 blog_pic_link（外链）/ '1' 用 blog_pic（上传）。
 */
@Service
public class ArticleService {

    static final String TYPE_ARTICLE = "1";
    static final String TYPE_ESSAY = "2";
    private static final Duration VIEW_DEDUP_WINDOW = Duration.ofMinutes(10);
    static final String LIST_COLUMNS = """
            b.id, b.title, b.blog_desc, b.blog_pic_type, b.blog_pic, b.blog_pic_link, b.type, b.`top`,
            b.status, b.views, b.create_time, b.update_time, b.content_type""";

    public record ListFilter(Long categoryId, Long tagId, String keyword, Boolean essay) {
    }


    private final JdbcClient jdbc;
    private final Times times;
    private final RateLimiter rateLimiter;

    public ArticleService(JdbcClient jdbc, Times times, RateLimiter rateLimiter) {
        this.jdbc = jdbc;
        this.times = times;
        this.rateLimiter = rateLimiter;
    }

    /** 已发布文章分页：置顶在前，再按创建时间倒序 */
    public PageResult<Article> list(ListFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder(" WHERE b.status = '1'");
        Map<String, Object> params = new HashMap<>();
        if (filter.essay() != null) {
            where.append(" AND b.type = :type");
            params.put("type", filter.essay() ? TYPE_ESSAY : TYPE_ARTICLE);
        }
        if (filter.categoryId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM cms_blog_type bt WHERE bt.blog_id = b.id AND bt.type_id = :categoryId)");
            params.put("categoryId", filter.categoryId());
        }
        if (filter.tagId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM cms_blog_tag bg WHERE bg.blog_id = b.id AND bg.tag_id = :tagId)");
            params.put("tagId", filter.tagId());
        }
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            where.append(" AND LOWER(b.title) LIKE :keyword");
            params.put("keyword", "%" + escapeLike(filter.keyword().trim().toLowerCase(Locale.ROOT)) + "%");
        }
        long total = jdbc.sql("SELECT COUNT(*) FROM cms_blog b" + where).params(params).query(Long.class).single();
        params.put("limit", page.pageSize());
        params.put("offset", page.offset());
        List<Article> rows = jdbc.sql("SELECT " + LIST_COLUMNS + " FROM cms_blog b" + where
                        + " ORDER BY b.`top` DESC, b.create_time DESC, b.id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> mapArticle(rs, false))
                .list();
        return new PageResult<>(enrich(rows), total);
    }

    /** 推荐：阅读量最高的已发布文章 */
    public List<Article> recommend(int limit) {
        List<Article> rows = jdbc.sql("SELECT " + LIST_COLUMNS + " FROM cms_blog b WHERE b.status = '1'"
                        + " ORDER BY b.views DESC, b.create_time DESC LIMIT :limit")
                .param("limit", Math.min(Math.max(limit, 1), 20))
                .query((rs, i) -> mapArticle(rs, false))
                .list();
        return enrich(rows);
    }

    public Article detail(long id) {
        Article article = jdbc.sql("SELECT " + LIST_COLUMNS + ", b.content, b.content_markdown FROM cms_blog b"
                        + " WHERE b.id = :id AND b.status = '1'")
                .param("id", id)
                .query((rs, i) -> mapArticle(rs, true))
                .optional()
                .orElseThrow(ApiException::notFound);
        Article enriched = enrich(List.of(article)).get(0);
        return withFiles(enriched, files(id));
    }

    /** 浏览量 +1；同一 IP 对同一篇文章 10 分钟内只计一次。返回最新浏览量 */
    public long addView(long id, String ip) {
        Long views = currentViews(id);
        if (rateLimiter.tryAcquire("view:" + ip + ":" + id, 1, VIEW_DEDUP_WINDOW) == 0) {
            jdbc.sql("UPDATE cms_blog SET views = COALESCE(views, 0) + 1 WHERE id = :id").param("id", id).update();
            views = currentViews(id);
        }
        return views;
    }

    public boolean isPublished(long id) {
        return jdbc.sql("SELECT COUNT(*) FROM cms_blog WHERE id = :id AND status = '1'")
                .param("id", id).query(Long.class).single() > 0;
    }

    public List<FileInfo> files(long articleId) {
        return files("cms_blog_file", "blog_id", articleId);
    }

    /** 文章或公告的附件。表名和列名是常量，不来自用户输入 */
    List<FileInfo> files(String linkTable, String ownerColumn, long ownerId) {
        return jdbc.sql("""
                SELECT f.file_id, f.file_origin_name, f.file_path, f.file_size_info, f.create_time
                FROM %s bf JOIN sys_file_info f ON f.file_id = bf.file_id
                WHERE bf.%s = :id AND f.del_flag = 'N'
                ORDER BY f.file_id
                """.formatted(linkTable, ownerColumn))
                .param("id", ownerId)
                .query((rs, i) -> new FileInfo(rs.getLong("file_id"), rs.getString("file_origin_name"),
                        rs.getString("file_path"), parseSize(rs.getString("file_size_info")), times.read(rs, "create_time")))
                .list();
    }

    private Long currentViews(long id) {
        return jdbc.sql("SELECT COALESCE(views, 0) FROM cms_blog WHERE id = :id AND status = '1'")
                .param("id", id).query(Long.class).optional().orElseThrow(ApiException::notFound);
    }

    Article mapArticle(ResultSet rs, boolean withContent) throws SQLException {
        String cover = "1".equals(rs.getString("blog_pic_type")) ? rs.getString("blog_pic") : rs.getString("blog_pic_link");
        return new Article(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("blog_desc"),
                Sql.emptyToNull(cover),
                withContent ? orEmpty(Sql.blobText(rs, "content_markdown")) : null,
                withContent ? orEmpty(Sql.blobText(rs, "content")) : null,
                withContent ? contentType(rs.getString("content_type")) : null,
                null, null, List.of(),
                "1".equals(rs.getString("top")),
                TYPE_ESSAY.equals(rs.getString("type")),
                "1".equals(rs.getString("status")) ? "published" : "draft",
                rs.getLong("views"),
                0,
                times.read(rs, "create_time"),
                times.read(rs, "update_time"),
                null);
    }

    /** 批量补上分类、标签和评论数 */
    List<Article> enrich(List<Article> articles) {
        if (articles.isEmpty()) {
            return articles;
        }
        List<Long> ids = articles.stream().map(Article::id).toList();
        Map<Long, TagRef> categories = new HashMap<>();
        jdbc.sql("""
                SELECT bt.blog_id, t.type_id, t.type_name FROM cms_blog_type bt
                JOIN cms_type t ON t.type_id = bt.type_id WHERE bt.blog_id IN (:ids) ORDER BY t.type_id
                """)
                .param("ids", ids)
                .query(rs -> {
                    categories.putIfAbsent(rs.getLong("blog_id"), new TagRef(rs.getLong("type_id"), rs.getString("type_name")));
                });
        Map<Long, List<TagRef>> tags = new HashMap<>();
        jdbc.sql("""
                SELECT bg.blog_id, t.tag_id, t.tag_name FROM cms_blog_tag bg
                JOIN cms_tag t ON t.tag_id = bg.tag_id WHERE bg.blog_id IN (:ids) ORDER BY t.tag_id
                """)
                .param("ids", ids)
                .query(rs -> {
                    tags.computeIfAbsent(rs.getLong("blog_id"), k -> new ArrayList<>())
                            .add(new TagRef(rs.getLong("tag_id"), rs.getString("tag_name")));
                });
        Map<Long, Long> comments = new HashMap<>();
        jdbc.sql("SELECT blog_id, COUNT(*) AS cnt FROM cms_comment WHERE del_flag = '0' AND blog_id IN (:ids) GROUP BY blog_id")
                .param("ids", ids)
                .query(rs -> {
                    comments.put(rs.getLong("blog_id"), rs.getLong("cnt"));
                });
        List<Article> result = new ArrayList<>(articles.size());
        for (Article a : articles) {
            TagRef category = categories.get(a.id());
            result.add(new Article(a.id(), a.title(), a.summary(), a.coverUrl(), a.contentMarkdown(), a.contentHtml(),
                    a.contentType(), category == null ? null : category.id(), category == null ? null : category.name(),
                    tags.getOrDefault(a.id(), List.of()), a.top(), a.essay(), a.status(), a.views(),
                    comments.getOrDefault(a.id(), 0L), a.createdAt(), a.updatedAt(), a.files()));
        }
        return result;
    }

    /** 归档：按年月分组，月份倒序 */
    public List<Dtos.ArchiveMonth> archives() {
        Map<String, List<Dtos.ArchiveEntry>> groups = new LinkedHashMap<>();
        jdbc.sql("SELECT id, title, create_time FROM cms_blog WHERE status = '1' AND create_time IS NOT NULL"
                        + " ORDER BY create_time DESC, id DESC")
                .query(rs -> {
                    OffsetDateTime createdAt = times.read(rs, "create_time");
                    groups.computeIfAbsent(createdAt.getYear() + "-" + createdAt.getMonthValue(), k -> new ArrayList<>())
                            .add(new Dtos.ArchiveEntry(rs.getLong("id"), rs.getString("title"), createdAt));
                });
        return groups.values().stream()
                .map(list -> new Dtos.ArchiveMonth(list.get(0).createdAt().getYear(),
                        list.get(0).createdAt().getMonthValue(), list))
                .toList();
    }

    static Article withFiles(Article a, List<FileInfo> files) {
        return new Article(a.id(), a.title(), a.summary(), a.coverUrl(), a.contentMarkdown(), a.contentHtml(),
                a.contentType(),
                a.categoryId(), a.categoryName(), a.tags(), a.top(), a.essay(), a.status(), a.views(),
                a.commentCount(), a.createdAt(), a.updatedAt(), files);
    }

    /** 旧库 content_type：'1' 富文本（Quill）、'3' 纯 HTML → html；'2'（Cherry）、'4'（Vditor）→ markdown */
    static String contentType(String legacy) {
        return "1".equals(legacy) || "3".equals(legacy) ? "html" : "markdown";
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static final Pattern SIZE = Pattern.compile("([0-9.]+)\\s*([KMG]?B)?", Pattern.CASE_INSENSITIVE);

    /** 旧库的 file_size_info 是 "1.63 MB" 这样的文本，换算成字节（近似值） */
    public static long parseSize(String text) {
        if (text == null) {
            return 0;
        }
        Matcher m = SIZE.matcher(text.trim());
        if (!m.lookingAt()) {
            return 0;
        }
        double value;
        try {
            value = Double.parseDouble(m.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
        String unit = m.group(2) == null ? "B" : m.group(2).toUpperCase(Locale.ROOT);
        double factor = switch (unit) {
            case "KB" -> 1024d;
            case "MB" -> 1024d * 1024;
            case "GB" -> 1024d * 1024 * 1024;
            default -> 1d;
        };
        return Math.round(value * factor);
    }
}
