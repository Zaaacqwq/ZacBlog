package vip.zaaac.blog.comment;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;

@RestController
@RequestMapping("/public")
public class PublicCommentController {

    /** homepage 是蜜罐字段：页面上隐藏，正常用户不会填写 */
    public record CommentRequest(
            Long parentId,
            @NotBlank @Size(max = 20) String nickname,
            @NotBlank @Email @Size(max = 128) String email,
            @Size(max = 255) @Pattern(regexp = "^https?://\\S+$", message = "必须是 http(s) 地址") String website,
            @NotBlank @Size(max = 255) String content,
            String homepage) {

        CommentService.PostRequest toPost() {
            return new CommentService.PostRequest(parentId, nickname, email, website, content, homepage);
        }
    }

    private final CommentService comments;

    public PublicCommentController(CommentService comments) {
        this.comments = comments;
    }

    @GetMapping("/articles/{id}/comments")
    public ApiResponse<PageResult<CommentDto>> articleComments(@PathVariable long id,
            @RequestParam(required = false) Integer pageNum, @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(comments.tree(Board.COMMENT, id, PageQuery.of(pageNum, pageSize)));
    }

    @PostMapping("/articles/{id}/comments")
    public ApiResponse<CommentDto> postComment(@PathVariable long id, @Valid @RequestBody CommentRequest req,
            HttpServletRequest request) {
        return ApiResponse.ok(comments.post(Board.COMMENT, id, req.toPost(), request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT)));
    }

    @GetMapping("/messages")
    public ApiResponse<PageResult<CommentDto>> messages(
            @RequestParam(required = false) Integer pageNum, @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(comments.tree(Board.MESSAGE, null, PageQuery.of(pageNum, pageSize)));
    }

    @PostMapping("/messages")
    public ApiResponse<CommentDto> postMessage(@Valid @RequestBody CommentRequest req, HttpServletRequest request) {
        return ApiResponse.ok(comments.post(Board.MESSAGE, null, req.toPost(), request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT)));
    }

    @PostMapping("/comments/{id}/like")
    public ApiResponse<Long> likeComment(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(comments.like(Board.COMMENT, id, request.getRemoteAddr(), true));
    }

    @DeleteMapping("/comments/{id}/like")
    public ApiResponse<Long> unlikeComment(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(comments.like(Board.COMMENT, id, request.getRemoteAddr(), false));
    }

    @PostMapping("/messages/{id}/like")
    public ApiResponse<Long> likeMessage(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(comments.like(Board.MESSAGE, id, request.getRemoteAddr(), true));
    }

    @DeleteMapping("/messages/{id}/like")
    public ApiResponse<Long> unlikeMessage(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(comments.like(Board.MESSAGE, id, request.getRemoteAddr(), false));
    }
}
