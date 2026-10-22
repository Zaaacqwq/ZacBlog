package vip.zaaac.blog.comment;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.common.Times;
import vip.zaaac.blog.config.BlogProperties;

/**
 * 读者管理：把评论和留言按读者标识聚合（见 {@link AdminCommentService#READER_KEY}），以及邮箱 / IP / 关键字黑名单。
 * 黑名单直接写表，{@link BlockService} 每次发表都查表，所以拉黑和解除立即生效。
 */
@Service
public class ReaderService {

    /**
     * 每行一条发表记录（评论或留言），博主自己的回复不算读者。
     * nickname 取这一条的昵称：匿名读者用 author_name，旧注册读者用 sys_user.nick_name。
     */
    private static final String POSTS = """
            SELECT %1$s AS reader_key, COALESCE(NULLIF(c.author_name, ''), u.nick_name) AS nickname,
                   c.create_time, c.id, '%2$s' AS kind, c.del_flag
            FROM %3$s c LEFT JOIN sys_user u ON u.user_id = c.user_id
            WHERE c.user_id IS NULL OR c.user_id NOT IN (:adminIds)
            """;

    /**
     * 按读者聚合。拉黑标记不 JOIN reader_block：线上 MySQL 里 reader_block（V2 新表，跟随库默认
     * utf8mb4_0900_ai_ci）和旧表（utf8mb4_general_ci）排序规则不同，两边的列直接比较会报
     * Illegal mix of collations。所以先把黑名单邮箱读出来，作为参数 :blockedEmails 传入（列和参数比较没有这个问题）。
     * 计数只算未删除的；最近时间和最近昵称算上已删除的，这样评论都被删掉的读者仍在列表里，可以拉黑。
     */
    private static final String AGGREGATE = "WITH posts AS (" + POSTS.formatted(AdminCommentService.READER_KEY, "C", "cms_comment")
            + " UNION ALL " + POSTS.formatted(AdminCommentService.READER_KEY, "M", "cms_message") + """
            ), agg AS (
              SELECT reader_key,
                     SUM(CASE WHEN kind = 'C' AND del_flag = '0' THEN 1 ELSE 0 END) AS comment_count,
                     SUM(CASE WHEN kind = 'M' AND del_flag = '0' THEN 1 ELSE 0 END) AS message_count,
                     MAX(create_time) AS last_time
              FROM posts WHERE reader_key IS NOT NULL GROUP BY reader_key
            ), latest AS (
              SELECT reader_key, nickname,
                     ROW_NUMBER() OVER (PARTITION BY reader_key ORDER BY create_time DESC, id DESC, kind) AS rn
              FROM posts WHERE reader_key IS NOT NULL
            ), readers AS (
              SELECT a.reader_key, l.nickname, a.comment_count, a.message_count, a.last_time,
                     CASE WHEN a.reader_key IN (:blockedEmails) THEN 1 ELSE 0 END AS blocked
              FROM agg a JOIN latest l ON l.reader_key = a.reader_key AND l.rn = 1
            )
            """;

    /** email 是读者标识：有邮箱时是小写邮箱，旧注册读者没有邮箱时是 "user:<id>" */
    public record Reader(String email, String nickname, long commentCount, long messageCount,
            OffsetDateTime lastActiveAt, boolean blocked) {
    }

    public record IpBlock(long id, String ip, String reason, OffsetDateTime createdAt) {
    }

    public record KeywordBlock(long id, String keyword, String reason, OffsetDateTime createdAt) {
    }

    private final JdbcClient jdbc;
    private final Times times;
    private final BlogProperties props;

    public ReaderService(JdbcClient jdbc, Times times, BlogProperties props) {
        this.jdbc = jdbc;
        this.times = times;
        this.props = props;
    }

