package vip.zaaac.blog.comment;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import vip.zaaac.blog.config.BlogProperties;

/**
 * 后台评论/留言审核：平铺列表（按文章、读者、时间筛选）、软删除（del_flag '1'，连同所有子回复）、以博主身份回复。
 * 读者标识（reader）与 /admin/readers 一致：有邮箱用小写邮箱，旧注册读者没有邮箱时用 "user:<user_id>"。
 */
@Service
public class AdminCommentService {

    /** 一条评论/留言的读者标识：匿名读者的 author_email → 旧注册读者 sys_user.email → user:<id> */
    static final String READER_KEY = """
            CASE WHEN c.author_email IS NOT NULL AND TRIM(c.author_email) <> '' THEN LOWER(TRIM(c.author_email))
                 WHEN u.email IS NOT NULL AND TRIM(u.email) <> '' THEN LOWER(TRIM(u.email))
                 WHEN c.user_id IS NOT NULL THEN CONCAT('user:', c.user_id)
                 ELSE NULL END""";

    public enum Kind {
        COMMENT(Board.COMMENT), MESSAGE(Board.MESSAGE);

        final Board board;

        Kind(Board board) {
            this.board = board;
        }
    }

    public record Filter(Long articleId, String reader, String keyword, LocalDate from, LocalDate to) {
    }

    /** docs/api.md 的 Comment 加上后台字段；后台列表是平铺的，没有 children */
    public record AdminComment(long id, Long parentId, String nickname, String website, String avatarUrl,
            String content, long likeCount, boolean fromAdmin, String replyToNickname, OffsetDateTime createdAt,
            String email, String reader, String ip, Long articleId, String articleTitle) {
    }

    private final JdbcClient jdbc;
    private final Times times;
    private final BlogProperties props;

    public AdminCommentService(JdbcClient jdbc, Times times, BlogProperties props) {
        this.jdbc = jdbc;
        this.times = times;
        this.props = props;
    }

    public PageResult<AdminComment> list(Kind kind, Filter filter, PageQuery page) {
        return list(kind, filter, page, null);
    }

    public AdminComment get(Kind kind, long id) {
        return list(kind, new Filter(null, null, null, null, null), new PageQuery(1, 1), id).rows().stream()
                .findFirst().orElseThrow(ApiException::notFound);
    }

    private PageResult<AdminComment> list(Kind kind, Filter filter, PageQuery page, Long onlyId) {
        Board board = kind.board;
        StringBuilder where = new StringBuilder(" WHERE c.del_flag = '0'");
        Map<String, Object> params = new HashMap<>();
        if (onlyId != null) {
            where.append(" AND c.id = :onlyId");
            params.put("onlyId", onlyId);
        }
        if (board == Board.COMMENT && filter.articleId() != null) {
            where.append(" AND c.blog_id = :articleId");
            params.put("articleId", filter.articleId());
        }
        if (filter.reader() != null && !filter.reader().isBlank()) {
            where.append(" AND (").append(READER_KEY).append(") = :reader");
            params.put("reader", filter.reader().trim().toLowerCase(Locale.ROOT));
        }
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            where.append(" AND LOWER(c.content) LIKE :keyword");
            params.put("keyword", "%" + filter.keyword().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (filter.from() != null) {
            where.append(" AND c.create_time >= :from");
            params.put("from", filter.from().atStartOfDay());
        }
        if (filter.to() != null) {
            where.append(" AND c.create_time < :to");
            params.put("to", filter.to().plusDays(1).atStartOfDay());
        }
        String from = " FROM " + board.table + " c LEFT JOIN sys_user u ON u.user_id = c.user_id"
                + (board == Board.COMMENT ? " LEFT JOIN cms_blog b ON b.id = c.blog_id" : "")
                + " LEFT JOIN " + board.table + " p ON p.id = c.parent_id"
                + " LEFT JOIN sys_user pu ON pu.user_id = p.user_id";
        long total = jdbc.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();
        params.put("limit", page.pageSize());
        params.put("offset", page.offset());
        List<AdminComment> rows = jdbc.sql("SELECT c.id, c.parent_id, c.like_num, c.content, c.user_id, c.author_name,"
                        + " c.author_email, c.author_website, c.author_ip, c.blog_id, c.create_time, u.nick_name, u.avatar,"
                        + " u.email AS user_email, (" + READER_KEY + ") AS reader_key,"
                        + " COALESCE(NULLIF(p.author_name, ''), pu.nick_name) AS parent_name,"
                        + (board == Board.COMMENT ? " b.title" : " NULL") + " AS article_title"
                        + from + where + " ORDER BY c.create_time DESC, c.id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> {
                    long userId = rs.getLong("user_id");
                    boolean hasUser = !rs.wasNull() && userId > 0;
                    String nickname = Sql.emptyToNull(rs.getString("author_name"));
                    if (nickname == null) {
                        nickname = Sql.emptyToNull(rs.getString("nick_name"));
                    }
                    long parentId = rs.getLong("parent_id");
                    Long parent = rs.wasNull() ? null : parentId;
                    long blogId = rs.getLong("blog_id");
                    Long articleId = rs.wasNull() ? null : blogId;
                    String email = Sql.emptyToNull(rs.getString("author_email"));
                    if (email == null) {
                        email = Sql.emptyToNull(rs.getString("user_email"));
                    }
                    return new AdminComment(rs.getLong("id"), parent, nickname == null ? "匿名" : nickname,
                            Sql.emptyToNull(rs.getString("author_website")),
                            hasUser ? Sql.emptyToNull(rs.getString("avatar")) : null,
                            rs.getString("content"), rs.getLong("like_num"),
                            hasUser && props.adminUserIds().contains(userId),
                            parent == null ? null : rs.getString("parent_name"),
                            times.read(rs, "create_time"), email, rs.getString("reader_key"),
                            rs.getString("author_ip"), articleId, rs.getString("article_title"));
                })
                .list();
        return new PageResult<>(rows, total);
    }

