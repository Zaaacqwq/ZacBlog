package vip.zaaac.blog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class PublicContentTest extends IntegrationTestSupport {

    @Test
    void listReturnsOnlyPublishedArticlesWithTopFirst() throws Exception {
        mvc.perform(get("/public/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.rows[*].id", contains(2, 4, 1)))
                .andExpect(jsonPath("$.data.rows[0].top").value(true))
                .andExpect(jsonPath("$.data.rows[0].coverUrl").value("https://example.com/c.png"))
                .andExpect(jsonPath("$.data.rows[0].contentHtml").doesNotExist())
                .andExpect(jsonPath("$.data.rows[2].coverUrl").value("/profile/upload/2023/10/13/a.png"))
                .andExpect(jsonPath("$.data.rows[2].categoryName").value("技术"))
                .andExpect(jsonPath("$.data.rows[2].tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.rows[2].commentCount").value(2))
                .andExpect(jsonPath("$.data.rows[2].createdAt").value("2024-01-01T10:00:00+08:00"));
    }

    @Test
    void listFiltersByCategoryTagEssayAndPages() throws Exception {
        mvc.perform(get("/public/articles").param("categoryId", "1"))
                .andExpect(jsonPath("$.data.rows[*].id", contains(1)));
        mvc.perform(get("/public/articles").param("tagId", "1"))
                .andExpect(jsonPath("$.data.rows[*].id", contains(1)));
        mvc.perform(get("/public/articles").param("essay", "true"))
                .andExpect(jsonPath("$.data.rows[*].id", contains(4)));
        mvc.perform(get("/public/articles").param("essay", "false"))
                .andExpect(jsonPath("$.data.rows[*].id", contains(2, 1)));
        mvc.perform(get("/public/articles").param("keyword", "随笔"))
                .andExpect(jsonPath("$.data.rows[*].id", contains(4)));
        mvc.perform(get("/public/articles").param("pageNum", "2").param("pageSize", "2"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.rows[*].id", contains(1)));
    }

    @Test
    void recommendOrdersByViewsAndSkipsDrafts() throws Exception {
        mvc.perform(get("/public/articles/recommend").param("limit", "2"))
                .andExpect(jsonPath("$.data[*].id", contains(2, 1)));
    }

    @Test
    void detailIncludesContentAndFilesAndHidesDrafts() throws Exception {
        mvc.perform(get("/public/articles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentHtml").value("<p>hi</p>"))
                .andExpect(jsonPath("$.data.contentMarkdown").value("# hi "))
                .andExpect(jsonPath("$.data.files[0].name").value("slides.pdf"))
                .andExpect(jsonPath("$.data.files[0].url").value("/profile/upload/2023/10/13/x.pdf"))
                .andExpect(jsonPath("$.data.files[0].size").value(1572864));
        mvc.perform(get("/public/articles/1/files")).andExpect(jsonPath("$.data", hasSize(1)));
        mvc.perform(get("/public/articles/3")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(get("/public/articles/3/files")).andExpect(status().isNotFound());
        mvc.perform(get("/public/articles/999")).andExpect(status().isNotFound());
    }

    @Test
    void viewsIncrementOncePerIpWithinWindow() throws Exception {
        mvc.perform(post("/public/articles/1/views").with(ip("10.1.0.1"))).andExpect(jsonPath("$.data").value(4));
        mvc.perform(post("/public/articles/1/views").with(ip("10.1.0.1"))).andExpect(jsonPath("$.data").value(4));
        mvc.perform(post("/public/articles/1/views").with(ip("10.1.0.2"))).andExpect(jsonPath("$.data").value(5));
        mvc.perform(post("/public/articles/3/views").with(ip("10.1.0.1"))).andExpect(status().isNotFound());
    }

    @Test
    void taxonomiesNoticesSiteAndAbout() throws Exception {
        mvc.perform(get("/public/categories"))
                .andExpect(jsonPath("$.data[0].name").value("技术"))
                .andExpect(jsonPath("$.data[0].articleCount").value(1));
        mvc.perform(get("/public/tags"))
                .andExpect(jsonPath("$.data[0].articleCount").value(1))
                .andExpect(jsonPath("$.data[1].articleCount").value(1));
        mvc.perform(get("/public/archives"))
                .andExpect(jsonPath("$.data[0].year").value(2024))
                .andExpect(jsonPath("$.data[0].month").value(6))
                .andExpect(jsonPath("$.data", hasSize(3)));
        mvc.perform(get("/public/notices/latest"))
                .andExpect(jsonPath("$.data.title").value("新公告"))
                .andExpect(jsonPath("$.data.content").value("new"));
        mvc.perform(get("/public/notices")).andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/public/site"))
                .andExpect(jsonPath("$.data.articleCount").value(3))
                .andExpect(jsonPath("$.data.categoryCount").value(2))
                .andExpect(jsonPath("$.data.commentCount").value(2))
                .andExpect(jsonPath("$.data.messageCount").value(1))
                .andExpect(jsonPath("$.data.totalViews").value(14));
        mvc.perform(get("/public/about")).andExpect(jsonPath("$.data.content", containsString("Zac")));
    }

    @Test
    void latestNoticeIsNullWhenNone() throws Exception {
        jdbc.sql("DELETE FROM sys_notice").update();
        mvc.perform(get("/public/notices/latest")).andExpect(jsonPath("$.data", nullValue()));
    }
}
