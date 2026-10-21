package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 在一份脱敏的旧库结构（db/legacy/legacy_schema.sql，摘自 zaaac_blog.sql）上启动应用：
 * Flyway 应该以 baseline-on-migrate 记基线 1、跳过 V1、执行 V2+，旧表和旧数据保持不变。
 * 需要 Docker，只在 CI 的 mysql-it job 里跑：mvn -B verify -Pmysql-it
 */
@Tag("mysql")
@Testcontainers
@SpringBootTest(properties = "blog.jwt.secret=test-only-not-a-real-secret-0123456789abcdef")
@AutoConfigureMockMvc
class LegacyMigrationMysqlTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withInitScript("db/legacy/legacy_schema.sql");

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MockMvc mvc;

    @Test
    void baselinesLegacySchemaAndAppliesOnlyNewMigrations() {
        List<String> history = jdbc.sql("SELECT CONCAT(version, ':', type) FROM flyway_schema_history ORDER BY installed_rank")
                .query(String.class).list();
        assertThat(history.get(0)).isEqualTo("1:BASELINE");
        assertThat(history).noneMatch(h -> h.equals("1:SQL")).anyMatch(h -> h.startsWith("2:"));

        // V2 新增的列和表
        assertThat(columns("cms_comment")).contains("author_name", "author_email", "author_ip", "user_agent");
        assertThat(columns("cms_message")).contains("author_name", "author_email", "author_ip", "user_agent");
        assertThat(count("reader_block") + count("ip_block") + count("keyword_block")).isZero();

        // 旧列、旧表、旧数据都还在（旧 jar 回滚后能继续用）
        assertThat(columns("sys_user")).contains("dept_id", "phonenumber", "sex", "user_type");
        assertThat(columns("cms_blog")).contains("Top", "blog_files");
        assertThat(count("sys_job")).isEqualTo(1);
        assertThat(count("cms_blog")).isEqualTo(2);
        assertThat(count("cms_comment")).isEqualTo(2);
    }

    @Test
    void publicApiWorksOnLegacyData() throws Exception {
        mvc.perform(get("/public/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[*].id", contains(1)))
                .andExpect(jsonPath("$.data.rows[0].top").value(true))
                .andExpect(jsonPath("$.data.rows[0].categoryName").value("技术"));
        mvc.perform(get("/public/articles/1"))
                .andExpect(jsonPath("$.data.contentHtml").value("<p>hi</p>"));
        mvc.perform(get("/public/articles/1/comments"))
                .andExpect(jsonPath("$.data.rows[0].nickname").value("老读者"))
                .andExpect(jsonPath("$.data.rows[0].children[0].fromAdmin").value(true));
        mvc.perform(post("/public/messages").with(r -> {
                    r.setRemoteAddr("10.9.0.1");
                    return r;
                }).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"访客\",\"email\":\"a@example.com\",\"content\":\"你好\",\"parentId\":1}"))
                .andExpect(status().isOk());
        mvc.perform(post("/public/comments/1/like").with(r -> {
                    r.setRemoteAddr("10.9.0.1");
                    return r;
                }))
                .andExpect(jsonPath("$.data").value(2));
        mvc.perform(get("/public/site")).andExpect(jsonPath("$.data.messageCount").value(2));
    }

    private List<String> columns(String table) {
        return jdbc.sql("SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = :t")
                .param("t", table).query(String.class).list();
    }

    private long count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }
}
