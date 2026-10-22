package vip.zaaac.blog.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vip.zaaac.blog.auth.AdminUser;
import vip.zaaac.blog.comment.AdminCommentService;
import vip.zaaac.blog.comment.AdminCommentService.AdminComment;
import vip.zaaac.blog.comment.AdminCommentService.Kind;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;

/** 评论、留言审核：/admin/comments/** 和 /admin/messages/** 同一套接口 */
@RestController
@RequestMapping("/admin")
public class AdminCommentController {

    public record ReplyRequest(@NotBlank @Size(max = 255) String content) {
    }

    private final AdminCommentService comments;

    public AdminCommentController(AdminCommentService comments) {
        this.comments = comments;
    }

    @GetMapping("/comments")
    public ApiResponse<PageResult<AdminComment>> comments(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) String reader,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(comments.list(Kind.COMMENT,
                new AdminCommentService.Filter(articleId, reader, keyword, from, to), PageQuery.of(pageNum, pageSize)));
    }

    @PostMapping("/comments/{id}/reply")
    public ApiResponse<AdminComment> replyComment(@PathVariable long id, @Valid @RequestBody ReplyRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(comments.reply(Kind.COMMENT, id, req.content(), admin.userId(), admin.username()));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Integer> deleteComment(@PathVariable long id) {
        return ApiResponse.ok(comments.delete(Kind.COMMENT, id));
    }

    @GetMapping("/messages")
    public ApiResponse<PageResult<AdminComment>> messages(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String reader,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(comments.list(Kind.MESSAGE,
                new AdminCommentService.Filter(null, reader, keyword, from, to), PageQuery.of(pageNum, pageSize)));
    }

    @PostMapping("/messages/{id}/reply")
    public ApiResponse<AdminComment> replyMessage(@PathVariable long id, @Valid @RequestBody ReplyRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(comments.reply(Kind.MESSAGE, id, req.content(), admin.userId(), admin.username()));
    }

    @DeleteMapping("/messages/{id}")
    public ApiResponse<Integer> deleteMessage(@PathVariable long id) {
        return ApiResponse.ok(comments.delete(Kind.MESSAGE, id));
    }
}
