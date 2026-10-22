-- 后台：读者管理按邮箱聚合、评论按读者筛选。只新增索引，不改动已有列和表。

CREATE INDEX idx_comment_author_email ON cms_comment (author_email);
CREATE INDEX idx_message_author_email ON cms_message (author_email);
