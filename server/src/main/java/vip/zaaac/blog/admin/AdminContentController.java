package vip.zaaac.blog.admin;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vip.zaaac.blog.auth.AdminUser;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.content.AdminContentService;
import vip.zaaac.blog.content.AdminContentService.AdminNotice;
import vip.zaaac.blog.content.Dtos;

/** 文章、分类、标签、公告 */
@RestController
@RequestMapping("/admin")
public class AdminContentController {

    /** 长度上限对应旧表的列宽：title varchar(50)，blog_desc / blog_pic / blog_pic_link varchar(100) */
    public record ArticleRequest(
            @NotBlank @Size(max = 50) String title,
            @Size(max = 100) String summary,
            @Size(max = 100) String coverUrl,
            @Pattern(regexp = "markdown|html", message = "只能是 markdown 或 html") String contentType,
            String contentMarkdown,
            String contentHtml,
            Long categoryId,
            List<Long> tagIds,
            boolean top,
            boolean essay,
            @NotBlank @Pattern(regexp = "published|draft", message = "只能是 published 或 draft") String status,
            List<Long> fileIds) {

        AdminContentService.ArticleForm toForm() {
            return new AdminContentService.ArticleForm(title, summary, coverUrl,
                    contentType == null ? "markdown" : contentType, contentMarkdown, contentHtml, categoryId, tagIds,
                    top, essay, status, fileIds);
        }
    }

    public record NameRequest(@NotBlank @Size(max = 50) String name) {
    }

    public record NoticeRequest(
            @NotBlank @Size(max = 50) String title,
            String content,
            @Pattern(regexp = "normal|closed", message = "只能是 normal 或 closed") String status,
            List<Long> fileIds) {

        AdminContentService.NoticeForm toForm() {
            return new AdminContentService.NoticeForm(title, content, status, fileIds);
        }
    }

    private final AdminContentService content;

    public AdminContentController(AdminContentService content) {
        this.content = content;
    }

    @GetMapping("/articles")
    public ApiResponse<PageResult<Dtos.Article>> articles(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean essay) {
        return ApiResponse.ok(content.listArticles(
                new AdminContentService.AdminFilter(keyword, categoryId, tagId, status, essay),
                PageQuery.of(pageNum, pageSize)));
    }

    @GetMapping("/articles/{id}")
    public ApiResponse<Dtos.Article> article(@PathVariable long id) {
        return ApiResponse.ok(content.article(id));
    }

    @PostMapping("/articles")
    public ApiResponse<Dtos.Article> createArticle(@Valid @RequestBody ArticleRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.createArticle(req.toForm(), admin.username()));
    }

    @PutMapping("/articles/{id}")
    public ApiResponse<Dtos.Article> updateArticle(@PathVariable long id, @Valid @RequestBody ArticleRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.updateArticle(id, req.toForm(), admin.username()));
    }

    @DeleteMapping("/articles/{id}")
    public ApiResponse<Void> deleteArticle(@PathVariable long id) {
        content.deleteArticle(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/categories")
    public ApiResponse<List<Dtos.Category>> categories() {
        return ApiResponse.ok(content.categories());
    }

    @PostMapping("/categories")
    public ApiResponse<Dtos.Category> createCategory(@Valid @RequestBody NameRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.createCategory(req.name(), admin.username()));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Dtos.Category> updateCategory(@PathVariable long id, @Valid @RequestBody NameRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.updateCategory(id, req.name(), admin.username()));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable long id) {
        content.deleteCategory(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/tags")
    public ApiResponse<List<Dtos.Tag>> tags() {
        return ApiResponse.ok(content.tags());
    }

    @PostMapping("/tags")
    public ApiResponse<Dtos.Tag> createTag(@Valid @RequestBody NameRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.createTag(req.name(), admin.username()));
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<Dtos.Tag> updateTag(@PathVariable long id, @Valid @RequestBody NameRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.updateTag(id, req.name(), admin.username()));
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable long id) {
        content.deleteTag(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/notices")
    public ApiResponse<PageResult<AdminNotice>> notices(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(content.notices(keyword, PageQuery.of(pageNum, pageSize)));
    }

    @GetMapping("/notices/{id}")
    public ApiResponse<AdminNotice> notice(@PathVariable long id) {
        return ApiResponse.ok(content.notice(id));
    }

    @PostMapping("/notices")
    public ApiResponse<AdminNotice> createNotice(@Valid @RequestBody NoticeRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.createNotice(req.toForm(), admin.username()));
    }

    @PutMapping("/notices/{id}")
    public ApiResponse<AdminNotice> updateNotice(@PathVariable long id, @Valid @RequestBody NoticeRequest req,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(content.updateNotice(id, req.toForm(), admin.username()));
    }

    @DeleteMapping("/notices/{id}")
    public ApiResponse<Void> deleteNotice(@PathVariable long id) {
        content.deleteNotice(id);
        return ApiResponse.ok(null);
    }
}
