<template>
  <div :class="{'hidden':hidden}" class="pagination-container">
    <el-pagination
      :background="background"
      :current-page.sync="currentPage"
      :page-size.sync="pageSize"
      :layout="layout"
      :page-sizes="pageSizes"
      :pager-count="pagerCount"
      :total="total"
      v-bind="$attrs"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script>
import { scrollTo } from '@/utils/scroll-to'

export default {
  name: 'Pagination',
  props: {
    total: {
      required: true,
      type: Number
    },
    page: {
      type: Number,
      default: 1
    },
    limit: {
      type: Number,
      default: 20
    },
    pageSizes: {
      type: Array,
      default() {
        return [10, 20, 30, 50]
      }
    },
    // 移动端页码按钮的数量端默认值5
    pagerCount: {
      type: Number,
      default: document.body.clientWidth < 992 ? 5 : 7
    },
    layout: {
      type: String,
      default: 'total, sizes, prev, pager, next, jumper'
    },
    background: {
      type: Boolean,
      default: true
    },
    autoScroll: {
      type: Boolean,
      default: true
    },
    hidden: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    currentPage: {
      get() {
        return this.page
      },
      set(val) {
        this.$emit('update:page', val)
      }
    },
    pageSize: {
      get() {
        return this.limit
      },
      set(val) {
        this.$emit('update:limit', val)
      }
    }
  },
  methods: {
    handleSizeChange(val) {
      this.$emit('pagination', { page: this.currentPage, limit: val })
      if (this.autoScroll) {
        scrollTo(0, 800)
      }
    },
    handleCurrentChange(val) {
      this.$emit('pagination', { page: val, limit: this.pageSize })
      if (this.autoScroll) {
        scrollTo(0, 800)
      }
    }
  }
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  width: 100%;
  margin-top: 20px;
  padding: 0;
  background: transparent;
  overflow-x: auto;
}

.pagination-container ::v-deep .el-pagination {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  margin-left: auto;
  gap: 10px;
  row-gap: 12px;
  color: rgba(29, 36, 51, 0.6);
  font-weight: 500;
}

.pagination-container ::v-deep .btn-prev,
.pagination-container ::v-deep .btn-next,
.pagination-container ::v-deep .el-pager li,
.pagination-container ::v-deep .el-pagination__jump .el-input__inner,
.pagination-container ::v-deep .el-pagination__sizes .el-input__inner {
  border: 1px solid rgba(29, 36, 51, 0.07);
  border-radius: 14px !important;
  background: #f5f5f5 !important;
  color: #1d2433 !important;
  box-shadow: none !important;
}

.pagination-container ::v-deep .el-pager li {
  min-width: 36px;
  height: 36px;
  line-height: 34px;
  margin: 0 2px;
}

.pagination-container ::v-deep .btn-prev,
.pagination-container ::v-deep .btn-next {
  width: 36px;
  height: 36px;
  padding: 0;
}

.pagination-container ::v-deep .el-pager li.active {
  background: #e8e8e8 !important;
  border-color: rgba(29, 36, 51, 0.1);
  color: #111827 !important;
  font-weight: 700;
}

.pagination-container ::v-deep .btn-prev:hover,
.pagination-container ::v-deep .btn-next:hover,
.pagination-container ::v-deep .el-pager li:hover,
.pagination-container ::v-deep .el-pagination__jump .el-input__inner:hover,
.pagination-container ::v-deep .el-pagination__sizes .el-input__inner:hover {
  background: #eeeeee !important;
}

.pagination-container.hidden {
  display: none;
}
</style>
