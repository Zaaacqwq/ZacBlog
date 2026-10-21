package vip.zaaac.blog.common;

import org.springframework.http.HttpStatus;

/** 业务异常：status 即响应的 HTTP 状态和 code；retryAfterSeconds 用于 429 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final long retryAfterSeconds;

    public ApiException(HttpStatus status, String msg) {
        this(status, msg, 0);
    }

    private ApiException(HttpStatus status, String msg, long retryAfterSeconds) {
        super(msg);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApiException badRequest(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg);
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "资源不存在");
    }

    public static ApiException tooManyRequests(long retryAfterSeconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "请求太频繁，请稍后再试", Math.max(retryAfterSeconds, 1));
    }

    public HttpStatus status() {
        return status;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
