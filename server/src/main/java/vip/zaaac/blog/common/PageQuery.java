package vip.zaaac.blog.common;

/** 分页参数：pageNum 从 1 开始，pageSize 默认 10、最大 100 */
public record PageQuery(int pageNum, int pageSize) {

    public PageQuery {
        pageNum = Math.max(pageNum, 1);
        pageSize = pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    public static PageQuery of(Integer pageNum, Integer pageSize) {
        return new PageQuery(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
    }

    public int offset() {
        return (pageNum - 1) * pageSize;
    }
}
