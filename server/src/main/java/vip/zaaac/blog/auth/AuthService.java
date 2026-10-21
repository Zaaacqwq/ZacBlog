package vip.zaaac.blog.auth;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.RateLimiter;
import vip.zaaac.blog.common.Times;
import vip.zaaac.blog.config.BlogProperties;

@Service
public class AuthService {

    // 用户不存在时也做一次 BCrypt 比较，避免通过响应时间探测用户名

    public record LoginResult(String token, java.time.OffsetDateTime expiresAt) {
    }

    private record UserRow(long userId, String username, String password) {
    }

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final CaptchaService captcha;
    private final RateLimiter rateLimiter;
    private final BlogProperties props;
    private final Times times;
    // 用户不存在时也做一次 BCrypt 比较，避免通过响应时间探测用户名
    private final String dummyHash;

    public AuthService(JdbcClient jdbc, PasswordEncoder passwordEncoder, JwtService jwt, CaptchaService captcha,
            RateLimiter rateLimiter, BlogProperties props, Times times) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.captcha = captcha;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.times = times;
        this.dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    public LoginResult login(String username, String password, String captchaId, String captchaCode, String ip) {
        int limit = props.rateLimit().loginPerMinute();
        long wait = rateLimiter.tryAcquire(List.of(
                new RateLimiter.Rule("login-ip:" + ip, limit, Duration.ofMinutes(1)),
                new RateLimiter.Rule("login-user:" + username.toLowerCase(Locale.ROOT), limit, Duration.ofMinutes(1))));
        if (wait > 0) {
            throw ApiException.tooManyRequests(wait);
        }
        if (!captcha.verify(captchaId, captchaCode)) {
            throw ApiException.badRequest("验证码错误或已过期");
        }
        Optional<UserRow> user = jdbc.sql("""
                SELECT user_id, user_name, password FROM sys_user
                WHERE user_name = :username AND del_flag = '0' AND status = '0'
                """)
                .param("username", username)
                .query((rs, i) -> new UserRow(rs.getLong("user_id"), rs.getString("user_name"), rs.getString("password")))
                .optional();
        String hash = user.map(UserRow::password).filter(h -> h != null && !h.isEmpty()).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !matches || !props.adminUserIds().contains(user.get().userId())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        jdbc.sql("UPDATE sys_user SET login_ip = :ip, login_date = :now WHERE user_id = :id")
                .param("ip", ip).param("now", times.now()).param("id", user.get().userId())
                .update();
        JwtService.Token token = jwt.issue(user.get().userId(), user.get().username());
        return new LoginResult(token.token(), token.expiresAt().atZone(props.timeZone()).toOffsetDateTime());
    }
}
