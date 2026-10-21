package vip.zaaac.blog.auth;

/** 已通过 JWT 认证的管理员，作为 Authentication 的 principal */
public record AdminUser(long userId, String username) {
}
