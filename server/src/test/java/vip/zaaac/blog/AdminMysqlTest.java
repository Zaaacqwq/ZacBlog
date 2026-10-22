package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

import vip.zaaac.blog.auth.JwtService;

/**
 * 后台查询在真实 MySQL 上的回归测试。旧表（db/legacy/legacy_schema.sql）是 utf8mb4_general_ci，
 * mysql:8.0 容器的库默认是 utf8mb4_0900_ai_ci（与线上一致），V2 新建的黑名单表默认跟随库：
 * 跨新旧表的“列 = 列”比较在这里会报 Illegal mix of collations，H2 测试发现不了。
 * 需要 Docker，只在 CI 的 mysql-it job 里跑：mvn -B verify -Pmysql-it
 */
@Tag("mysql")
@Testcontainers
@SpringBootTest(properties = "blog.jwt.secret=test-only-not-a-real-secret-0123456789abcdef")
@AutoConfigureMockMvc
class AdminMysqlTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withInitScript("db/legacy/legacy_schema.sql");

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + jwtService.issue(1, "admin").token());
    }

    private ResultActions postPublic(String path, String ip, String nickname, String email, String content)
            throws Exception {
        return mvc.perform(post(path).with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                }).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"%s\",\"email\":\"%s\",\"content\":\"%s\"}".formatted(nickname, email, content)));
    }

    @Test
    void blockTablesUseTheLegacyCollation() {
        String legacy = collation("cms_comment", "author_email");
        assertThat(legacy).isEqualTo("utf8mb4_general_ci");
        assertThat(collation("reader_block", "email")).isEqualTo(legacy);
        assertThat(collation("ip_block", "ip")).isEqualTo(legacy);
        assertThat(collation("keyword_block", "keyword")).isEqualTo(legacy);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '11' AND success = 1")
                .query(Long.class).single()).isEqualTo(1L);
    }

    /**
     * 一个用例按顺序走完：读者聚合（旧注册读者 + 匿名读者 + 同一邮箱多次发表）→ 邮箱拉黑/解除 →
     * IP / 关键字黑名单 → 评论、留言列表 → 博主回复和删除 → 仪表盘。
     */
    @Test
    void adminQueriesWorkOnLegacySchema() throws Exception {
        // 旧注册读者 4 有邮箱；之后又用同一邮箱匿名评论一次，应该合并成一行
        jdbc.sql("UPDATE sys_user SET email = 'Old@Example.com' WHERE user_id = 4").update();
        postPublic("/public/articles/1/comments", "10.20.0.1", "访客", "Guest@Example.com", "匿名评论一").andExpect(status().isOk());
        postPublic("/public/articles/1/comments", "10.20.0.2", "访客", "guest@example.com", "匿名评论二").andExpect(status().isOk());
        postPublic("/public/messages", "10.20.0.3", "访客", "GUEST@example.com", "匿名留言").andExpect(status().isOk());
        postPublic("/public/articles/1/comments", "10.20.0.4", "老读者", "old@example.com", "老读者匿名评论").andExpect(status().isOk());

        // ---- 读者列表：博主自己（user 1）不算读者 ----
        mvc.perform(admin(get("/admin/readers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.rows[?(@.email == 'guest@example.com')].commentCount", contains(2)))
                .andExpect(jsonPath("$.data.rows[?(@.email == 'guest@example.com')].messageCount", contains(1)))
                .andExpect(jsonPath("$.data.rows[?(@.email == 'guest@example.com')].nickname", contains("访客")))
                .andExpect(jsonPath("$.data.rows[?(@.email == 'guest@example.com')].blocked", contains(false)))
                .andExpect(jsonPath("$.data.rows[?(@.email == 'old@example.com')].commentCount", contains(2)))
                .andExpect(jsonPath("$.data.rows[?(@.email == 'old@example.com')].messageCount", contains(1)));
        mvc.perform(admin(get("/admin/readers")).param("keyword", "老读").param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        // ---- 邮箱拉黑：列表里的拉黑标记来自 reader_block 与评论邮箱的跨表比较 ----
        mvc.perform(admin(post("/admin/readers/Guest@Example.com/block"))).andExpect(status().isOk());
        mvc.perform(admin(get("/admin/readers")).param("blocked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].email").value("guest@example.com"))
                .andExpect(jsonPath("$.data.rows[0].blocked").value(true));
        postPublic("/public/messages", "10.20.0.5", "访客", "guest@EXAMPLE.com", "又来了").andExpect(status().isForbidden());
        mvc.perform(admin(delete("/admin/readers/guest@example.com/block"))).andExpect(status().isOk());
        mvc.perform(admin(get("/admin/readers")).param("blocked", "false"))
                .andExpect(jsonPath("$.data.total").value(2));
        postPublic("/public/messages", "10.20.0.6", "访客", "guest@example.com", "解除后").andExpect(status().isOk());

        // ---- IP、关键字黑名单 ----
        String ipJson = mvc.perform(admin(post("/admin/readers/ip-blocks")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ip\":\"10.20.1.1\",\"reason\":\"spam\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(admin(get("/admin/readers/ip-blocks"))).andExpect(jsonPath("$.data.total").value(1));
        postPublic("/public/messages", "10.20.1.1", "a", "a@example.com", "hi").andExpect(status().isForbidden());
        mvc.perform(admin(delete("/admin/readers/ip-blocks/" + JsonPath.read(ipJson, "$.data.id")))).andExpect(status().isOk());
        String kwJson = mvc.perform(admin(post("/admin/readers/keyword-blocks")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"Casino\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        postPublic("/public/messages", "10.20.1.2", "a", "a@example.com", "casino").andExpect(status().isForbidden());
        mvc.perform(admin(delete("/admin/readers/keyword-blocks/" + JsonPath.read(kwJson, "$.data.id")))).andExpect(status().isOk());
        mvc.perform(admin(get("/admin/readers/keyword-blocks"))).andExpect(jsonPath("$.data.total").value(0));

        // ---- 评论、留言列表 ----
        mvc.perform(admin(get("/admin/comments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5));
        mvc.perform(admin(get("/admin/comments")).param("articleId", "1").param("reader", "OLD@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(admin(get("/admin/comments")).param("from", "2023-10-15").param("to", "2023-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.rows[0].fromAdmin").value(true))
                .andExpect(jsonPath("$.data.rows[0].replyToNickname").value("老读者"))
                .andExpect(jsonPath("$.data.rows[0].articleTitle").value("旧文章"));
        mvc.perform(admin(get("/admin/messages")).param("reader", "guest@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        // ---- 博主回复、删除 ----
        mvc.perform(admin(post("/admin/messages/1/reply")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"谢谢留言\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromAdmin").value(true));
        mvc.perform(admin(delete("/admin/comments/1"))).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(2));

        // ---- 仪表盘 ----
        mvc.perform(admin(get("/admin/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleCount").value(2))
                .andExpect(jsonPath("$.data.draftCount").value(1))
                .andExpect(jsonPath("$.data.commentCount").value(3))
                .andExpect(jsonPath("$.data.messageCount").value(4))
                .andExpect(jsonPath("$.data.totalViews").value(7))
                .andExpect(jsonPath("$.data.articlesByCategory[0].name").value("技术"))
                .andExpect(jsonPath("$.data.articlesByCategory[0].count").value(1))
                .andExpect(jsonPath("$.data.articlesByTag[0].count").value(1));

        // 删除评论后读者计数随之变化（拉黑标记查询再跑一次）
        mvc.perform(admin(get("/admin/readers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[?(@.email == 'old@example.com')].commentCount", contains(1)));
    }

    private String collation(String table, String column) {
        return jdbc.sql("SELECT COLLATION_NAME FROM information_schema.COLUMNS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :t AND COLUMN_NAME = :c")
                .param("t", table).param("c", column).query(String.class).single();
    }
}
