package vip.zaaac.blog.common;

import java.util.List;

public record PageResult<T>(List<T> rows, long total) {
}
