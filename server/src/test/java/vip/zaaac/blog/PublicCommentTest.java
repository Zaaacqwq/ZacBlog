package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PublicCommentTest extends IntegrationTestSupport {

    private static String body(String nickname, String email, String content, String extra) {
        return """
                {"nickname":"%s","email":"%s","content":"%s"%s}""".formatted(nickname, email, content,
                extra == null ? "" : "," + extra);
    }

    private ResultActions postComment(String ip, String json) throws Exception {
        return mvc.perform(post("/public/articles/1/comments").with(ip(ip))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }

    @Test
    void treeGroupsLegacyRepliesUnderTheirFloor() throws Exception {
        mvc.perform(get("/public/articles/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].nickname").value("老读者"))
                .andExpect(jsonPath("$.data.rows[0].avatarUrl").value("/profile/avatar/reader.png"))
                .andExpect(jsonPath("$.data.rows[0].likeCount").value(2))
                .andExpect(jsonPath("$.data.rows[0].fromAdmin").value(false))
                .andExpect(jsonPath("$.data.rows[0].children", hasSize(1)))
                .andExpect(jsonPath("$.data.rows[0].children[0].nickname").value("博主"))
                .andExpect(jsonPath("$.data.rows[0].children[0].fromAdmin").value(true))
                .andExpect(jsonPath("$.data.rows[0].children[0].replyToNickname").value("老读者"));
        mvc.perform(get("/public/articles/3/comments")).andExpect(status().isNotFound());
    }

    @Test
    void anonymousCommentIsStoredWithoutExposingEmail() throws Exception {
        postComment("10.2.0.1", body("访客", "Guest@Example.com", "写得好", "\"website\":\"https://guest.example.com\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("访客"))
                .andExpect(jsonPath("$.data.website").value("https://guest.example.com"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.ip").doesNotExist())
                .andExpect(jsonPath("$.data.fromAdmin").value(false));
        var row = jdbc.sql("SELECT author_email, author_ip, type, blog_id FROM cms_comment WHERE content = '写得好'")
                .query().singleRow();
        assertThat(row.get("author_email")).isEqualTo("Guest@Example.com");
        assertThat(row.get("author_ip")).isEqualTo("10.2.0.1");
        assertThat(row.get("type")).isEqualTo("0");
        assertThat(((Number) row.get("blog_id")).longValue()).isEqualTo(1L);
        mvc.perform(get("/public/articles/1/comments"))
                .andExpect(jsonPath("$.data.rows[0].content").value("写得好"))
                .andExpect(jsonPath("$.data.rows[0].email").doesNotExist());
    }

    @Test
    void replyJoinsTheFloorOfItsParent() throws Exception {
        postComment("10.2.0.2", body("访客", "a@example.com", "回复博主", "\"parentId\":2")).andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT main_id FROM cms_comment WHERE content = '回复博主'").query(Long.class).single())
                .isEqualTo(1L);
        mvc.perform(get("/public/articles/1/comments"))
                .andExpect(jsonPath("$.data.rows[0].children", hasSize(2)))
                .andExpect(jsonPath("$.data.rows[0].children[1].replyToNickname").value("博主"));
        postComment("10.2.0.3", body("访客", "a@example.com", "x", "\"parentId\":999")).andExpect(status().isBadRequest());
    }

    @Test
    void commentOnDraftIsNotFoundAndInvalidBodyIsRejected() throws Exception {
        mvc.perform(post("/public/articles/3/comments").with(ip("10.2.0.4"))
                .contentType(MediaType.APPLICATION_JSON).content(body("a", "a@example.com", "x", null)))
                .andExpect(status().isNotFound());
        postComment("10.2.0.5", "{\"nickname\":\"a\",\"content\":\"x\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        postComment("10.2.0.6", body("a", "not-an-email", "x", null)).andExpect(status().isBadRequest());
        postComment("10.2.0.7", body("a", "a@example.com", "x", "\"website\":\"javascript:alert(1)\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void honeypotSubmissionIsSilentlyDropped() throws Exception {
        long before = count("cms_comment");
        postComment("10.2.1.1", body("bot", "bot@example.com", "spam", "\"homepage\":\"http://spam.example\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", nullValue()));
        assertThat(count("cms_comment")).isEqualTo(before);
    }

    @Test
    void secondPostWithinIntervalIsRateLimited() throws Exception {
        postComment("10.2.2.1", body("a", "a@example.com", "第一条", null)).andExpect(status().isOk());
        mvc.perform(post("/public/messages").with(ip("10.2.2.1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("a", "a@example.com", "第二条", null)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(header().exists("Retry-After"));
        postComment("10.2.2.2", body("b", "b@example.com", "别的 IP", null)).andExpect(status().isOk());
    }

    @Test
    void blockedIpEmailAndKeywordAreForbidden() throws Exception {
        jdbc.sql("INSERT INTO ip_block (ip, reason) VALUES ('10.2.3.1', 'spam')").update();
        jdbc.sql("INSERT INTO reader_block (email, reason) VALUES ('bad@example.com', 'spam')").update();
        jdbc.sql("INSERT INTO keyword_block (keyword) VALUES ('Casino')").update();
        long before = count("cms_comment");

        postComment("10.2.3.1", body("a", "a@example.com", "hi", null))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        postComment("10.2.3.2", body("a", "BAD@example.com", "hi", null)).andExpect(status().isForbidden());
        postComment("10.2.3.3", body("a", "a@example.com", "best casino here", null)).andExpect(status().isForbidden());
        mvc.perform(post("/public/messages").with(ip("10.2.3.4"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("casino", "a@example.com", "hi", null)))
                .andExpect(status().isForbidden());

        assertThat(count("cms_comment")).isEqualTo(before);
    }

    @Test
    void anonymousMessageAndReply() throws Exception {
        mvc.perform(post("/public/messages").with(ip("10.2.4.1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("访客", "a@example.com", "留言", "\"parentId\":1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(1));
        mvc.perform(get("/public/messages"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].children[0].content").value("留言"));
    }

    @Test
    void likeIsCountedOncePerIpAndCanBeUndone() throws Exception {
        mvc.perform(post("/public/comments/1/like").with(ip("10.2.5.1"))).andExpect(jsonPath("$.data").value(3));
        mvc.perform(post("/public/comments/1/like").with(ip("10.2.5.1"))).andExpect(jsonPath("$.data").value(3));
        mvc.perform(post("/public/comments/1/like").with(ip("10.2.5.2"))).andExpect(jsonPath("$.data").value(4));
        mvc.perform(delete("/public/comments/1/like").with(ip("10.2.5.1"))).andExpect(jsonPath("$.data").value(3));
        mvc.perform(delete("/public/comments/1/like").with(ip("10.2.5.1"))).andExpect(jsonPath("$.data").value(3));
        mvc.perform(post("/public/messages/1/like").with(ip("10.2.5.1"))).andExpect(jsonPath("$.data").value(1));
        mvc.perform(post("/public/comments/3/like").with(ip("10.2.5.1"))).andExpect(status().isNotFound());
        assertThat(jdbc.sql("SELECT MAX(user_id) FROM cms_comment_like").query(Long.class).single()).isNegative();
    }
}
