package vip.zaaac.blog.comment;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 前台评论/留言。顶层是楼层，children 是这一楼里的所有回复（按时间正序），
 * replyToNickname 是被回复那条的昵称。邮箱和 IP 不对外返回。
 */
public record CommentDto(
        long id,
        Long parentId,
        String nickname,
        String website,
        String avatarUrl,
        String content,
        long likeCount,
        boolean fromAdmin,
        String replyToNickname,
        OffsetDateTime createdAt,
        List<CommentDto> children) {

    CommentDto withReplyTo(String nickname) {
        return new CommentDto(id, parentId, this.nickname, website, avatarUrl, content, likeCount, fromAdmin,
                nickname, createdAt, children);
    }

    static CommentDto withChildren(CommentDto c) {
        return new CommentDto(c.id, c.parentId, c.nickname, c.website, c.avatarUrl, c.content, c.likeCount,
                c.fromAdmin, c.replyToNickname, c.createdAt, new ArrayList<>());
    }
}
