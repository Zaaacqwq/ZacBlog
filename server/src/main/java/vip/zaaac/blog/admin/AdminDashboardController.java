package vip.zaaac.blog.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.Times;

/** 仪表盘：对应旧后台 /cms/chart/total、lineChart、pieChart、tagChart 的数据 */
@RestController
public class AdminDashboardController {

    public record DailyCount(LocalDate date, long articles, long comments, long messages) {
    }

    public record NameCount(String name, long count) {
    }

    public record Dashboard(long articleCount, long essayCount, long draftCount, long commentCount, long messageCount,
            long totalViews, List<DailyCount> last7Days, List<NameCount> articlesByCategory,
            List<NameCount> articlesByTag) {
    }

    private final JdbcClient jdbc;
    private final Times times;

    public AdminDashboardController(JdbcClient jdbc, Times times) {
        this.jdbc = jdbc;
        this.times = times;
    }

    /** articleCount / essayCount 含草稿（与旧版一致），totalViews 是全部文章和随笔的阅读数之和 */
    @GetMapping("/admin/dashboard")
    public ApiResponse<Dashboard> dashboard() {
        LocalDate today = times.now().toLocalDate();
        LocalDate first = today.minusDays(6);
        Map<LocalDate, long[]> daily = new HashMap<>();
        String[] tables = {"cms_blog", "cms_comment", "cms_message"};
        for (int t = 0; t < tables.length; t++) {
            int index = t;
            String filter = t == 0 ? "" : " AND del_flag = '0'";
            jdbc.sql("SELECT create_time FROM " + tables[t] + " WHERE create_time >= :from" + filter)
                    .param("from", first.atStartOfDay())
                    .query(rs -> {
                        LocalDate day = rs.getObject("create_time", java.time.LocalDateTime.class).toLocalDate();
                        daily.computeIfAbsent(day, k -> new long[3])[index]++;
                    });
        }
        List<DailyCount> last7Days = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            long[] c = daily.getOrDefault(day, new long[3]);
            last7Days.add(new DailyCount(day, c[0], c[1], c[2]));
        }
        Dashboard dashboard = jdbc.sql("""
                SELECT
                  (SELECT COUNT(*) FROM cms_blog WHERE type = '1') AS article_count,
                  (SELECT COUNT(*) FROM cms_blog WHERE type = '2') AS essay_count,
                  (SELECT COUNT(*) FROM cms_blog WHERE status = '0') AS draft_count,
                  (SELECT COUNT(*) FROM cms_comment WHERE del_flag = '0') AS comment_count,
                  (SELECT COUNT(*) FROM cms_message WHERE del_flag = '0') AS message_count,
                  (SELECT COALESCE(SUM(views), 0) FROM cms_blog) AS total_views
                """)
                .query((rs, i) -> new Dashboard(rs.getLong("article_count"), rs.getLong("essay_count"),
                        rs.getLong("draft_count"), rs.getLong("comment_count"), rs.getLong("message_count"),
                        rs.getLong("total_views"), last7Days,
                        counts("cms_type", "type_id", "type_name", "cms_blog_type"),
                        counts("cms_tag", "tag_id", "tag_name", "cms_blog_tag")))
                .single();
        return ApiResponse.ok(dashboard);
    }

    private List<NameCount> counts(String table, String idColumn, String nameColumn, String linkTable) {
        return jdbc.sql("SELECT t." + nameColumn + " AS name, COUNT(l.blog_id) AS cnt FROM " + table + " t"
                        + " LEFT JOIN " + linkTable + " l ON l." + idColumn + " = t." + idColumn
                        + " GROUP BY t." + idColumn + ", t." + nameColumn + " ORDER BY t." + idColumn)
                .query((rs, i) -> new NameCount(rs.getString("name"), rs.getLong("cnt")))
                .list();
    }
}
