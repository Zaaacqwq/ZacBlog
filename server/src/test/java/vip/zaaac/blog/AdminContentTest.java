package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import vip.zaaac.blog.auth.CaptchaService;
import vip.zaaac.blog.config.BlogProperties;

/** 后台：文章、分类、标签、公告、文件、仪表盘、改密码，以及 /admin/** 的鉴权 */
class AdminContentTest extends IntegrationTestSupport {

    @Autowired
    private BlogProperties props;

    @Autowired
    private CaptchaService captchaService;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void adminEndpointsRequireToken() throws Exception {
        String[] paths = {"/admin/articles", "/admin/categories", "/admin/tags", "/admin/notices", "/admin/files",
                "/admin/comments", "/admin/messages", "/admin/dashboard", "/admin/readers",
                "/admin/readers/ip-blocks", "/admin/readers/keyword-blocks"};
        for (String path : paths) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header("Authorization", "Bearer not.a.token")).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(admin())).andExpect(status().isOk());
        }
        mvc.perform(json(post("/admin/articles"), "{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/admin/tags/1")).andExpect(status().isUnauthorized());
        mvc.perform(json(put("/admin/profile/password"), "{}")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/admin/files").file(new MockMultipartFile("file", "a.txt", "text/plain", new byte[] {1})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void articleCrudWithDraftsTagsCategoryAndFiles() throws Exception {
        String body = """
                {"title":"新文章","summary":"简介","coverUrl":"/profile/upload/2024/01/01/c.png","contentType":"markdown",
                 "contentMarkdown":"# 标题","contentHtml":"<h1>标题</h1>","categoryId":2,"tagIds":[1,2],
                 "top":true,"essay":false,"status":"draft","fileIds":[10]}""";
        String created = mvc.perform(json(post("/admin/articles").with(admin()), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("新文章"))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.contentType").value("markdown"))
                .andExpect(jsonPath("$.data.contentMarkdown").value("# 标题"))
                .andExpect(jsonPath("$.data.coverUrl").value("/profile/upload/2024/01/01/c.png"))
                .andExpect(jsonPath("$.data.categoryName").value("生活"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)))
                .andExpect(jsonPath("$.data.files[0].id").value(10))
                .andExpect(jsonPath("$.data.top").value(true))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.data.id");
        var row = jdbc.sql("SELECT blog_pic_type, blog_pic, content_type, status, create_by FROM cms_blog WHERE id = :id")
                .param("id", id).query().singleRow();
        assertThat(row.get("blog_pic_type")).isEqualTo("1");
        assertThat(row.get("content_type")).isEqualTo("4");
        assertThat(row.get("status")).isEqualTo("0");
        assertThat(row.get("create_by")).isEqualTo("admin");

        // 草稿前台看不到，后台能看到
        mvc.perform(get("/public/articles/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/admin/articles").with(admin()).param("status", "draft"))
                .andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/admin/articles").with(admin())).andExpect(jsonPath("$.data.total").value(5));

        // 改成发布的富文本文章、外链封面、换分类和标签、去掉附件
        mvc.perform(json(put("/admin/articles/" + id).with(admin()), """
                        {"title":"改过的","contentType":"html","contentHtml":"<p>x</p>","coverUrl":"https://example.com/p.png",
                         "categoryId":1,"tagIds":[2],"top":false,"essay":true,"status":"published","fileIds":[]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentType").value("html"))
                .andExpect(jsonPath("$.data.coverUrl").value("https://example.com/p.png"))
                .andExpect(jsonPath("$.data.essay").value(true))
                .andExpect(jsonPath("$.data.tags", hasSize(1)))
                .andExpect(jsonPath("$.data.files", hasSize(0)));
        mvc.perform(get("/public/articles/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentHtml").value("<p>x</p>"));

        // 校验
        mvc.perform(json(post("/admin/articles").with(admin()), "{\"title\":\"\",\"status\":\"draft\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/admin/articles").with(admin()), "{\"title\":\"x\",\"status\":\"draft\",\"categoryId\":99}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/admin/articles").with(admin()), "{\"title\":\"x\",\"status\":\"other\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/admin/articles/999").with(admin()), "{\"title\":\"x\",\"status\":\"draft\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/admin/articles/" + id).with(admin())).andExpect(status().isOk());
        mvc.perform(get("/admin/articles/" + id).with(admin())).andExpect(status().isNotFound());
        mvc.perform(delete("/admin/articles/" + id).with(admin())).andExpect(status().isNotFound());
        assertThat(jdbc.sql("SELECT COUNT(*) FROM cms_blog_tag WHERE blog_id = :id").param("id", id)
                .query(Long.class).single()).isZero();
    }

    @Test
    void categoryAndTagCrud() throws Exception {
        String created = mvc.perform(json(post("/admin/categories").with(admin()), "{\"name\":\"读书\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("读书"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.data.id");
        mvc.perform(json(post("/admin/categories").with(admin()), "{\"name\":\"读书\"}")).andExpect(status().isBadRequest());
        mvc.perform(json(put("/admin/categories/" + id).with(admin()), "{\"name\":\"读书笔记\"}"))
                .andExpect(jsonPath("$.data.name").value("读书笔记"));
        // 后台的文章数包含草稿：分类 1 下有文章 1 和草稿 3
        mvc.perform(get("/admin/categories").with(admin()))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].articleCount").value(2));
        mvc.perform(delete("/admin/categories/1").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(delete("/admin/categories/" + id).with(admin())).andExpect(status().isOk());
        mvc.perform(delete("/admin/categories/" + id).with(admin())).andExpect(status().isNotFound());

        mvc.perform(json(post("/admin/tags").with(admin()), "{\"name\":\"Go\"}")).andExpect(status().isOk());
        mvc.perform(json(put("/admin/tags/1").with(admin()), "{\"name\":\"Spring\"}")).andExpect(status().isBadRequest());
        mvc.perform(json(put("/admin/tags/1").with(admin()), "{\"name\":\"JVM\"}"))
                .andExpect(jsonPath("$.data.name").value("JVM"));
        // 删除标签同时解除关联
        mvc.perform(delete("/admin/tags/1").with(admin())).andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT COUNT(*) FROM cms_blog_tag WHERE tag_id = 1").query(Long.class).single()).isZero();
        mvc.perform(get("/admin/tags").with(admin())).andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void noticeCrudWithAttachments() throws Exception {
        mvc.perform(get("/admin/notices").with(admin())).andExpect(jsonPath("$.data.total").value(3));
        String created = mvc.perform(json(post("/admin/notices").with(admin()),
                        "{\"title\":\"维护\",\"content\":\"<p>今晚维护</p>\",\"fileIds\":[10]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("normal"))
                .andExpect(jsonPath("$.data.files[0].name").value("slides.pdf"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.data.id");
        mvc.perform(get("/public/notices/latest")).andExpect(jsonPath("$.data.title").value("维护"));
        mvc.perform(json(put("/admin/notices/" + id).with(admin()),
                        "{\"title\":\"维护结束\",\"content\":\"ok\",\"status\":\"closed\",\"fileIds\":[]}"))
                .andExpect(jsonPath("$.data.title").value("维护结束"))
                .andExpect(jsonPath("$.data.status").value("closed"))
                .andExpect(jsonPath("$.data.files", hasSize(0)));
        mvc.perform(get("/public/notices/latest")).andExpect(jsonPath("$.data.title").value("新公告"));
        mvc.perform(json(post("/admin/notices").with(admin()), "{\"title\":\"x\",\"fileIds\":[999]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/admin/notices/" + id).with(admin())).andExpect(status().isOk());
        mvc.perform(get("/admin/notices/" + id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void uploadedFileIsServedUnderProfileAndCanBeDeleted() throws Exception {
        byte[] bytes = "hello upload".getBytes(StandardCharsets.UTF_8);
        String json = mvc.perform(multipart("/admin/files")
                        .file(new MockMultipartFile("file", "笔记.txt", "text/plain", bytes)).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("笔记.txt"))
                .andExpect(jsonPath("$.data.size").value(bytes.length))
                .andExpect(jsonPath("$.data.url", startsWith("/profile/upload/")))
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(json, "$.data.url");
        int id = JsonPath.read(json, "$.data.id");
        assertThat(url).matches("/profile/upload/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}\\.txt");
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string("hello upload"));
        Path disk = Path.of(props.uploadPath()).resolve(url.substring("/profile/".length()));
        assertThat(disk).exists();

        mvc.perform(get("/admin/files").with(admin()))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.rows[0].url").value(url));
        mvc.perform(multipart("/admin/files")
                        .file(new MockMultipartFile("file", "x.jsp", "text/plain", bytes)).with(admin()))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/admin/files/" + id).with(admin())).andExpect(status().isOk());
        assertThat(Files.exists(disk)).isFalse();
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(delete("/admin/files/" + id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void dashboardCountsContent() throws Exception {
        mvc.perform(get("/admin/dashboard").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleCount").value(3))
                .andExpect(jsonPath("$.data.essayCount").value(1))
                .andExpect(jsonPath("$.data.draftCount").value(1))
                .andExpect(jsonPath("$.data.commentCount").value(2))
                .andExpect(jsonPath("$.data.messageCount").value(1))
                .andExpect(jsonPath("$.data.totalViews").value(14))
                .andExpect(jsonPath("$.data.last7Days", hasSize(7)))
                .andExpect(jsonPath("$.data.articlesByCategory[0].name").value("技术"))
                .andExpect(jsonPath("$.data.articlesByCategory[0].count").value(2))
                .andExpect(jsonPath("$.data.articlesByTag[1].count").value(1));
    }

    @Test
    void changePasswordInvalidatesOldPassword() throws Exception {
        mvc.perform(json(put("/admin/profile/password").with(admin()),
                        "{\"oldPassword\":\"wrong-password\",\"newPassword\":\"test-only-new-password\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/admin/profile/password").with(admin()),
                        "{\"oldPassword\":\"%s\",\"newPassword\":\"123\"}".formatted(ADMIN_PASSWORD)))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/admin/profile/password").with(admin()),
                        "{\"oldPassword\":\"%s\",\"newPassword\":\"test-only-new-password\"}".formatted(ADMIN_PASSWORD)))
                .andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT password FROM sys_user WHERE user_id = 1").query(String.class).single())
                .startsWith("$2");

        login("10.6.0.1", ADMIN_PASSWORD).andExpect(status().isUnauthorized());
        login("10.6.0.2", "test-only-new-password").andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions login(String ip, String password) throws Exception {
        String json = mvc.perform(get("/auth/captcha")).andReturn().getResponse().getContentAsString();
        String captchaId = JsonPath.read(json, "$.data.captchaId");
        return mvc.perform(json(post("/auth/login").with(ip(ip)), """
                {"username":"admin","password":"%s","captchaId":"%s","captchaCode":"%s"}"""
                .formatted(password, captchaId, captchaService.peekAnswer(captchaId))));
    }
}
