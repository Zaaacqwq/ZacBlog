package vip.zaaac.blog.content;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.common.Sql;
import vip.zaaac.blog.common.Times;
import vip.zaaac.blog.content.Dtos.Article;

/**
 * 后台：文章（含草稿）、分类、标签、公告的增删改查。
 * 写入沿用旧库约定：status '1' 发布 / '0' 草稿；type '1' 文章 / '2' 随笔；
 * 封面是 /profile/ 开头的上传文件时 blog_pic_type '1' 存 blog_pic，否则 '0' 存 blog_pic_link（外链）。
 */
@Service
public class AdminContentService {

    // 新文章的 content_type：Markdown 用 '4'（旧后台的默认编辑器 Vditor），富文本用 '1'
    private static final String LEGACY_MARKDOWN = "4";
    private static final String LEGACY_HTML = "1";

    public record ArticleForm(String title, String summary, String coverUrl, String contentType,
            String contentMarkdown, String contentHtml, Long categoryId, List<Long> tagIds, boolean top,
            boolean essay, String status, List<Long> fileIds) {
    }

    public record AdminFilter(String keyword, Long categoryId, Long tagId, String status, Boolean essay) {
    }

    public record NoticeForm(String title, String content, String status, List<Long> fileIds) {
    }

    public record AdminNotice(long id, String title, String content, String status, List<Dtos.FileInfo> files,
            java.time.OffsetDateTime createdAt, java.time.OffsetDateTime updatedAt) {
    }

    private final JdbcClient jdbc;
    private final Times times;
    private final ArticleService articles;

    public AdminContentService(JdbcClient jdbc, Times times, ArticleService articles) {
        this.jdbc = jdbc;
        this.times = times;
        this.articles = articles;
    }

    // ---------- 文章 ----------

    /** 全部文章（含草稿）分页：置顶在前，再按创建时间倒序 */
    public PageResult<Article> listArticles(AdminFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND b.status = :status");
            params.put("status", legacyStatus(filter.status()));
        }
        if (filter.essay() != null) {
            where.append(" AND b.type = :type");
            params.put("type", filter.essay() ? ArticleService.TYPE_ESSAY : ArticleService.TYPE_ARTICLE);
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
            params.put("keyword", "%" + ArticleService.escapeLike(filter.keyword().trim().toLowerCase(Locale.ROOT)) + "%");
        }
        long total = jdbc.sql("SELECT COUNT(*) FROM cms_blog b" + where).params(params).query(Long.class).single();
        params.put("limit", page.pageSize());
        params.put("offset", page.offset());
        List<Article> rows = jdbc.sql("SELECT " + ArticleService.LIST_COLUMNS + " FROM cms_blog b" + where
                        + " ORDER BY b.`top` DESC, b.create_time DESC, b.id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> articles.mapArticle(rs, false))
                .list();
        return new PageResult<>(articles.enrich(rows), total);
    }

    /** 详情（含草稿、正文、附件） */
    public Article article(long id) {
        Article article = jdbc.sql("SELECT " + ArticleService.LIST_COLUMNS + ", b.content, b.content_markdown"
                        + " FROM cms_blog b WHERE b.id = :id")
                .param("id", id)
                .query((rs, i) -> articles.mapArticle(rs, true))
                .optional()
                .orElseThrow(ApiException::notFound);
        return ArticleService.withFiles(articles.enrich(List.of(article)).get(0), articles.files(id));
    }

