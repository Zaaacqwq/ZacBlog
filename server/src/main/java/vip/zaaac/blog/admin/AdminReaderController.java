package vip.zaaac.blog.admin;

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
import vip.zaaac.blog.comment.ReaderService;
import vip.zaaac.blog.comment.ReaderService.IpBlock;
import vip.zaaac.blog.comment.ReaderService.KeywordBlock;
import vip.zaaac.blog.comment.ReaderService.Reader;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;

/** 读者管理：按邮箱聚合的读者列表、邮箱拉黑，以及 IP / 关键字黑名单 */
@RestController
@RequestMapping("/admin/readers")
public class AdminReaderController {

    public record ReasonRequest(@Size(max = 255) String reason) {
    }

    public record IpBlockRequest(@NotBlank @Size(max = 64) String ip, @Size(max = 255) String reason) {
    }

    public record KeywordBlockRequest(@NotBlank @Size(max = 64) String keyword, @Size(max = 255) String reason) {
    }

    private final ReaderService readers;

    public AdminReaderController(ReaderService readers) {
        this.readers = readers;
    }

    @GetMapping
    public ApiResponse<PageResult<Reader>> list(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(defaultValue = "desc") String order) {
        return ApiResponse.ok(readers.list(keyword, "asc".equalsIgnoreCase(order), blocked,
                PageQuery.of(pageNum, pageSize)));
    }

    @PostMapping("/{email}/block")
    public ApiResponse<Void> block(@PathVariable String email, @Valid @RequestBody(required = false) ReasonRequest req) {
        readers.block(email, req == null ? null : req.reason());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{email}/block")
    public ApiResponse<Void> unblock(@PathVariable String email) {
        readers.unblock(email);
        return ApiResponse.ok(null);
    }

    @GetMapping("/ip-blocks")
    public ApiResponse<PageResult<IpBlock>> ipBlocks(
            @RequestParam(required = false) Integer pageNum, @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(readers.ipBlocks(PageQuery.of(pageNum, pageSize)));
    }

    @PostMapping("/ip-blocks")
    public ApiResponse<IpBlock> addIpBlock(@Valid @RequestBody IpBlockRequest req) {
        return ApiResponse.ok(readers.addIpBlock(req.ip(), req.reason()));
    }

    @DeleteMapping("/ip-blocks/{id}")
    public ApiResponse<Void> deleteIpBlock(@PathVariable long id) {
        readers.deleteIpBlock(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/keyword-blocks")
    public ApiResponse<PageResult<KeywordBlock>> keywordBlocks(
            @RequestParam(required = false) Integer pageNum, @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(readers.keywordBlocks(PageQuery.of(pageNum, pageSize)));
    }

    @PostMapping("/keyword-blocks")
    public ApiResponse<KeywordBlock> addKeywordBlock(@Valid @RequestBody KeywordBlockRequest req) {
        return ApiResponse.ok(readers.addKeywordBlock(req.keyword(), req.reason()));
    }

    @DeleteMapping("/keyword-blocks/{id}")
    public ApiResponse<Void> deleteKeywordBlock(@PathVariable long id) {
        readers.deleteKeywordBlock(id);
        return ApiResponse.ok(null);
    }
}
