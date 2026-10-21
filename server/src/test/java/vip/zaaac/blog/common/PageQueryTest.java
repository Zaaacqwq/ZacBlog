package vip.zaaac.blog.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void offsetDoesNotOverflowForHugePageNumbers() {
        assertThat(PageQuery.of(Integer.MAX_VALUE, 100).offset()).isEqualTo((Integer.MAX_VALUE - 1L) * 100);
        assertThat(PageQuery.of(1, 10).offset()).isZero();
    }

    @Test
    void clampsInvalidValues() {
        PageQuery q = PageQuery.of(-5, 1000);
        assertThat(q.pageNum()).isEqualTo(1);
        assertThat(q.pageSize()).isEqualTo(100);
        assertThat(PageQuery.of(null, 0).pageSize()).isEqualTo(10);
    }
}
