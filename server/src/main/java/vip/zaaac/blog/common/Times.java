package vip.zaaac.blog.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import vip.zaaac.blog.config.BlogProperties;

/** 旧库的 datetime 列没有时区，按 blog.time-zone 解释 */
@Component
public class Times {

    private final Clock clock;
    private final BlogProperties props;

    public Times(Clock clock, BlogProperties props) {
        this.clock = clock;
        this.props = props;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(props.timeZone()));
    }

    public OffsetDateTime toOffset(LocalDateTime time) {
        return time == null ? null : time.atZone(props.timeZone()).toOffsetDateTime();
    }

    public OffsetDateTime read(ResultSet rs, String column) throws SQLException {
        return toOffset(rs.getObject(column, LocalDateTime.class));
    }
}