    /** 以博主身份回复：user_id 记当前管理员，昵称和头像取 sys_user，前台显示为博主回复 */
    @Transactional
    public AdminComment reply(Kind kind, long parentId, String content, long adminId, String username) {
        Board board = kind.board;
        Map<String, Object> parent = jdbc.sql("SELECT id, parent_id, main_id, blog_id FROM " + board.table
                        + " WHERE id = :id AND del_flag = '0'")
                .param("id", parentId).query().listOfRows().stream().findFirst()
                .orElseThrow(ApiException::notFound);
        Long mainId = floorOf(board, parentId);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO " + board.table + """
                 (parent_id, main_id, like_num, content, type, blog_id, del_flag, user_id, create_by, create_time)
                VALUES (:parentId, :mainId, 0, :content, '1', :blogId, '0', :userId, :user, :now)
                """)
                .param("parentId", parentId)
                .param("mainId", mainId)
                .param("content", content.strip())
                .param("blogId", parent.get("blog_id"))
                .param("userId", adminId)
                .param("user", username)
                .param("now", times.now())
                .update(keys, "id");
        return get(kind, keys.getKey().longValue());
    }

    /** 软删除这一条和它下面的所有回复（沿 parent_id 向下找），返回删除的条数 */
    @Transactional
    public int delete(Kind kind, long id) {
        Board board = kind.board;
        if (jdbc.sql("SELECT COUNT(*) FROM " + board.table + " WHERE id = :id AND del_flag = '0'")
                .param("id", id).query(Long.class).single() == 0) {
            throw ApiException.notFound();
        }
        Set<Long> all = new LinkedHashSet<>();
        List<Long> frontier = List.of(id);
        while (!frontier.isEmpty() && all.size() < 100_000) {
            all.addAll(frontier);
            List<Long> next = new ArrayList<>();
            for (Long child : jdbc.sql("SELECT id FROM " + board.table + " WHERE parent_id IN (:ids)")
                    .param("ids", frontier).query(Long.class).list()) {
                if (!all.contains(child)) {
                    next.add(child);
                }
            }
            frontier = next;
        }
        return jdbc.sql("UPDATE " + board.table + " SET del_flag = '1' WHERE id IN (:ids) AND del_flag = '0'")
                .param("ids", all).update();
    }

    /** 所在楼层（顶层）的 id；旧数据的 main_id 可能为空，沿 parent_id 往上找 */
    private long floorOf(Board board, long id) {
        long current = id;
        for (int depth = 0; depth < 100; depth++) {
            Map<String, Object> row = jdbc.sql("SELECT parent_id, main_id FROM " + board.table + " WHERE id = :id")
                    .param("id", current).query().listOfRows().stream().findFirst().orElse(null);
            if (row == null) {
                return current;
            }
            if (row.get("main_id") != null) {
                return ((Number) row.get("main_id")).longValue();
            }
            if (row.get("parent_id") == null) {
                return current;
            }
            current = ((Number) row.get("parent_id")).longValue();
        }
        return current;
    }
}
