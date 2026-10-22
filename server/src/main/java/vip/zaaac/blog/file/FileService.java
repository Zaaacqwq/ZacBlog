package vip.zaaac.blog.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vip.zaaac.blog.common.ApiException;
import vip.zaaac.blog.common.PageQuery;
import vip.zaaac.blog.common.PageResult;
import vip.zaaac.blog.common.Times;
import vip.zaaac.blog.config.BlogProperties;
import vip.zaaac.blog.content.ArticleService;
import vip.zaaac.blog.content.Dtos.FileInfo;

/**
 * 上传文件：存到 UPLOAD_PATH/upload/yyyy/MM/dd/<uuid>.<扩展名>，sys_file_info.file_path 记
 * /profile/upload/yyyy/MM/dd/<uuid>.<扩展名>，与旧版若依 FileUploadUtils 的格式一致。
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    // 与旧版若依的默认白名单一致：图片、文档、压缩包、音视频
    private static final Set<String> ALLOWED = Set.of("bmp", "gif", "jpg", "jpeg", "png", "webp", "ico",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm", "txt", "md", "pdf",
            "rar", "zip", "gz", "bz2", "7z", "mp3", "mp4", "avi", "rmvb", "mov", "flv", "swf");

    private final JdbcClient jdbc;
    private final Times times;
    private final Path root;

    public FileService(JdbcClient jdbc, Times times, BlogProperties props) {
        this.jdbc = jdbc;
        this.times = times;
        this.root = Path.of(props.uploadPath()).toAbsolutePath().normalize();
    }

    @Transactional
    public FileInfo upload(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename().replace('\\', '/'))
                .getFileName().toString();
        if (originalName.length() > 200) {
            originalName = originalName.substring(originalName.length() - 200);
        }
        int dot = originalName.lastIndexOf('.');
        String suffix = dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(suffix)) {
            throw ApiException.badRequest("不支持的文件类型");
        }
        LocalDateTime now = times.now();
        String objectName = UUID.randomUUID() + "." + suffix;
        String relative = "upload/" + DATE_PATH.format(now) + "/" + objectName;
        Path target = root.resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String url = "/profile/" + relative;
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO sys_file_info (file_origin_name, file_suffix, file_size_info, file_object_name, file_path,
                  del_flag, create_by, create_time)
                VALUES (:name, :suffix, :size, :object, :path, 'N', :user, :now)
                """)
                .param("name", originalName)
                .param("suffix", suffix)
                .param("size", sizeInfo(file.getSize()))
                .param("object", objectName)
                .param("path", url)
                .param("user", username)
                .param("now", now)
                .update(keys, "file_id");
        return new FileInfo(keys.getKey().longValue(), originalName, url, file.getSize(), times.toOffset(now));
    }

    public PageResult<FileInfo> list(String keyword, PageQuery page) {
        String where = " WHERE del_flag = 'N'";
        Map<String, Object> params = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            where += " AND LOWER(file_origin_name) LIKE :keyword";
            params.put("keyword", "%" + keyword.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        long total = jdbc.sql("SELECT COUNT(*) FROM sys_file_info" + where).params(params).query(Long.class).single();
        params.put("limit", page.pageSize());
        params.put("offset", page.offset());
        List<FileInfo> rows = jdbc.sql("SELECT file_id, file_origin_name, file_path, file_size_info, create_time"
                        + " FROM sys_file_info" + where + " ORDER BY create_time DESC, file_id DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> new FileInfo(rs.getLong("file_id"), rs.getString("file_origin_name"),
                        rs.getString("file_path"), ArticleService.parseSize(rs.getString("file_size_info")), times.read(rs, "create_time")))
                .list();
        return new PageResult<>(rows, total);
    }

    /** 删除记录、文章/公告里的附件关联和磁盘文件 */
    @Transactional
    public void delete(long id) {
        String path = jdbc.sql("SELECT file_path FROM sys_file_info WHERE file_id = :id AND del_flag = 'N'")
                .param("id", id).query(String.class).optional().orElseThrow(ApiException::notFound);
        jdbc.sql("DELETE FROM cms_blog_file WHERE file_id = :id").param("id", id).update();
        jdbc.sql("DELETE FROM sys_notice_file WHERE file_id = :id").param("id", id).update();
        jdbc.sql("DELETE FROM sys_file_info WHERE file_id = :id").param("id", id).update();
        Path file = diskPath(path);
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Failed to delete {}", file, e);
            }
        }
    }

    /** /profile/xxx → UPLOAD_PATH/xxx；不在 UPLOAD_PATH 里的（外链、越界路径）返回 null */
    Path diskPath(String url) {
        if (url == null || !url.startsWith("/profile/") || url.contains("\\") || url.contains("..")) {
            return null;
        }
        try {
            Path file = root.resolve(url.substring("/profile/".length())).normalize();
            return file.startsWith(root) && !file.equals(root) ? file : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** 与旧版一致的大小文本，例如 "1.5 MB" */
    static String sizeInfo(long size) {
        if (size <= 0) {
            return "0kB";
        }
        String[] units = {"B", "kB", "MB", "GB", "TB"};
        int group = Math.min(units.length - 1, (int) (Math.log10(size) / Math.log10(1024)));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, group)) + " " + units[group];
    }
}
