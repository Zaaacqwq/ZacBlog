package vip.zaaac.blog.comment;

/** 评论（挂在文章下）和留言板共用一套逻辑，只是表不同。表名是常量，不来自用户输入 */
enum Board {
    COMMENT("cms_comment", "cms_comment_like", "comment_id"),
    MESSAGE("cms_message", "cms_message_like", "message_id");

    final String table;
    final String likeTable;
    final String likeColumn;

    Board(String table, String likeTable, String likeColumn) {
        this.table = table;
        this.likeTable = likeTable;
        this.likeColumn = likeColumn;
    }
}
