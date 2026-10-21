package vip.zaaac.blog.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("blog")
public record BlogProperties(
        Jwt jwt,
        String uploadPath,
        ZoneId timeZone,
        Set<Long> adminUserIds,
        RateLimit rateLimit) {

    public record Jwt(String secret, Duration ttl) {
    }

    public record RateLimit(int loginPerMinute, Duration postInterval, int postsPerDay) {
    }
}
