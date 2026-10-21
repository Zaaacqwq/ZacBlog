package vip.zaaac.blog.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter = new RateLimiter(clock);

    private List<RateLimiter.Rule> postRules() {
        return List.of(new RateLimiter.Rule("interval", 1, Duration.ofSeconds(30)),
                new RateLimiter.Rule("day", 20, Duration.ofDays(1)));
    }

    @Test
    void enforcesIntervalAndDailyLimit() {
        assertThat(limiter.tryAcquire(postRules())).isZero();
        assertThat(limiter.tryAcquire(postRules())).isEqualTo(30);
        clock.now = clock.now.plusSeconds(10);
        assertThat(limiter.tryAcquire(postRules())).isEqualTo(20);

        for (int i = 1; i < 20; i++) {
            clock.now = clock.now.plusSeconds(31);
            assertThat(limiter.tryAcquire(postRules())).as("post %d", i + 1).isZero();
        }
        clock.now = clock.now.plusSeconds(31);
        long wait = limiter.tryAcquire(postRules());
        assertThat(wait).isPositive().isLessThanOrEqualTo(Duration.ofDays(1).toSeconds());

        clock.now = clock.now.plusSeconds(wait);
        assertThat(limiter.tryAcquire(postRules())).isZero();
    }

    @Test
    void rejectedRequestsDoNotConsumeOtherRules() {
        List<RateLimiter.Rule> rules = List.of(new RateLimiter.Rule("a", 1, Duration.ofMinutes(1)),
                new RateLimiter.Rule("b", 2, Duration.ofMinutes(1)));
        assertThat(limiter.tryAcquire(rules)).isZero();
        assertThat(limiter.tryAcquire(rules)).isPositive();
        // b 只记了一次，还能再用一次
        assertThat(limiter.tryAcquire("b", 2, Duration.ofMinutes(1))).isZero();
        assertThat(limiter.tryAcquire("b", 2, Duration.ofMinutes(1))).isPositive();
    }
}
