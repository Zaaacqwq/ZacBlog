package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import vip.zaaac.blog.comment.ReaderService;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;

/**
 * 需要 Docker，只在 CI 的 mysql-it job 里跑：mvn -B verify -Pmysql-it
 */
@Tag("mysql")
@Testcontainers
@SpringBootTest(properties = "blog.jwt.secret=test-only-not-a-real-secret-0123456789abcdef")
class MysqlConnectivityTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ReaderService readers;

    @Test
    void connectsToMysql() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.0");
    }

    /**
     * 空库：V1 按库默认（utf8mb4_0900_ai_ci）建旧表，V11 让黑名单表跟随旧表，
     * 读者列表里 reader_block 与评论邮箱的跨表比较也不能报 Illegal mix of collations。
     */
    @Test
    void freshDatabaseKeepsBlockTablesInLineWithLegacyTables() {
        String legacy = collation("cms_comment", "author_email");
        assertThat(collation("reader_block", "email")).isEqualTo(legacy);
        assertThat(collation("ip_block", "ip")).isEqualTo(legacy);
        assertThat(collation("keyword_block", "keyword")).isEqualTo(legacy);

        jdbc.update("INSERT INTO cms_message (content, type, del_flag, author_name, author_email, create_time)"
                + " VALUES ('hi', '0', '0', '访客', 'Fresh@Example.com', NOW())");
        readers.block("fresh@example.com", null);
        PageResult<ReaderService.Reader> page = readers.list(null, false, true, PageQuery.of(1, 10));
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.rows().get(0).blocked()).isTrue();
        assertThat(page.rows().get(0).messageCount()).isEqualTo(1);
    }

    private String collation(String table, String column) {
        return jdbc.queryForObject("SELECT COLLATION_NAME FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", String.class, table, column);
    }
}
