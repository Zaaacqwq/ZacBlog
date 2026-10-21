package vip.zaaac.blog.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import vip.zaaac.blog.config.BlogProperties;

/** HS256 JWT，密钥来自 JWT_SECRET */
@Service
public class JwtService {

    private static final String HEADER = b64(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final int MIN_SECRET_BYTES = 32;

    public record Token(String token, Instant expiresAt) {
    }

    public record Claims(long userId, String username) {
    }

    private final byte[] secret;
    private final BlogProperties.Jwt config;
    private final Clock clock;
    private final ObjectMapper mapper;

    public JwtService(BlogProperties props, Clock clock, ObjectMapper mapper) {
        this.config = props.jwt();
        String raw = config == null ? null : config.secret();
        if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET 未设置或少于 " + MIN_SECRET_BYTES + " 字节");
        }
        this.secret = raw.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
        this.mapper = mapper;
    }

    public Token issue(long userId, String username) {
        Instant now = clock.instant();
        Instant exp = now.plus(config.ttl());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("uid", userId);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", exp.getEpochSecond());
        try {
            String body = HEADER + "." + b64(mapper.writeValueAsBytes(payload));
            return new Token(body + "." + b64(sign(body)), exp);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 签名、算法、过期时间都校验通过时返回 claims，否则返回 null */
    public Claims verify(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !HEADER.equals(parts[0])) {
            return null;
        }
        try {
            byte[] expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected, Base64.getUrlDecoder().decode(parts[2]))) {
                return null;
            }
            Map<String, Object> payload = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<Map<String, Object>>() { });
            if (!(payload.get("exp") instanceof Number exp) || exp.longValue() <= clock.instant().getEpochSecond()) {
                return null;
            }
            if (!(payload.get("uid") instanceof Number uid) || !(payload.get("sub") instanceof String sub)) {
                return null;
            }
            return new Claims(uid.longValue(), sub);
        } catch (IllegalArgumentException | java.io.IOException e) {
            return null;
        }
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
