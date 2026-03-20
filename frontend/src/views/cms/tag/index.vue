<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="Tag Name" prop="tagName">
        <el-input
          v-model="queryParams.tagName"
          placeholder="Enter tag name"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">Search</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">Reset</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['cms:tag:add']"
        >Add</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['cms:tag:edit']"
        >Edit</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cms:tag:remove']"
        >Delete</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['cms:tag:export']"
        >Export</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="tagList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <!-- <el-table-column label="标签ID" align="center" prop="tagId" /> -->
      <el-table-column label="Tag Name" align="center" prop="tagName" />
      <el-table-column label="Blog Count" align="center" prop="blogNum" />
      <el-table-column label="Creator" align="center" prop="createBy" />
      <el-table-column label="Creation Time" align="center" prop="createTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Actions" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:tag:edit']"
          >Edit</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['cms:tag:remove']"
          >Delete</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改标签管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="560px" custom-class="tag-editor-dialog" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-position="top" class="tag-editor-form">
        <el-form-item label="Tag Name" prop="tagName" class="tag-title-field">
          <div class="tag-editor-hint">Create a short, scannable label that works well in blog cards and filters.</div>
          <el-input v-model="form.tagName" placeholder="Enter tag name" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer tag-editor-footer">
        <el-button class="tag-action-btn tag-action-btn--confirm" type="success" plain @click="submitForm">Confirm</el-button>
        <el-button class="tag-action-btn tag-action-btn--cancel" type="danger" plain @click="cancel">Cancel</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTag, getTag, delTag, addTag, updateTag } from "@/api/cms/tag";

export default {
  name: "Tag",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      names: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 标签管理表格数据
      tagList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tagName: null,
        createBy: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        tagName: [
          { required: true, message: "Tag name cannot be empty", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询标签管理列表 */
    getList() {
      this.loading = true;
      listTag(this.queryParams).then(response => {
        this.tagList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        tagId: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        tagName: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.tagId)
      this.names = selection.map(item => item.tagName)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "Add Tag Management";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const tagId = row.tagId || this.ids
      getTag(tagId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "Edit Tag Management";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.tagId != null) {
            updateTag(this.form).then(response => {
              this.$modal.msgSuccess("Update successful");
              this.open = false;
              this.getList();
            });
          } else {
            addTag(this.form).then(response => {
              this.$modal.msgSuccess("Addition successful");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const tagIds = row.tagId || this.ids;
      let name = row.tagName || this.names;
      this.$modal.confirm('Are you sure you want to delete the tag"' + name + '"?').then(function() {
        return delTag(tagIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("Deletion successful");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('cms/tag/export', {
        ...this.queryParams
      }, `tag_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

<style scoped lang="scss">
.tag-editor-form {
  padding: 8px 6px 0;
}

.tag-title-field :deep(.el-form-item__content) {
  padding: 18px 18px 14px;
  border: 1px solid rgba(23, 32, 51, 0.08);
  border-radius: 28px;
  background: rgba(255, 252, 245, 0.72);
  box-shadow: 0 24px 60px rgba(18, 27, 43, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.tag-editor-hint {
  margin-bottom: 14px;
  color: rgba(29, 36, 51, 0.52);
  font-size: 13px;
  line-height: 1.6;
}

.tag-editor-form :deep(.el-form-item__label) {
  color: #3f4c61;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
  padding-bottom: 10px;
}

.tag-editor-form :deep(.el-input__inner) {
  height: 42px;
  border-radius: 14px;
  border-color: rgba(23, 32, 51, 0.1);
  background: rgba(255, 255, 255, 0.9);
}

.tag-editor-form :deep(.el-input__inner:focus) {
  border-color: rgba(174, 123, 50, 0.42);
  box-shadow: 0 0 0 4px rgba(174, 123, 50, 0.12);
}

.tag-editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 18px;
}

.tag-editor-footer :deep(.el-button) {
  min-width: 108px;
  height: 40px;
  padding: 0 22px;
  border-radius: 14px;
  font-weight: 700;
  letter-spacing: 0.01em;
  box-shadow: 0 10px 20px rgba(18, 27, 43, 0.05);
}

.tag-editor-footer :deep(.tag-action-btn--confirm.el-button--success.is-plain) {
  background: rgba(47, 143, 116, 0.1);
  border-color: rgba(47, 143, 116, 0.18);
  color: #236b56;
}

.tag-editor-footer :deep(.tag-action-btn--cancel.el-button--danger.is-plain) {
  background: rgba(192, 54, 57, 0.08);
  border-color: rgba(192, 54, 57, 0.15);
  color: #b02e33;
}

.tag-editor-form :deep(.el-form-item__error) {
  position: static;
  margin-top: 8px;
  line-height: 1.35;
}
</style>

<style lang="scss">
.tag-editor-dialog {
  border-radius: 30px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 253, 249, 0.98) 0%, rgba(251, 248, 241, 0.98) 100%);
  box-shadow: 0 40px 100px rgba(18, 27, 43, 0.16);
}

.tag-editor-dialog .el-dialog__header {
  padding: 28px 30px 20px;
  border-bottom: 1px solid rgba(23, 32, 51, 0.06);
}

.tag-editor-dialog .el-dialog__title {
  color: #172033;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.tag-editor-dialog .el-dialog__headerbtn {
  top: 26px;
  right: 28px;
}

.tag-editor-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(29, 36, 51, 0.42);
  font-size: 18px;
}

.tag-editor-dialog .el-dialog__body {
  padding: 20px 30px 16px;
}

.tag-editor-dialog .el-dialog__footer {
  padding: 0 30px 28px;
}
</style>