    /** keyword 匹配邮箱或昵称；按最近时间排序，asc 为 true 时正序 */
    public PageResult<Reader> list(String keyword, boolean asc, Boolean blocked, PageQuery page) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        params.put("adminIds", adminIds());
        params.put("blockedEmails", blockedEmails());
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (reader_key LIKE :keyword OR LOWER(nickname) LIKE :keyword)");
            params.put("keyword", "%" + keyword.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (blocked != null) {
            where.append(" AND blocked = :blocked");
            params.put("blocked", blocked ? 1 : 0);
        }
        long total = jdbc.sql(AGGREGATE + "SELECT COUNT(*) FROM readers" + where).params(params).query(Long.class).single();
        params.put("limit", page.pageSize());
        params.put("offset", page.offset());
        String order = asc ? "ASC" : "DESC";
        List<Reader> rows = jdbc.sql(AGGREGATE + "SELECT * FROM readers" + where
                        + " ORDER BY last_time " + order + ", reader_key " + order + " LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> new Reader(rs.getString("reader_key"), rs.getString("nickname"),
                        rs.getLong("comment_count"), rs.getLong("message_count"), times.read(rs, "last_time"),
                        rs.getInt("blocked") == 1))
                .list();
        return new PageResult<>(rows, total);
    }

    @Transactional
    public void block(String email, String reason) {
        String normalized = normalizeEmail(email);
        if (jdbc.sql("SELECT COUNT(*) FROM reader_block WHERE LOWER(email) = :email")
                .param("email", normalized).query(Long.class).single() > 0) {
            return;
        }
        try {
            jdbc.sql("INSERT INTO reader_block (email, reason, create_time) VALUES (:email, :reason, :now)")
                    .param("email", normalized).param("reason", reason).param("now", times.now()).update();
        } catch (DuplicateKeyException e) {
            // 并发拉黑同一邮箱：已经在黑名单里了
        }
    }

    public void unblock(String email) {
        jdbc.sql("DELETE FROM reader_block WHERE LOWER(email) = :email").param("email", normalizeEmail(email)).update();
    }

    public PageResult<IpBlock> ipBlocks(PageQuery page) {
        long total = jdbc.sql("SELECT COUNT(*) FROM ip_block").query(Long.class).single();
        List<IpBlock> rows = jdbc.sql("SELECT id, ip, reason, create_time FROM ip_block ORDER BY id DESC LIMIT :limit OFFSET :offset")
                .param("limit", page.pageSize()).param("offset", page.offset())
                .query((rs, i) -> new IpBlock(rs.getLong("id"), rs.getString("ip"), rs.getString("reason"),
                        times.read(rs, "create_time")))
                .list();
        return new PageResult<>(rows, total);
    }

    public IpBlock addIpBlock(String ip, String reason) {
        long id = insertBlock("ip_block", "ip", ip.strip(), reason, "该 IP 已在黑名单中");
        return new IpBlock(id, ip.strip(), reason, times.toOffset(times.now()));
    }

    public void deleteIpBlock(long id) {
        if (jdbc.sql("DELETE FROM ip_block WHERE id = :id").param("id", id).update() == 0) {
            throw ApiException.notFound();
        }
    }

    public PageResult<KeywordBlock> keywordBlocks(PageQuery page) {
        long total = jdbc.sql("SELECT COUNT(*) FROM keyword_block").query(Long.class).single();
        List<KeywordBlock> rows = jdbc.sql("SELECT id, keyword, reason, create_time FROM keyword_block ORDER BY id DESC LIMIT :limit OFFSET :offset")
                .param("limit", page.pageSize()).param("offset", page.offset())
                .query((rs, i) -> new KeywordBlock(rs.getLong("id"), rs.getString("keyword"), rs.getString("reason"),
                        times.read(rs, "create_time")))
                .list();
        return new PageResult<>(rows, total);
    }

    public KeywordBlock addKeywordBlock(String keyword, String reason) {
        long id = insertBlock("keyword_block", "keyword", keyword.strip(), reason, "该关键字已在黑名单中");
        return new KeywordBlock(id, keyword.strip(), reason, times.toOffset(times.now()));
    }

    public void deleteKeywordBlock(long id) {
        if (jdbc.sql("DELETE FROM keyword_block WHERE id = :id").param("id", id).update() == 0) {
            throw ApiException.notFound();
        }
    }

    private long insertBlock(String table, String column, String value, String reason, String duplicateMsg) {
        if (value.isEmpty()) {
            throw ApiException.badRequest(column + " 不能为空");
        }
        if (jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE LOWER(" + column + ") = :v")
                .param("v", value.toLowerCase(Locale.ROOT)).query(Long.class).single() > 0) {
            throw ApiException.badRequest(duplicateMsg);
        }
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        try {
            jdbc.sql("INSERT INTO " + table + " (" + column + ", reason, create_time) VALUES (:v, :reason, :now)")
                    .param("v", value).param("reason", reason).param("now", times.now())
                    .update(keys, "id");
        } catch (DuplicateKeyException e) {
            throw ApiException.badRequest(duplicateMsg);
        }
        return keys.getKey().longValue();
    }

    /** 小写、去空格后的黑名单邮箱；IN () 不能为空，空时放一个不可能是读者标识的空串 */
    private List<String> blockedEmails() {
        List<String> emails = jdbc.sql("SELECT email FROM reader_block").query(String.class).list().stream()
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        return emails.isEmpty() ? List.of("") : emails;
    }

    private List<Long> adminIds() {
        List<Long> ids = new ArrayList<>(props.adminUserIds());
        // IN () 不能为空
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    /** 读者标识必须是邮箱（旧注册读者的 user:<id> 不能拉黑：没有邮箱，也不能匿名发表） */
    private static String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 128 || !normalized.matches("[^@\\s]+@[^@\\s]+")) {
            throw ApiException.badRequest("邮箱格式不正确");
        }
        return normalized;
    }
}
