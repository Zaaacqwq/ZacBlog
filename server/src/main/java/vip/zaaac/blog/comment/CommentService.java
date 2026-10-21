package vip.zaaac.blog.comment;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.common.RateLimiter;
import vip.zaaac.blog.common.Sql;
import vip.zaaac.blog.common.Times;
import vip.zaaac.blog.config.BlogProperties;
import vip.zaaac.blog.content.ArticleService;

/**
 * 评论和留言：读取、匿名发表、点赞。
 * 旧库约定：type '0' 顶层 / '1' 回复；parent_id 是被回复的那条；main_id 是所在楼层（顶层）的 id；
 * del_flag '0' 正常；旧评论通过 user_id 关联 sys_user 取昵称和头像，新评论用 author_* 列。
 */
@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    static final String BLOCKED_MESSAGE = "提交失败，请稍后再试或联系博主";

    private static final String SELECT = """
            SELECT c.id, c.parent_id, c.like_num, c.content, c.user_id, c.author_name, c.author_website,
                   c.create_time, u.nick_name, u.avatar
            FROM %s c LEFT JOIN sys_user u ON u.user_id = c.user_id
            """;

    public record PostRequest(Long parentId, String nickname, String email, String website, String content,
            String honeypot) {
    }

    private final JdbcClient jdbc;
    private final Times times;
    private final BlogProperties props;
    private final RateLimiter rateLimiter;
    private final BlockService blocks;
    private final ArticleService articles;

    public CommentService(JdbcClient jdbc, Times times, BlogProperties props, RateLimiter rateLimiter,
            BlockService blocks, ArticleService articles) {
        this.jdbc = jdbc;
        this.times = times;
        this.props = props;
        this.rateLimiter = rateLimiter;
        this.blocks = blocks;
        this.articles = articles;
    }

    /** 分页的是顶层楼层；每层带上该楼所有回复 */
    public PageResult<CommentDto> tree(Board board, Long articleId, PageQuery page) {
        if (board == Board.COMMENT && !articles.isPublished(articleId)) {
            throw ApiException.notFound();
        }
        String scope = board == Board.COMMENT ? " AND c.blog_id = :articleId" : "";
        Map<String, Object> params = new HashMap<>();
        if (articleId != null && board == Board.COMMENT) {
            params.put("articleId", articleId);
        }
        long total = jdbc.sql("SELECT COUNT(*) FROM " + board.table + " c WHERE c.del_flag = '0' AND c.parent_id IS NULL" + scope)
                .params(params).query(Long.class).single();
        params.put("limit", page.pageSize());
        params.put("offset", page.offset());
        List<CommentDto> tops = jdbc.sql(SELECT.formatted(board.table)
                        + " WHERE c.del_flag = '0' AND c.parent_id IS NULL" + scope
                        + " ORDER BY c.create_time DESC, c.id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> CommentDto.withChildren(map(rs)))
                .list();
        if (tops.isEmpty()) {
            return new PageResult<>(tops, total);
        }
        params.remove("limit");
        params.remove("offset");
        // 回复数量不大：取出范围内全部回复，沿 parent_id 找到所在楼层（不依赖旧数据的 main_id 是否完整）
        List<CommentDto> replies = jdbc.sql(SELECT.formatted(board.table)
                        + " WHERE c.del_flag = '0' AND c.parent_id IS NOT NULL" + scope
                        + " ORDER BY c.create_time ASC, c.id ASC")
                .params(params)
                .query((rs, i) -> map(rs))
                .list();
        Map<Long, CommentDto> byId = new HashMap<>();
        Map<Long, CommentDto> topById = new LinkedHashMap<>();
        tops.forEach(t -> topById.put(t.id(), t));
        tops.forEach(t -> byId.put(t.id(), t));
        replies.forEach(r -> byId.put(r.id(), r));
        for (CommentDto reply : replies) {
            Long rootId = findRoot(reply, byId);
            CommentDto top = rootId == null ? null : topById.get(rootId);
            if (top != null) {
                CommentDto parent = byId.get(reply.parentId());
                top.children().add(reply.withReplyTo(parent == null ? null : parent.nickname()));
            }
        }
        return new PageResult<>(tops, total);
    }

    private static Long findRoot(CommentDto c, Map<Long, CommentDto> byId) {
        Set<Long> seen = new HashSet<>();
        CommentDto cur = c;
        while (cur.parentId() != null) {
            if (!seen.add(cur.id())) {
                return null;
            }
            cur = byId.get(cur.parentId());
            if (cur == null) {
                return null;
            }
        }
        return cur.id();
    }

    /**
     * 匿名发表。蜜罐字段非空时不入库、返回 null（接口照常返回成功）。
     * 顺序：蜜罐 → 文章是否存在 → 黑名单（403）→ 限流（429）→ 回复对象校验 → 入库。
     */
    @Transactional
    public CommentDto post(Board board, Long articleId, PostRequest req, String ip, String userAgent) {
        if (req.honeypot() != null && !req.honeypot().isEmpty()) {
            log.info("Honeypot triggered on {} from {}", board, ip);
            return null;
        }
        if (board == Board.COMMENT && !articles.isPublished(articleId)) {
            throw ApiException.notFound();
        }
        String nickname = req.nickname().strip();
        String content = req.content().strip();
        if (nickname.isEmpty() || content.isEmpty()) {
            throw ApiException.badRequest("昵称和内容不能为空");
        }
        if (blocks.isBlocked(req.email(), ip, content, nickname)) {
            throw new ApiException(HttpStatus.FORBIDDEN, BLOCKED_MESSAGE);
        }
        BlogProperties.RateLimit limits = props.rateLimit();
        long wait = rateLimiter.tryAcquire(List.of(
                new RateLimiter.Rule("post-interval:" + ip, 1, limits.postInterval()),
                new RateLimiter.Rule("post-day:" + ip, limits.postsPerDay(), Duration.ofDays(1))));
        if (wait > 0) {
            throw ApiException.tooManyRequests(wait);
        }
        Long mainId = req.parentId() == null ? null : floorOf(board, articleId, req.parentId());
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO " + board.table + """
                 (parent_id, main_id, like_num, content, type, blog_id, del_flag, create_by, create_time,
                  author_name, author_email, author_website, author_ip, user_agent)
                VALUES (:parentId, :mainId, 0, :content, :type, :blogId, '0', '', :now,
                  :name, :email, :website, :ip, :ua)
                """)
                .param("parentId", req.parentId())
                .param("mainId", mainId)
                .param("content", content)
                .param("type", req.parentId() == null ? "0" : "1")
                .param("blogId", board == Board.COMMENT ? articleId : null)
                .param("now", times.now())
                .param("name", nickname)
                .param("email", req.email().trim())
                .param("website", Sql.emptyToNull(req.website()))
                .param("ip", ip)
                .param("ua", truncate(userAgent, 255))
                .update(keys, "id");
        long id = keys.getKey().longValue();
        return jdbc.sql(SELECT.formatted(board.table) + " WHERE c.id = :id")
                .param("id", id)
                .query((rs, i) -> CommentDto.withChildren(map(rs)))
                .single();
    }

    /** 被回复那条所在楼层的 id；旧数据的 main_id 可能为空，沿 parent_id 往上找 */
    private long floorOf(Board board, Long articleId, long parentId) {
        String sql = "SELECT parent_id, main_id FROM " + board.table + " WHERE id = :id AND del_flag = '0'"
                + (board == Board.COMMENT ? " AND blog_id = :articleId" : "");
        Map<String, Object> params = new HashMap<>();
        if (board == Board.COMMENT) {
            params.put("articleId", articleId);
        }
        long current = parentId;
        for (int depth = 0; depth < 100; depth++) {
            params.put("id", current);
            Long[] row = jdbc.sql(sql).params(params)
                    .query((rs, i) -> new Long[] {(Long) rs.getObject("parent_id", Long.class),
                            (Long) rs.getObject("main_id", Long.class)})
                    .optional()
                    .orElseThrow(() -> ApiException.badRequest("回复的评论不存在"));
            if (row[1] != null) {
                return row[1];
            }
            if (row[0] == null) {
                return current;
            }
            current = row[0];
        }
        throw ApiException.badRequest("回复的评论不存在");
    }

    /** 点赞 / 取消点赞，同一 IP 只计一次。返回最新点赞数 */
    @Transactional
    public long like(Board board, long id, String ip, boolean like) {
        boolean exists = jdbc.sql("SELECT COUNT(*) FROM " + board.table + " WHERE id = :id AND del_flag = '0'")
                .param("id", id).query(Long.class).single() > 0;
        if (!exists) {
            throw ApiException.notFound();
        }
        long voter = voterId(ip);
        int changed;
        if (like) {
            try {
                changed = jdbc.sql("INSERT INTO " + board.likeTable + " (" + board.likeColumn
                                + ", user_id, create_by, create_time, voter_ip) VALUES (:id, :voter, '', :now, :ip)")
                        .param("id", id).param("voter", voter).param("now", times.now()).param("ip", ip)
                        .update();
            } catch (DuplicateKeyException e) {
                changed = 0;
            }
            if (changed > 0) {
                jdbc.sql("UPDATE " + board.table + " SET like_num = COALESCE(like_num, 0) + 1 WHERE id = :id")
                        .param("id", id).update();
            }
        } else {
            changed = jdbc.sql("DELETE FROM " + board.likeTable + " WHERE " + board.likeColumn + " = :id AND user_id = :voter")
                    .param("id", id).param("voter", voter).update();
            if (changed > 0) {
                jdbc.sql("UPDATE " + board.table
                                + " SET like_num = CASE WHEN COALESCE(like_num, 0) > 0 THEN like_num - 1 ELSE 0 END WHERE id = :id")
                        .param("id", id).update();
            }
        }
        return jdbc.sql("SELECT COALESCE(like_num, 0) FROM " + board.table + " WHERE id = :id")
                .param("id", id).query(Long.class).single();
    }

    /**
     * 匿名点赞者标识：由 IP 推出的负数，写进旧 like 表的 user_id（主键是 (评论id, user_id)），
     * 不会和真实用户 id（正数）冲突。
     */
    static long voterId(String ip) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(("ip:" + ip).getBytes(StandardCharsets.UTF_8));
            long positive = ByteBuffer.wrap(hash).getLong() & Long.MAX_VALUE;
            return -(positive % (Long.MAX_VALUE - 1)) - 1;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private CommentDto map(ResultSet rs) throws SQLException {
        long userId = rs.getLong("user_id");
        boolean hasUser = !rs.wasNull() && userId > 0;
        String nickname = Sql.emptyToNull(rs.getString("author_name"));
        if (nickname == null) {
            nickname = Sql.emptyToNull(rs.getString("nick_name"));
        }
        long parentId = rs.getLong("parent_id");
        Long parent = rs.wasNull() ? null : parentId;
        return new CommentDto(
                rs.getLong("id"),
                parent,
                nickname == null ? "匿名" : nickname,
                Sql.emptyToNull(rs.getString("author_website")),
                hasUser ? Sql.emptyToNull(rs.getString("avatar")) : null,
                rs.getString("content"),
                rs.getLong("like_num"),
                hasUser && props.adminUserIds().contains(userId),
                null,
                times.read(rs, "create_time"),
                List.of());
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
