-- 匿名评论/留言 + 黑名单。只新增可空列和新表，不改动已有列：旧 jar 回滚后照常可用。

ALTER TABLE cms_comment ADD COLUMN author_name varchar(64) DEFAULT NULL;
ALTER TABLE cms_comment ADD COLUMN author_email varchar(128) DEFAULT NULL;
ALTER TABLE cms_comment ADD COLUMN author_website varchar(255) DEFAULT NULL;
ALTER TABLE cms_comment ADD COLUMN author_ip varchar(64) DEFAULT NULL;
ALTER TABLE cms_comment ADD COLUMN user_agent varchar(255) DEFAULT NULL;

ALTER TABLE cms_message ADD COLUMN author_name varchar(64) DEFAULT NULL;
ALTER TABLE cms_message ADD COLUMN author_email varchar(128) DEFAULT NULL;
ALTER TABLE cms_message ADD COLUMN author_website varchar(255) DEFAULT NULL;
ALTER TABLE cms_message ADD COLUMN author_ip varchar(64) DEFAULT NULL;
ALTER TABLE cms_message ADD COLUMN user_agent varchar(255) DEFAULT NULL;

-- 匿名点赞：user_id 存由 IP 推出的负数标识（不会与真实用户 id 冲突），voter_ip 便于排查
ALTER TABLE cms_comment_like ADD COLUMN voter_ip varchar(64) DEFAULT NULL;
ALTER TABLE cms_message_like ADD COLUMN voter_ip varchar(64) DEFAULT NULL;

CREATE TABLE reader_block (
  id bigint NOT NULL AUTO_INCREMENT,
  email varchar(128) NOT NULL,
  reason varchar(255) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_reader_block_email UNIQUE (email)
);

CREATE TABLE ip_block (
  id bigint NOT NULL AUTO_INCREMENT,
  ip varchar(64) NOT NULL,
  reason varchar(255) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_ip_block_ip UNIQUE (ip)
);

CREATE TABLE keyword_block (
  id bigint NOT NULL AUTO_INCREMENT,
  keyword varchar(64) NOT NULL,
  reason varchar(255) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_keyword_block_keyword UNIQUE (keyword)
);
