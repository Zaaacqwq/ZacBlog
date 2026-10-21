package vip.zaaac.blog.content;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;

@RestController
@RequestMapping("/public")
public class PublicContentController {

    private final ArticleService articles;
    private final SiteService site;

    public PublicContentController(ArticleService articles, SiteService site) {
        this.articles = articles;
        this.site = site;
    }

    @GetMapping("/articles")
    public ApiResponse<PageResult<Dtos.Article>> articles(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean essay) {
        return ApiResponse.ok(articles.list(new ArticleService.ListFilter(categoryId, tagId, keyword, essay),
                PageQuery.of(pageNum, pageSize)));
    }

    @GetMapping("/articles/recommend")
    public ApiResponse<List<Dtos.Article>> recommend(@RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(articles.recommend(limit));
    }

    @GetMapping("/articles/{id}")
    public ApiResponse<Dtos.Article> detail(@PathVariable long id) {
        return ApiResponse.ok(articles.detail(id));
    }

    @GetMapping("/articles/{id}/files")
    public ApiResponse<List<Dtos.FileInfo>> files(@PathVariable long id) {
        if (!articles.isPublished(id)) {
            throw vip.zaaac.blog.common.ApiException.notFound();
        }
        return ApiResponse.ok(articles.files(id));
    }

    @PostMapping("/articles/{id}/views")
    public ApiResponse<Long> addView(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(articles.addView(id, request.getRemoteAddr()));
    }

    @GetMapping("/archives")
    public ApiResponse<List<Dtos.ArchiveMonth>> archives() {
        return ApiResponse.ok(articles.archives());
    }

    @GetMapping("/categories")
    public ApiResponse<List<Dtos.Category>> categories() {
        return ApiResponse.ok(site.categories());
    }

    @GetMapping("/tags")
    public ApiResponse<List<Dtos.Tag>> tags() {
        return ApiResponse.ok(site.tags());
    }

    @GetMapping("/notices")
    public ApiResponse<PageResult<Dtos.Notice>> notices(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(site.notices(PageQuery.of(pageNum, pageSize)));
    }

    @GetMapping("/notices/latest")
    public ApiResponse<Dtos.Notice> latestNotice() {
        return ApiResponse.ok(site.latestNotice());
    }

    @GetMapping("/site")
    public ApiResponse<Dtos.SiteStats> stats() {
        return ApiResponse.ok(site.stats());
    }

    @GetMapping("/about")
    public ApiResponse<Dtos.About> about() {
        return ApiResponse.ok(site.about());
    }
}
