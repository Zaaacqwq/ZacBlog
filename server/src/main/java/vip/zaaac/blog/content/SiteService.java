package vip.zaaac.blog.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.common.Sql;
import vip.zaaac.blog.common.Times;

/** 分类、标签、公告、侧栏统计、About */
@Service
public class SiteService {

    private final JdbcClient jdbc;
    private final Times times;
    private volatile String about;

    public SiteService(JdbcClient jdbc, Times times) {
        this.jdbc = jdbc;
        this.times = times;
    }

    public List<Dtos.Category> categories() {
        return jdbc.sql("""
                SELECT t.type_id, t.type_name, COUNT(b.id) AS cnt FROM cms_type t
                LEFT JOIN cms_blog_type bt ON bt.type_id = t.type_id
                LEFT JOIN cms_blog b ON b.id = bt.blog_id AND b.status = '1'
                GROUP BY t.type_id, t.type_name ORDER BY t.type_id
                """)
                .query((rs, i) -> new Dtos.Category(rs.getLong("type_id"), rs.getString("type_name"), rs.getLong("cnt")))
                .list();
    }

    public List<Dtos.Tag> tags() {
        return jdbc.sql("""
                SELECT t.tag_id, t.tag_name, COUNT(b.id) AS cnt FROM cms_tag t
                LEFT JOIN cms_blog_tag bg ON bg.tag_id = t.tag_id
                LEFT JOIN cms_blog b ON b.id = bg.blog_id AND b.status = '1'
                GROUP BY t.tag_id, t.tag_name ORDER BY t.tag_id
                """)
                .query((rs, i) -> new Dtos.Tag(rs.getLong("tag_id"), rs.getString("tag_name"), rs.getLong("cnt")))
                .list();
    }

    /** 状态正常（status='0'）的公告，最新的在前 */
    public PageResult<Dtos.Notice> notices(PageQuery page) {
        long total = jdbc.sql("SELECT COUNT(*) FROM sys_notice WHERE status = '0'").query(Long.class).single();
        List<Dtos.Notice> rows = jdbc.sql("""
                SELECT notice_id, notice_title, notice_content, create_time FROM sys_notice WHERE status = '0'
                ORDER BY create_time DESC, notice_id DESC LIMIT :limit OFFSET :offset
                """)
                .param("limit", page.pageSize())
                .param("offset", page.offset())
                .query((rs, i) -> new Dtos.Notice(rs.getLong("notice_id"), rs.getString("notice_title"),
                        Sql.blobText(rs, "notice_content"), times.read(rs, "create_time")))
                .list();
        return new PageResult<>(rows, total);
    }

    public Dtos.Notice latestNotice() {
        List<Dtos.Notice> rows = notices(new PageQuery(1, 1)).rows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Dtos.SiteStats stats() {
        return jdbc.sql("""
                SELECT
                  (SELECT COUNT(*) FROM cms_blog WHERE status = '1') AS article_count,
                  (SELECT COUNT(*) FROM cms_type) AS category_count,
                  (SELECT COUNT(*) FROM cms_tag) AS tag_count,
                  (SELECT COUNT(*) FROM cms_comment WHERE del_flag = '0') AS comment_count,
                  (SELECT COUNT(*) FROM cms_message WHERE del_flag = '0') AS message_count,
                  (SELECT COALESCE(SUM(views), 0) FROM cms_blog WHERE status = '1') AS total_views
                """)
                .query((rs, i) -> new Dtos.SiteStats(rs.getLong("article_count"), rs.getLong("category_count"),
                        rs.getLong("tag_count"), rs.getLong("comment_count"), rs.getLong("message_count"),
                        rs.getLong("total_views")))
                .single();
    }

    /**
     * About 页内容。旧版来自前端打包的静态 Markdown（frontend/.../my/aboutMe.md），
     * 这里沿用同一份文件，放在 classpath:about/about.md。
     */
    public Dtos.About about() {
        String cached = about;
        if (cached == null) {
            try (InputStream in = new ClassPathResource("about/about.md").getInputStream()) {
                cached = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            about = cached;
        }
        return new Dtos.About(cached);
    }
}
