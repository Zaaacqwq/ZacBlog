package vip.zaaac.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import vip.zaaac.blog.auth.CaptchaService;

class AuthTest extends IntegrationTestSupport {

    @Autowired
    private CaptchaService captchaService;

    private String[] captcha() throws Exception {
        String json = mvc.perform(get("/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.image", startsWith("data:image/png;base64,")))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.data.captchaId");
        return new String[] {id, captchaService.peekAnswer(id)};
    }

    private ResultActions login(String ip, String username, String password, String captchaId, String code)
            throws Exception {
        return mvc.perform(post("/auth/login").with(ip(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s","captchaId":"%s","captchaCode":"%s"}"""
                        .formatted(username, password, captchaId, code)));
    }

    private ResultActions login(String ip, String username, String password) throws Exception {
        String[] c = captcha();
        return login(ip, username, password, c[0], c[1]);
    }

    @Test
    void loginSucceedsAndTokenOpensAdmin() throws Exception {
        String json = login("10.3.0.1", "admin", ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.data.token");
        mvc.perform(get("/admin/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.nickname").value("博主"));
        assertThat(jdbc.sql("SELECT login_ip FROM sys_user WHERE user_id = 1").query(String.class).single())
                .isEqualTo("10.3.0.1");
    }

    @Test
    void wrongPasswordUnknownUserAndNonAdminAreUnauthorized() throws Exception {
        login("10.3.1.1", "admin", "wrong-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        login("10.3.1.2", "nobody", ADMIN_PASSWORD).andExpect(status().isUnauthorized());
        // 旧注册读者在 sys_user 里，但不是管理员
        login("10.3.1.3", "reader", ADMIN_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongOrReusedCaptchaIsBadRequest() throws Exception {
        String[] c = captcha();
        String wrong = Integer.toString(Integer.parseInt(c[1]) + 1);
        login("10.3.2.1", "admin", ADMIN_PASSWORD, c[0], wrong).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        // 用错一次即作废
        login("10.3.2.1", "admin", ADMIN_PASSWORD, c[0], c[1]).andExpect(status().isBadRequest());
        login("10.3.2.1", "admin", ADMIN_PASSWORD, "no-such-captcha", "1").andExpect(status().isBadRequest());
    }

    @Test
    void loginIsRateLimitedPerIpAndPerUsername() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("10.3.3.1", "admin", "wrong-password").andExpect(status().isUnauthorized());
        }
        login("10.3.3.1", "admin", ADMIN_PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // 同一用户名换 IP 也被限
        login("10.3.3.2", "admin", ADMIN_PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void adminRequiresValidToken() throws Exception {
        mvc.perform(get("/admin/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/admin/anything")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/me").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
        // 签名被篡改
        String json = login("10.3.4.1", "admin", ADMIN_PASSWORD).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.data.token");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        mvc.perform(get("/admin/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routesHaveNoApiPrefixAndUnknownPathsAreRejected() throws Exception {
        mvc.perform(get("/api/public/articles")).andExpect(status().is4xxClientError());
        mvc.perform(get("/something-else")).andExpect(status().is4xxClientError());
    }
}