    @Transactional
    public Article createArticle(ArticleForm form, String username) {
        validateRefs(form);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO cms_blog (create_by, create_time, update_by, update_time, title, type, content_type,
                  content, content_markdown, `top`, views, status, blog_pic_type, blog_pic, blog_pic_link, blog_desc)
                VALUES (:user, :now, :user, :now, :title, :type, :contentType, :content, :markdown, :top, 0, :status,
                  :picType, :pic, :picLink, :summary)
                """)
                .params(articleParams(form, username))
                .update(keys, "id");
        long id = keys.getKey().longValue();
        writeArticleLinks(id, form);
        return article(id);
    }

    @Transactional
    public Article updateArticle(long id, ArticleForm form, String username) {
        validateRefs(form);
        Map<String, Object> params = articleParams(form, username);
        params.put("id", id);
        int changed = jdbc.sql("""
                UPDATE cms_blog SET update_by = :user, update_time = :now, title = :title, type = :type,
                  content_type = :contentType, content = :content, content_markdown = :markdown, `top` = :top,
                  status = :status, blog_pic_type = :picType, blog_pic = :pic, blog_pic_link = :picLink,
                  blog_desc = :summary
                WHERE id = :id
                """)
                .params(params)
                .update();
        if (changed == 0) {
            throw ApiException.notFound();
        }
        for (String table : new String[] {"cms_blog_type", "cms_blog_tag", "cms_blog_file"}) {
            jdbc.sql("DELETE FROM " + table + " WHERE blog_id = :id").param("id", id).update();
        }
        writeArticleLinks(id, form);
        return article(id);
    }

    /** 删除文章：关联的分类、标签、附件关系一并删除，文章下的评论软删除 */
    @Transactional
    public void deleteArticle(long id) {
        for (String table : new String[] {"cms_blog_type", "cms_blog_tag", "cms_blog_file"}) {
            jdbc.sql("DELETE FROM " + table + " WHERE blog_id = :id").param("id", id).update();
        }
        if (jdbc.sql("DELETE FROM cms_blog WHERE id = :id").param("id", id).update() == 0) {
            throw ApiException.notFound();
        }
        jdbc.sql("UPDATE cms_comment SET del_flag = '1' WHERE blog_id = :id").param("id", id).update();
    }

    private Map<String, Object> articleParams(ArticleForm form, String username) {
        boolean html = "html".equals(form.contentType());
        String cover = Sql.emptyToNull(form.coverUrl());
        boolean uploaded = cover != null && cover.startsWith("/profile/");
        Map<String, Object> params = new HashMap<>();
        params.put("user", username);
        params.put("now", times.now());
        params.put("title", form.title().strip());
        params.put("type", form.essay() ? ArticleService.TYPE_ESSAY : ArticleService.TYPE_ARTICLE);
        params.put("contentType", html ? LEGACY_HTML : LEGACY_MARKDOWN);
        params.put("content", bytes(form.contentHtml()));
        params.put("markdown", bytes(form.contentMarkdown()));
        params.put("top", form.top() ? "1" : "0");
        params.put("status", legacyStatus(form.status()));
        params.put("picType", uploaded ? "1" : "0");
        params.put("pic", uploaded ? cover : "");
        params.put("picLink", uploaded || cover == null ? "" : cover);
        params.put("summary", form.summary() == null ? "" : form.summary().strip());
        return params;
    }

    private void writeArticleLinks(long id, ArticleForm form) {
        if (form.categoryId() != null) {
            jdbc.sql("INSERT INTO cms_blog_type (type_id, blog_id) VALUES (:ref, :id)")
                    .param("ref", form.categoryId()).param("id", id).update();
        }
        link("INSERT INTO cms_blog_tag (tag_id, blog_id) VALUES (:ref, :id)", id, form.tagIds());
        link("INSERT INTO cms_blog_file (file_id, blog_id) VALUES (:ref, :id)", id, form.fileIds());
    }

    private void validateRefs(ArticleForm form) {
        if (form.categoryId() != null && count("cms_type", "type_id", List.of(form.categoryId())) == 0) {
            throw ApiException.badRequest("分类不存在");
        }
        requireAll("cms_tag", "tag_id", form.tagIds(), "标签不存在");
        requireFiles(form.fileIds());
    }

    // ---------- 分类、标签 ----------

    /** 后台列表的 articleCount 包含草稿 */
    public List<Dtos.Category> categories() {
        return jdbc.sql("""
                SELECT t.type_id, t.type_name, COUNT(bt.blog_id) AS cnt FROM cms_type t
                LEFT JOIN cms_blog_type bt ON bt.type_id = t.type_id
                GROUP BY t.type_id, t.type_name ORDER BY t.type_id
                """)
                .query((rs, i) -> new Dtos.Category(rs.getLong("type_id"), rs.getString("type_name"), rs.getLong("cnt")))
                .list();
    }

    public List<Dtos.Tag> tags() {
        return jdbc.sql("""
                SELECT t.tag_id, t.tag_name, COUNT(bg.blog_id) AS cnt FROM cms_tag t
                LEFT JOIN cms_blog_tag bg ON bg.tag_id = t.tag_id
                GROUP BY t.tag_id, t.tag_name ORDER BY t.tag_id
                """)
                .query((rs, i) -> new Dtos.Tag(rs.getLong("tag_id"), rs.getString("tag_name"), rs.getLong("cnt")))
                .list();
    }

    public Dtos.Category createCategory(String name, String username) {
        long id = insertNamed("cms_type", "type_id", "type_name", name, username);
        return new Dtos.Category(id, name.strip(), 0);
    }

    public Dtos.Category updateCategory(long id, String name, String username) {
        renameNamed("cms_type", "type_id", "type_name", id, name, username);
        return categories().stream().filter(c -> c.id() == id).findFirst().orElseThrow(ApiException::notFound);
    }

    /** 仍有文章的分类不能删除 */
    @Transactional
    public void deleteCategory(long id) {
        if (jdbc.sql("SELECT COUNT(*) FROM cms_blog_type WHERE type_id = :id").param("id", id).query(Long.class).single() > 0) {
            throw ApiException.badRequest("分类下还有文章，不能删除");
        }
        if (jdbc.sql("DELETE FROM cms_type WHERE type_id = :id").param("id", id).update() == 0) {
            throw ApiException.notFound();
        }
    }

    public Dtos.Tag createTag(String name, String username) {
        long id = insertNamed("cms_tag", "tag_id", "tag_name", name, username);
        return new Dtos.Tag(id, name.strip(), 0);
    }

    public Dtos.Tag updateTag(long id, String name, String username) {
        renameNamed("cms_tag", "tag_id", "tag_name", id, name, username);
        return tags().stream().filter(t -> t.id() == id).findFirst().orElseThrow(ApiException::notFound);
    }

    /** 删除标签，同时解除与文章的关联 */
    @Transactional
    public void deleteTag(long id) {
        jdbc.sql("DELETE FROM cms_blog_tag WHERE tag_id = :id").param("id", id).update();
        if (jdbc.sql("DELETE FROM cms_tag WHERE tag_id = :id").param("id", id).update() == 0) {
            throw ApiException.notFound();
        }
    }

    private long insertNamed(String table, String idColumn, String nameColumn, String name, String username) {
        String trimmed = name.strip();
        if (jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE " + nameColumn + " = :name")
                .param("name", trimmed).query(Long.class).single() > 0) {
            throw ApiException.badRequest("名称已存在");
        }
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO " + table + " (" + nameColumn + ", create_by, create_time) VALUES (:name, :user, :now)")
                .param("name", trimmed).param("user", username).param("now", times.now())
                .update(keys, idColumn);
        return keys.getKey().longValue();
    }

    private void renameNamed(String table, String idColumn, String nameColumn, long id, String name, String username) {
        String trimmed = name.strip();
        if (jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE " + nameColumn + " = :name AND " + idColumn + " <> :id")
                .param("name", trimmed).param("id", id).query(Long.class).single() > 0) {
            throw ApiException.badRequest("名称已存在");
        }
        int changed = jdbc.sql("UPDATE " + table + " SET " + nameColumn + " = :name, update_by = :user, update_time = :now"
                        + " WHERE " + idColumn + " = :id")
                .param("name", trimmed).param("user", username).param("now", times.now()).param("id", id)
                .update();
        if (changed == 0) {
            throw ApiException.notFound();
        }
    }

    // ---------- 公告 ----------

    /** 全部公告（含关闭的）分页，最新在前；列表不带附件 */
    public PageResult<AdminNotice> notices(String keyword, PageQuery page) {
        String where = "";
        Map<String, Object> params = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            where = " WHERE LOWER(notice_title) LIKE :keyword";
            params.put("keyword", "%" + ArticleService.escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%");
        }
        long total = jdbc.sql("SELECT COUNT(*) FROM sys_notice" + where).params(params).query(Long.class).single();
        params.put("limit", page.pageSize());
        params.put("offset", page.offset());
        List<AdminNotice> rows = jdbc.sql("SELECT notice_id, notice_title, notice_content, status, create_time, update_time"
                        + " FROM sys_notice" + where
                        + " ORDER BY create_time DESC, notice_id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> new AdminNotice(rs.getLong("notice_id"), rs.getString("notice_title"),
                        Sql.blobText(rs, "notice_content"), "0".equals(rs.getString("status")) ? "normal" : "closed",
                        null, times.read(rs, "create_time"), times.read(rs, "update_time")))
                .list();
        return new PageResult<>(rows, total);
    }

    public AdminNotice notice(long id) {
        AdminNotice notice = jdbc.sql("SELECT notice_id, notice_title, notice_content, status, create_time, update_time"
                        + " FROM sys_notice WHERE notice_id = :id")
                .param("id", id)
                .query((rs, i) -> new AdminNotice(rs.getLong("notice_id"), rs.getString("notice_title"),
                        Sql.blobText(rs, "notice_content"), "0".equals(rs.getString("status")) ? "normal" : "closed",
                        null, times.read(rs, "create_time"), times.read(rs, "update_time")))
                .optional()
                .orElseThrow(ApiException::notFound);
        return new AdminNotice(notice.id(), notice.title(), notice.content(), notice.status(),
                articles.files("sys_notice_file", "notice_id", id), notice.createdAt(), notice.updatedAt());
    }

    @Transactional
    public AdminNotice createNotice(NoticeForm form, String username) {
        requireFiles(form.fileIds());
        KeyHolder keys = new GeneratedKeyHolder();
        // notice_type 沿用若依的 '1'（通知）
        jdbc.sql("""
                INSERT INTO sys_notice (notice_title, notice_type, notice_content, status, create_by, create_time,
                  update_by, update_time)
                VALUES (:title, '1', :content, :status, :user, :now, :user, :now)
                """)
                .param("title", form.title().strip())
                .param("content", bytes(form.content()))
                .param("status", noticeStatus(form.status()))
                .param("user", username)
                .param("now", times.now())
                .update(keys, "notice_id");
        long id = keys.getKey().longValue();
        link("INSERT INTO sys_notice_file (file_id, notice_id) VALUES (:ref, :id)", id, form.fileIds());
        return notice(id);
    }

    @Transactional
    public AdminNotice updateNotice(long id, NoticeForm form, String username) {
        requireFiles(form.fileIds());
        int changed = jdbc.sql("""
                UPDATE sys_notice SET notice_title = :title, notice_content = :content, status = :status,
                  update_by = :user, update_time = :now WHERE notice_id = :id
                """)
                .param("title", form.title().strip())
                .param("content", bytes(form.content()))
                .param("status", noticeStatus(form.status()))
                .param("user", username)
                .param("now", times.now())
                .param("id", id)
                .update();
        if (changed == 0) {
            throw ApiException.notFound();
        }
        jdbc.sql("DELETE FROM sys_notice_file WHERE notice_id = :id").param("id", id).update();
        link("INSERT INTO sys_notice_file (file_id, notice_id) VALUES (:ref, :id)", id, form.fileIds());
        return notice(id);
    }

    @Transactional
    public void deleteNotice(long id) {
        jdbc.sql("DELETE FROM sys_notice_file WHERE notice_id = :id").param("id", id).update();
        if (jdbc.sql("DELETE FROM sys_notice WHERE notice_id = :id").param("id", id).update() == 0) {
            throw ApiException.notFound();
        }
    }

    // ---------- 工具 ----------

    private void link(String sql, long id, List<Long> refs) {
        if (refs == null) {
            return;
        }
        for (Long ref : new LinkedHashSet<>(refs)) {
            jdbc.sql(sql).param("ref", ref).param("id", id).update();
        }
    }

    private void requireFiles(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        long found = jdbc.sql("SELECT COUNT(*) FROM sys_file_info WHERE del_flag = 'N' AND file_id IN (:ids)")
                .param("ids", new LinkedHashSet<>(fileIds)).query(Long.class).single();
        if (found != new LinkedHashSet<>(fileIds).size()) {
            throw ApiException.badRequest("附件不存在");
        }
    }

    private void requireAll(String table, String idColumn, List<Long> ids, String msg) {
        if (ids != null && !ids.isEmpty() && count(table, idColumn, new LinkedHashSet<>(ids)) != new LinkedHashSet<>(ids).size()) {
            throw ApiException.badRequest(msg);
        }
    }

    private long count(String table, String idColumn, Collection<Long> ids) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " IN (:ids)")
                .param("ids", ids).query(Long.class).single();
    }

    private static String legacyStatus(String status) {
        return switch (status) {
            case "published" -> "1";
            case "draft" -> "0";
            default -> throw ApiException.badRequest("status 只能是 published 或 draft");
        };
    }

    private static String noticeStatus(String status) {
        if (status == null || status.equals("normal")) {
            return "0";
        }
        if (status.equals("closed")) {
            return "1";
        }
        throw ApiException.badRequest("status 只能是 normal 或 closed");
    }

    private static byte[] bytes(String text) {
        return text == null ? null : text.getBytes(StandardCharsets.UTF_8);
    }
}
