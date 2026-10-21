package vip.zaaac.blog.common;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Sql {

    private Sql() {
    }

    /** 旧库的 content、notice_content 等是 longblob，按 UTF-8 文本读取 */
    public static String blobText(ResultSet rs, String column) throws SQLException {
        byte[] bytes = rs.getBytes(column);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    public static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
