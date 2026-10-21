package vip.zaaac.blog.common;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 进程内的滑动窗口限流（单实例部署足够）。重启后计数清零。
 */
@Component
public class RateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    public record Rule(String key, int limit, Duration window) {
    }

    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new HashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 所有规则都未超限时，记一次请求并返回 0；否则不记录，返回需要等待的秒数。
     */
    public synchronized long tryAcquire(List<Rule> rules) {
        long now = clock.millis();
        if (hits.size() > CLEANUP_THRESHOLD) {
            cleanup(now);
        }
        long waitMillis = 0;
        for (Rule rule : rules) {
            Deque<Long> q = hits.computeIfAbsent(rule.key(), k -> new ArrayDeque<>());
            long windowStart = now - rule.window().toMillis();
            while (!q.isEmpty() && q.peekFirst() <= windowStart) {
                q.pollFirst();
            }
            if (q.size() >= rule.limit()) {
                // 最早的那次请求滑出窗口后才能再请求
                long oldestInWindow = q.size() == rule.limit() ? q.peekFirst() : q.toArray(Long[]::new)[q.size() - rule.limit()];
                waitMillis = Math.max(waitMillis, oldestInWindow + rule.window().toMillis() - now);
            }
        }
        if (waitMillis > 0) {
            return (waitMillis + 999) / 1000;
        }
        for (Rule rule : rules) {
            hits.get(rule.key()).addLast(now);
        }
        return 0;
    }

    public long tryAcquire(String key, int limit, Duration window) {
        return tryAcquire(List.of(new Rule(key, limit, window)));
    }

    /** 测试用：清空所有计数 */
    public synchronized void reset() {
        hits.clear();
    }

    // 规则窗口最长一天，超过一天没有请求的 key 可以丢掉
    private void cleanup(long now) {
        long dayAgo = now - Duration.ofDays(1).toMillis();
        hits.values().removeIf(q -> q.isEmpty() || q.peekLast() <= dayAgo);
    }
}
