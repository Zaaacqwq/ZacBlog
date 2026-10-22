-- V2 新建的黑名单表跟随数据库默认排序规则（MySQL 8 是 utf8mb4_0900_ai_ci），而旧表 cms_comment / cms_message /
-- sys_user 是 utf8mb4_general_ci。两边的列直接比较（例如读者列表里 reader_block.email 与评论邮箱）会报
-- "Illegal mix of collations"。这里把三张黑名单表改成与 cms_comment.author_email 相同的字符集和排序规则：
-- 线上旧库得到 utf8mb4_general_ci；从 V1 建出来的新库旧表本身就是数据库默认值，转换后仍然一致。
-- 只改 V2 新建的表，不动旧表。

SET @blog_charset = (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'author_email');
SET @blog_collation = (SELECT COLLATION_NAME FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'author_email');

SET @ddl = CONCAT('ALTER TABLE reader_block CONVERT TO CHARACTER SET ', @blog_charset, ' COLLATE ', @blog_collation);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = CONCAT('ALTER TABLE ip_block CONVERT TO CHARACTER SET ', @blog_charset, ' COLLATE ', @blog_collation);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = CONCAT('ALTER TABLE keyword_block CONVERT TO CHARACTER SET ', @blog_charset, ' COLLATE ', @blog_collation);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
