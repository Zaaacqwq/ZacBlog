package vip.zaaac.blog.admin;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vip.zaaac.blog.auth.AdminUser;
import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.Sql;

/** GET /admin/me。其余 /admin/** 接口由 T3 实现 */
@RestController
public class AdminProfileController {

    public record Me(String username, String nickname, String avatarUrl) {
    }

    private final JdbcClient jdbc;

    public AdminProfileController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/admin/me")
    public ApiResponse<Me> me(@AuthenticationPrincipal AdminUser admin) {
        Me me = jdbc.sql("SELECT user_name, nick_name, avatar FROM sys_user WHERE user_id = :id")
                .params(Map.of("id", admin.userId()))
                .query((rs, i) -> new Me(rs.getString("user_name"), rs.getString("nick_name"),
                        Sql.emptyToNull(rs.getString("avatar"))))
                .optional()
                .orElseThrow(ApiException::notFound);
        return ApiResponse.ok(me);
    }
}
