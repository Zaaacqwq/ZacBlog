package vip.zaaac.blog;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import vip.zaaac.blog.common.RateLimiter;

/** H2 集成测试的公共基类：每个用例前重置数据和限流计数 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    protected static final String ADMIN_PASSWORD = "test-only-password-123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected RateLimiter rateLimiter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected static RequestPostProcessor ip(String ip) {
        return req -> {
            req.setRemoteAddr(ip);
            return req;
        };
    }

    @BeforeEach
    void resetData() {
        rateLimiter.reset();
        for (String table : new String[] {"cms_blog_tag", "cms_blog_type", "cms_blog_file", "cms_comment_like",
                "cms_message_like", "cms_comment", "cms_message", "cms_blog", "cms_type", "cms_tag", "sys_notice",
                "sys_notice_file", "sys_file_info", "sys_user", "reader_block", "ip_block", "keyword_block"}) {
            jdbc.sql("DELETE FROM " + table).update();
        }
        String adminHash = passwordEncoder.encode(ADMIN_PASSWORD);
        jdbc.sql("""
                INSERT INTO sys_user (user_id, user_name, nick_name, avatar, password, status, del_flag) VALUES
                  (1, 'admin', '博主', '/profile/avatar/admin.png', :hash, '0', '0'),
                  (4, 'reader', '老读者', '/profile/avatar/reader.png', :hash, '0', '0')
                """).param("hash", adminHash).update();
        jdbc.sql("INSERT INTO cms_type (type_id, type_name) VALUES (1, '技术'), (2, '生活')").update();
        jdbc.sql("INSERT INTO cms_tag (tag_id, tag_name) VALUES (1, 'Java'), (2, 'Spring')").update();
        jdbc.sql("""
                INSERT INTO cms_blog (id, title, type, content_type, content, content_markdown, `top`, views, status,
                  blog_pic_type, blog_pic, blog_pic_link, blog_desc, create_time) VALUES
                  (1, '普通文章', '1', '2', X'3c703e68693c2f703e', X'2320686920', '0', 3, '1', '1',
                     '/profile/upload/2023/10/13/a.png', '', '摘要1', '2024-01-01 10:00:00'),
                  (2, '置顶文章', '1', '2', NULL, NULL, '1', 10, '1', '0', '', 'https://example.com/c.png', '摘要2',
                     '2023-01-01 10:00:00'),
                  (3, '草稿', '1', '2', NULL, NULL, '1', 0, '0', '0', '', '', '', '2025-01-01 10:00:00'),
                  (4, '一篇随笔', '2', '2', NULL, NULL, '0', 1, '1', '0', '', '', '', '2024-06-01 10:00:00')
                """).update();
        jdbc.sql("INSERT INTO cms_blog_type (type_id, blog_id) VALUES (1, 1), (1, 3), (2, 2)").update();
        jdbc.sql("INSERT INTO cms_blog_tag (tag_id, blog_id) VALUES (1, 1), (1, 3), (2, 2)").update();
        jdbc.sql("""
                INSERT INTO sys_file_info (file_id, file_origin_name, file_suffix, file_size_info, file_object_name,
                  file_path, del_flag, create_time) VALUES
                  (10, 'slides.pdf', 'pdf', '1.5 MB', 'x.pdf', '/profile/upload/2023/10/13/x.pdf', 'N', '2023-10-13 09:00:00')
                """).update();
        jdbc.sql("INSERT INTO cms_blog_file (file_id, blog_id) VALUES (10, 1)").update();
        // 旧数据：读者 4 的评论 + 博主回复（main_id 为空，测试按 parent_id 归楼）
        jdbc.sql("""
                INSERT INTO cms_comment (id, parent_id, main_id, like_num, content, type, blog_id, del_flag, user_id,
                  create_time) VALUES
                  (1, NULL, NULL, 2, '老评论', '0', 1, '0', 4, '2024-02-01 10:00:00'),
                  (2, 1, NULL, 0, '博主回复', '1', 1, '0', 1, '2024-02-02 10:00:00'),
                  (3, NULL, NULL, 0, '已删除', '0', 1, '1', 4, '2024-02-03 10:00:00')
                """).update();
        jdbc.sql("""
                INSERT INTO cms_message (id, parent_id, main_id, like_num, content, type, del_flag, user_id, create_time)
                VALUES (1, NULL, NULL, 0, '老留言', '0', '0', 4, '2024-03-01 10:00:00')
                """).update();
        jdbc.sql("""
                INSERT INTO sys_notice (notice_id, notice_title, notice_type, notice_content, status, create_time) VALUES
                  (1, '旧公告', '1', X'6f6c64', '0', '2024-01-01 00:00:00'),
                  (2, '新公告', '1', X'6e6577', '0', '2024-05-01 00:00:00'),
                  (3, '关闭的公告', '1', NULL, '1', '2024-06-01 00:00:00')
                """).update();
    }
}
