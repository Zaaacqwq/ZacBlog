package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** 后台：评论/留言审核、读者聚合、邮箱 / IP / 关键字黑名单 */
class AdminReaderTest extends IntegrationTestSupport {

    private ResultActions postComment(String ip, String nickname, String email, String content) throws Exception {
        return mvc.perform(post("/public/articles/1/comments").with(ip(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"%s\",\"email\":\"%s\",\"content\":\"%s\"}".formatted(nickname, email, content)));
    }

    /** 匿名读者（同一邮箱大小写不同、多次发表）+ 有邮箱的旧注册读者 + 没邮箱的旧注册读者 + 博主回复 */
    private void seedMixedReaders() {
        jdbc.sql("UPDATE sys_user SET email = 'Old@Example.com' WHERE user_id = 4").update();
        jdbc.sql("INSERT INTO sys_user (user_id, user_name, nick_name, email) VALUES (5, 'nomail', '无邮箱读者', '')").update();
        jdbc.sql("""
                INSERT INTO cms_comment (id, parent_id, content, type, blog_id, del_flag, user_id, author_name,
                  author_email, author_ip, create_time) VALUES
                  (11, NULL, '匿名1', '0', 1, '0', NULL, '访客', 'Guest@Example.com', '10.7.0.1', '2024-04-01 10:00:00'),
                  (12, NULL, '匿名2', '0', 2, '0', NULL, '新昵称', 'guest@example.com ', '10.7.0.2', '2024-04-03 10:00:00'),
                  (13, NULL, '无邮箱', '0', 1, '0', 5, NULL, NULL, NULL, '2023-12-01 10:00:00'),
                  (14, NULL, '被删的', '0', 1, '1', NULL, '访客', 'guest@example.com', '10.7.0.3', '2024-04-04 10:00:00')
                """).update();
        jdbc.sql("""
                INSERT INTO cms_message (id, parent_id, content, type, del_flag, user_id, author_name, author_email,
                  author_ip, create_time) VALUES
                  (11, NULL, '匿名留言', '0', '0', NULL, '旧昵称', 'GUEST@example.com', '10.7.0.4', '2024-04-02 10:00:00'),
                  (12, 11, '博主回复', '1', '0', 1, NULL, NULL, NULL, '2024-04-05 10:00:00')
                """).update();
    }

    @Test
    void readersAreAggregatedByEmailIncludingLegacyUsers() throws Exception {
        seedMixedReaders();
        mvc.perform(get("/admin/readers").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                // 最近时间倒序：guest（含已删除那条 04-04）→ old（03-01）→ user:5（2023-12-01）
                .andExpect(jsonPath("$.data.rows[0].email").value("guest@example.com"))
                .andExpect(jsonPath("$.data.rows[0].nickname").value("访客"))
                .andExpect(jsonPath("$.data.rows[0].commentCount").value(2))
                .andExpect(jsonPath("$.data.rows[0].messageCount").value(1))
                .andExpect(jsonPath("$.data.rows[0].lastActiveAt").value("2024-04-04T10:00:00+08:00"))
                .andExpect(jsonPath("$.data.rows[0].blocked").value(false))
                .andExpect(jsonPath("$.data.rows[1].email").value("old@example.com"))
                .andExpect(jsonPath("$.data.rows[1].nickname").value("老读者"))
                .andExpect(jsonPath("$.data.rows[1].commentCount").value(1))
                .andExpect(jsonPath("$.data.rows[1].messageCount").value(1))
                .andExpect(jsonPath("$.data.rows[2].email").value("user:5"))
                .andExpect(jsonPath("$.data.rows[2].nickname").value("无邮箱读者"))
                .andExpect(jsonPath("$.data.rows[2].commentCount").value(1))
                .andExpect(jsonPath("$.data.rows[2].messageCount").value(0));

        mvc.perform(get("/admin/readers").with(admin()).param("order", "asc").param("pageSize", "1").param("pageNum", "1"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.rows", hasSize(1)))
                .andExpect(jsonPath("$.data.rows[0].email").value("user:5"));
        mvc.perform(get("/admin/readers").with(admin()).param("keyword", "老读"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].email").value("old@example.com"));
        mvc.perform(get("/admin/readers").with(admin()).param("keyword", "GUEST@"))
                .andExpect(jsonPath("$.data.total").value(1));

        mvc.perform(post("/admin/readers/Guest@Example.com/block").with(admin())).andExpect(status().isOk());
        mvc.perform(get("/admin/readers").with(admin()).param("blocked", "true"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].blocked").value(true));
        mvc.perform(post("/admin/readers/user:5/block").with(admin())).andExpect(status().isBadRequest());
    }

    @Test
    void commentListFiltersByArticleReaderAndTime() throws Exception {
        seedMixedReaders();
        mvc.perform(get("/admin/comments").with(admin()))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.rows[0].email").value("guest@example.com "))
                .andExpect(jsonPath("$.data.rows[0].ip").value("10.7.0.2"))
                .andExpect(jsonPath("$.data.rows[0].articleTitle").value("置顶文章"));
        mvc.perform(get("/admin/comments").with(admin()).param("articleId", "1"))
                .andExpect(jsonPath("$.data.total").value(4));
        mvc.perform(get("/admin/comments").with(admin()).param("reader", "GUEST@example.com"))
                .andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/admin/comments").with(admin()).param("reader", "old@example.com"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].nickname").value("老读者"));
        mvc.perform(get("/admin/comments").with(admin()).param("reader", "user:5"))
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/admin/comments").with(admin()).param("from", "2024-04-01").param("to", "2024-04-02"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].content").value("匿名1"));
        mvc.perform(get("/admin/messages").with(admin()))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.rows[0].fromAdmin").value(true))
                .andExpect(jsonPath("$.data.rows[0].replyToNickname").value("旧昵称"));
    }

    @Test
    void adminReplyAndSoftDelete() throws Exception {
        String json = mvc.perform(post("/admin/comments/1/reply").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"谢谢\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromAdmin").value(true))
                .andExpect(jsonPath("$.data.nickname").value("博主"))
                .andExpect(jsonPath("$.data.articleId").value(1))
                .andReturn().getResponse().getContentAsString();
        int replyId = JsonPath.read(json, "$.data.id");
        assertThat(jdbc.sql("SELECT main_id FROM cms_comment WHERE id = :id").param("id", replyId)
                .query(Long.class).single()).isEqualTo(1L);
        mvc.perform(get("/public/articles/1/comments"))
                .andExpect(jsonPath("$.data.rows[0].children", hasSize(2)))
                .andExpect(jsonPath("$.data.rows[0].children[1].fromAdmin").value(true));
        mvc.perform(post("/admin/comments/3/reply").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"x\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/admin/messages/1/reply").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"留言回复\"}"))
                .andExpect(status().isOk());

