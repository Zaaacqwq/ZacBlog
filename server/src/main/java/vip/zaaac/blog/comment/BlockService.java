package vip.zaaac.blog.comment;

import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** 黑名单：reader_block（邮箱）、ip_block、keyword_block（内容或昵称包含即命中，不区分大小写） */
@Service
public class BlockService {

    private final JdbcClient jdbc;

    public BlockService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isBlocked(String email, String ip, String... texts) {
        if (exists("SELECT COUNT(*) FROM ip_block WHERE ip = :v", ip)) {
            return true;
        }
        if (email != null && exists("SELECT COUNT(*) FROM reader_block WHERE LOWER(email) = :v",
                email.trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        List<String> keywords = jdbc.sql("SELECT keyword FROM keyword_block").query(String.class).list();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            String lower = text.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank() && lower.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean exists(String sql, String value) {
        return jdbc.sql(sql).param("v", value).query(Long.class).single() > 0;
    }
}
