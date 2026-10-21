-- 基线：旧库（zaaac_blog.sql）中 v2 仍在使用的表，结构与旧库一致。
-- 线上库用 baseline-on-migrate + baseline-version=1，不会执行本脚本；只有空库（新环境、H2 测试）才会执行。
-- 必须同时兼容 MySQL 8 和 H2 MySQL 模式：不写字符集/引擎/注释，索引单独创建。
-- 旧库中的 Top 列在 H2 里是关键字，统一用反引号小写 `top` 引用（MySQL 列名不区分大小写）。

CREATE TABLE cms_blog (
  id bigint NOT NULL AUTO_INCREMENT,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  title varchar(50) NOT NULL,
  type char(1) NOT NULL,
  content_type char(1) NOT NULL,
  content longblob,
  content_markdown longblob,
  `top` char(1) DEFAULT '0',
  views int DEFAULT 0,
  status char(1) DEFAULT '0',
  blog_pic_type char(1) DEFAULT '0',
  blog_pic varchar(100) DEFAULT '',
  blog_pic_link varchar(100) DEFAULT '',
  blog_desc varchar(100) DEFAULT '',
  blog_files text,
  PRIMARY KEY (id)
);

CREATE TABLE cms_type (
  type_id bigint NOT NULL AUTO_INCREMENT,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  type_name varchar(50) NOT NULL,
  type_pic varchar(100) DEFAULT '',
  type_pic_type char(1) DEFAULT '0',
  type_pic_link varchar(100) DEFAULT '',
  PRIMARY KEY (type_id)
);

CREATE TABLE cms_tag (
  tag_id bigint NOT NULL AUTO_INCREMENT,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  tag_name varchar(50) NOT NULL,
  PRIMARY KEY (tag_id)
);

CREATE TABLE cms_blog_tag (
  tag_id bigint NOT NULL,
  blog_id bigint NOT NULL,
  PRIMARY KEY (tag_id, blog_id),
  CONSTRAINT fk_btag_blog FOREIGN KEY (blog_id) REFERENCES cms_blog (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_btag_tag FOREIGN KEY (tag_id) REFERENCES cms_tag (tag_id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX idx_btag_blog ON cms_blog_tag (blog_id);
CREATE INDEX idx_btag_tag ON cms_blog_tag (tag_id);

CREATE TABLE cms_blog_type (
  type_id bigint NOT NULL,
  blog_id bigint NOT NULL,
  PRIMARY KEY (type_id, blog_id),
  CONSTRAINT fk_bt_blog FOREIGN KEY (blog_id) REFERENCES cms_blog (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_bt_type FOREIGN KEY (type_id) REFERENCES cms_type (type_id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX idx_bt_blog ON cms_blog_type (blog_id);
CREATE INDEX idx_bt_type ON cms_blog_type (type_id);

CREATE TABLE cms_blog_file (
  file_id bigint NOT NULL,
  blog_id bigint NOT NULL,
  PRIMARY KEY (file_id, blog_id)
);

CREATE TABLE cms_comment (
  id bigint NOT NULL AUTO_INCREMENT,
  parent_id bigint DEFAULT NULL,
  main_id bigint DEFAULT NULL,
  like_num int DEFAULT NULL,
  content varchar(255) DEFAULT NULL,
  type char(1) DEFAULT NULL,
  blog_id bigint DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  user_id bigint DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE cms_comment_like (
  comment_id bigint NOT NULL,
  user_id bigint NOT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (comment_id, user_id)
);

CREATE TABLE cms_message (
  id bigint NOT NULL AUTO_INCREMENT,
  parent_id bigint DEFAULT NULL,
  main_id bigint DEFAULT NULL,
  like_num int DEFAULT NULL,
  content varchar(255) DEFAULT NULL,
  type char(1) DEFAULT NULL,
  blog_id bigint DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  user_id bigint DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE cms_message_like (
  message_id bigint NOT NULL,
  user_id bigint NOT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (message_id, user_id)
);

CREATE TABLE sys_notice (
  notice_id int NOT NULL AUTO_INCREMENT,
  notice_title varchar(50) NOT NULL,
  notice_type char(1) NOT NULL,
  notice_content longblob,
  status char(1) DEFAULT '0',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (notice_id)
);

CREATE TABLE sys_notice_file (
  file_id bigint NOT NULL,
  notice_id bigint NOT NULL,
  PRIMARY KEY (file_id, notice_id)
);

CREATE TABLE sys_file_info (
  file_id bigint NOT NULL AUTO_INCREMENT,
  file_origin_name varchar(255) NOT NULL,
  file_suffix varchar(255) DEFAULT NULL,
  file_size_info varchar(255) DEFAULT NULL,
  file_object_name varchar(255) DEFAULT NULL,
  file_path varchar(255) DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT 'N',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (file_id)
);

-- 旧 sys_user 的子集：只建 v2 用到的列（管理员登录、旧读者评论的昵称和头像）。
-- 线上库保留原来的全部列，不受影响。
CREATE TABLE sys_user (
  user_id bigint NOT NULL AUTO_INCREMENT,
  user_name varchar(30) NOT NULL,
  nick_name varchar(30) NOT NULL,
  email varchar(50) DEFAULT '',
  avatar varchar(100) DEFAULT '',
  password varchar(100) DEFAULT '',
  status char(1) DEFAULT '0',
  del_flag char(1) DEFAULT '0',
  login_ip varchar(128) DEFAULT '',
  login_date datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (user_id)
);
