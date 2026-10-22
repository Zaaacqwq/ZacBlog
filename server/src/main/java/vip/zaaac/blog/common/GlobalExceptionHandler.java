package vip.zaaac.blog.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handleApi(ApiException e) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(e.status());
        if (e.retryAfterSeconds() > 0) {
            builder.header(HttpHeaders.RETRY_AFTER, Long.toString(e.retryAfterSeconds()));
        }
        return builder.body(ApiResponse.error(e.status().value(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleInvalid(MethodArgumentNotValidException e) {
        FieldError field = e.getBindingResult().getFieldError();
        String msg = field == null ? "参数错误" : field.getField() + " " + field.getDefaultMessage();
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> handleUnreadable(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "参数错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "文件过大，单个文件不能超过 10MB");
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> handleMissing(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "参数错误");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return error(HttpStatus.NOT_FOUND, "资源不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "不支持的请求方法");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("Unhandled error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }

    private static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), msg));
    }
}
