package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import vip.zaaac.blog.config.BlogProperties;

class ProfileFileTest extends IntegrationTestSupport {

    @Autowired
    private BlogProperties props;

    private Path root;

    @BeforeEach
    void createFiles() throws Exception {
        root = Path.of(props.uploadPath());
        Path dir = Files.createDirectories(root.resolve("upload/2023/10/13"));
        Files.writeString(dir.resolve("hello.txt"), "hello profile", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("x.svg"), "<svg/>", StandardCharsets.UTF_8);
        // UPLOAD_PATH 外面的“机密”文件
        Files.writeString(root.getParent().resolve("zacblog-secret.txt"), "secret", StandardCharsets.UTF_8);
    }

    @Test
    void servesFilesUnderUploadPathAtLegacyUrls() throws Exception {
        mvc.perform(get("/profile/upload/2023/10/13/hello.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello profile"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/profile/upload/2023/10/13/x.svg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
    }

    @Test
    void missingFilesAndDirectoriesAreNotFound() throws Exception {
        mvc.perform(get("/profile/upload/2023/10/13/nope.png")).andExpect(status().isNotFound());
        mvc.perform(get("/profile/upload/2023/10/13")).andExpect(status().isNotFound());
        mvc.perform(get("/profile/upload/2023/10/13/")).andExpect(status().isNotFound());
    }

    @Test
    void pathTraversalIsRejected() throws Exception {
        assertThat(root.getParent().resolve("zacblog-secret.txt")).exists();
        String[] attacks = {
                "/profile/../zacblog-secret.txt",
                "/profile/upload/../../zacblog-secret.txt",
                "/profile/upload/%2e%2e/%2e%2e/zacblog-secret.txt",
                "/profile/upload/..%2f..%2fzacblog-secret.txt",
                "/profile/upload/..%5c..%5czacblog-secret.txt",
                "/profile/%2e%2e%2fzacblog-secret.txt",
        };
        for (String attack : attacks) {
            var result = mvc.perform(get(URI.create(attack))).andReturn().getResponse();
            assertThat(result.getStatus()).as(attack).isBetween(400, 499);
            assertThat(result.getContentAsString()).as(attack).doesNotContain("secret");
        }
    }
}
