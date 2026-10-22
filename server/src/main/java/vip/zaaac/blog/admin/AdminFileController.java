package vip.zaaac.blog.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vip.zaaac.blog.auth.AdminUser;
import vip.zaaac.blog.common.ApiResponse;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.content.Dtos.FileInfo;
import vip.zaaac.blog.file.FileService;

@RestController
@RequestMapping("/admin/files")
public class AdminFileController {

    private final FileService files;

    public AdminFileController(FileService files) {
        this.files = files;
    }

    @PostMapping
    public ApiResponse<FileInfo> upload(@RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AdminUser admin) {
        return ApiResponse.ok(files.upload(file, admin.username()));
    }

    @GetMapping
    public ApiResponse<PageResult<FileInfo>> list(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(files.list(keyword, PageQuery.of(pageNum, pageSize)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        files.delete(id);
        return ApiResponse.ok(null);
    }
}
