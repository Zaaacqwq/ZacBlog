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

    @Test
    void connectsToMysql() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.0");
    }
}