        // 删除顶层评论：连同两条回复一起软删除
        mvc.perform(delete("/admin/comments/1").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(3));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM cms_comment WHERE del_flag = '1'").query(Long.class).single())
                .isEqualTo(4L);
        mvc.perform(get("/public/articles/1/comments")).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(delete("/admin/comments/1").with(admin())).andExpect(status().isNotFound());
        mvc.perform(delete("/admin/messages/1").with(admin())).andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void emailBlockTakesEffectImmediatelyAndCanBeLifted() throws Exception {
        postComment("10.8.0.1", "访客", "fan@example.com", "第一条").andExpect(status().isOk());
        mvc.perform(post("/admin/readers/FAN@example.com/block").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"spam\"}"))
                .andExpect(status().isOk());
        // 重复拉黑是幂等的
        mvc.perform(post("/admin/readers/fan@example.com/block").with(admin())).andExpect(status().isOk());
        postComment("10.8.0.2", "访客", "Fan@Example.com", "第二条").andExpect(status().isForbidden());
        mvc.perform(get("/admin/readers").with(admin()))
                .andExpect(jsonPath("$.data.rows[0].email").value("fan@example.com"))
                .andExpect(jsonPath("$.data.rows[0].blocked").value(true));

        mvc.perform(delete("/admin/readers/fan@example.com/block").with(admin())).andExpect(status().isOk());
        postComment("10.8.0.3", "访客", "fan@example.com", "第三条").andExpect(status().isOk());
        mvc.perform(get("/admin/readers").with(admin()))
                .andExpect(jsonPath("$.data.rows[0].blocked").value(false))
                .andExpect(jsonPath("$.data.rows[0].commentCount").value(2));
    }

    @Test
    void ipBlockTakesEffectImmediatelyAndCanBeLifted() throws Exception {
        String json = mvc.perform(post("/admin/readers/ip-blocks").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ip\":\"10.8.1.1\",\"reason\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ip").value("10.8.1.1"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.data.id");
        mvc.perform(post("/admin/readers/ip-blocks").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ip\":\"10.8.1.1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/readers/ip-blocks").with(admin()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].reason").value("spam"));
        postComment("10.8.1.1", "访客", "a@example.com", "hi").andExpect(status().isForbidden());

        mvc.perform(delete("/admin/readers/ip-blocks/" + id).with(admin())).andExpect(status().isOk());
        mvc.perform(delete("/admin/readers/ip-blocks/" + id).with(admin())).andExpect(status().isNotFound());
        postComment("10.8.1.1", "访客", "a@example.com", "hi").andExpect(status().isOk());
    }

    @Test
    void keywordBlockTakesEffectImmediatelyAndCanBeLifted() throws Exception {
        String json = mvc.perform(post("/admin/readers/keyword-blocks").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"keyword\":\"Casino\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.data.id");
        mvc.perform(get("/admin/readers/keyword-blocks").with(admin()))
                .andExpect(jsonPath("$.data.rows[0].keyword").value("Casino"));
        mvc.perform(post("/public/messages").with(ip("10.8.2.1")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"a\",\"email\":\"a@example.com\",\"content\":\"best casino\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/admin/readers/keyword-blocks/" + id).with(admin())).andExpect(status().isOk());
        mvc.perform(post("/public/messages").with(ip("10.8.2.2")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"a\",\"email\":\"a@example.com\",\"content\":\"best casino\"}"))
                .andExpect(status().isOk());
    }
}
