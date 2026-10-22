package vip.zaaac.blog.admin;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vip.zaaac.blog.auth.AdminUser;
import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.Sql;
import vip.zaaac.blog.common.Times;

/** 个人中心：当前用户信息（只读）和修改密码。不提供头像、资料修改 */
@RestController
public class AdminProfileController {

    public record Me(String username, String nickname, String avatarUrl) {
    }

    /** BCrypt 只取前 72 字节，新密码限制在 64 个字符以内 */
    public record PasswordRequest(@NotBlank String oldPassword, @NotBlank @Size(min = 6, max = 64) String newPassword) {
    }

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Times times;

    public AdminProfileController(JdbcClient jdbc, PasswordEncoder passwordEncoder, Times times) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.times = times;
    }

    /** 校验旧密码后以 BCrypt 存新密码。旧密码错返回 400（不是 401，避免 web 端把人踢回登录页） */
    @PutMapping("/admin/profile/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        String hash = jdbc.sql("SELECT password FROM sys_user WHERE user_id = :id")
                .param("id", admin.userId()).query(String.class).optional().orElseThrow(ApiException::notFound);
        if (hash == null || hash.isEmpty() || !passwordEncoder.matches(req.oldPassword(), hash)) {
            throw ApiException.badRequest("旧密码错误");
        }
        if (req.oldPassword().equals(req.newPassword())) {
            throw ApiException.badRequest("新密码不能与旧密码相同");
        }
        jdbc.sql("UPDATE sys_user SET password = :hash, update_by = :user, update_time = :now WHERE user_id = :id")
                .param("hash", passwordEncoder.encode(req.newPassword()))
                .param("user", admin.username())
                .param("now", times.now())
                .param("id", admin.userId())
                .update();
        return ApiResponse.ok(null);
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
