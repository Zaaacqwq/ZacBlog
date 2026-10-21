package vip.zaaac.blog.auth;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

/**
 * 算术验证码（沿用旧版若依的 "a op b = ?" 形式）。答案只存在内存里，5 分钟有效、用一次即作废。
 */
@Service
public class CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_PENDING = 10_000;

    public record Captcha(String captchaId, String image) {
    }

    private record Pending(String answer, long expiresAt) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final Clock clock;

    public CaptchaService(Clock clock) {
        this.clock = clock;
    }

    public Captcha create() {
        long now = clock.millis();
        if (pending.size() >= MAX_PENDING) {
            pending.values().removeIf(p -> p.expiresAt() <= now);
            if (pending.size() >= MAX_PENDING) {
                pending.clear();
            }
        }
        int a = random.nextInt(10);
        int b = random.nextInt(10);
        String question;
        int answer;
        switch (random.nextInt(3)) {
            case 0 -> {
                question = a + " + " + b + " = ?";
                answer = a + b;
            }
            case 1 -> {
                int hi = Math.max(a, b);
                int lo = Math.min(a, b);
                question = hi + " - " + lo + " = ?";
                answer = hi - lo;
            }
            default -> {
                question = a + " × " + b + " = ?";
                answer = a * b;
            }
        }
        String id = UUID.randomUUID().toString();
        pending.put(id, new Pending(Integer.toString(answer), now + TTL.toMillis()));
        return new Captcha(id, "data:image/png;base64," + render(question));
    }

    /** 校验并作废；不管对错，同一个 captchaId 只能用一次 */
    public boolean verify(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }
        Pending p = pending.remove(captchaId);
        return p != null && p.expiresAt() > clock.millis() && p.answer().equals(code.trim());
    }

    /** 仅供测试读取答案，不会作废验证码 */
    public String peekAnswer(String captchaId) {
        Pending p = pending.get(captchaId);
        return p == null ? null : p.answer();
    }

    private String render(String text) {
        int width = 160;
        int height = 60;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
            }
            g.setColor(new Color(30, 60, 120));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            g.drawString(text, 12, 40);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
