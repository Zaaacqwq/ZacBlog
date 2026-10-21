package vip.zaaac.blog.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.http.HttpServletRequest;
import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.config.BlogProperties;

/**
 * /profile/** → UPLOAD_PATH 下的文件，兼容旧地址 /profile/upload/yyyy/MM/dd/xxx。
 * 只返回 UPLOAD_PATH 内的普通文件：不列目录，拒绝 ..、反斜杠、跳出目录的符号链接。
 */
@RestController
public class ProfileController {

    private static final String PREFIX = "/profile/";
    // 这些类型在浏览器里能执行脚本，一律按附件下载
    private static final Set<String> UNSAFE_INLINE = Set.of("text/html", "image/svg+xml", "application/xml",
            "text/xml", "application/xhtml+xml", "text/javascript", "application/javascript");

    private final Path root;
    private final UrlPathHelper pathHelper = new UrlPathHelper();

    public ProfileController(BlogProperties props) {
        this.root = Path.of(props.uploadPath()).toAbsolutePath().normalize();
    }

    @GetMapping("/profile/**")
    public ResponseEntity<Resource> file(HttpServletRequest request) throws IOException {
        Path file = resolve(pathHelper.getPathWithinApplication(request));
        MediaType type = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .contentType(type);
        if (UNSAFE_INLINE.contains(type.getType() + "/" + type.getSubtype())) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(file.getFileName().toString(), java.nio.charset.StandardCharsets.UTF_8).build().toString());
        }
        return builder.body(new FileSystemResource(file));
    }

    Path resolve(String requestPath) throws IOException {
        if (requestPath == null || !requestPath.startsWith(PREFIX)) {
            throw ApiException.notFound();
        }
        String relative = requestPath.substring(PREFIX.length());
        if (relative.isEmpty() || relative.contains("\\") || relative.indexOf('\0') >= 0) {
            throw ApiException.notFound();
        }
        for (String segment : relative.split("/")) {
            if (segment.equals("..") || segment.equals(".")) {
                throw ApiException.badRequest("非法路径");
            }
        }
        Path file;
        try {
            file = root.resolve(relative).normalize();
        } catch (InvalidPathException e) {
            throw ApiException.notFound();
        }
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw ApiException.notFound();
        }
        // 符号链接解析后也必须还在 UPLOAD_PATH 里
        if (!file.toRealPath().startsWith(root.toRealPath())) {
            throw ApiException.notFound();
        }
        return file;
    }
}
