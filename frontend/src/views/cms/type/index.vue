<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="Category Name" prop="typeName">
        <el-input v-model="queryParams.typeName" placeholder="Enter category name" clearable size="small"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">Search</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">Reset</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
          v-hasPermi="['cms:type:add']">Add</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['cms:type:edit']">Edit</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['cms:type:remove']">Delete</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
          v-hasPermi="['cms:type:export']">Export</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="typeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <!-- <el-table-column label="分类ID" align="center" prop="typeId" /> -->
      <el-table-column label="Category Image" align="center" prop="typePic" width="130">
        <template slot-scope="scope">
          <div class="category-image-cell" v-if="scope.row.typePicType == '0'">
            <el-image style="width: 28px;height: 28px; border-radius: 50%;" :src="scope.row.typePicLink || defaultCategoryIcon" lazy :preview-src-list="[scope.row.typePicLink || defaultCategoryIcon]">
              <div slot="error" class="image-slot">
                <img :src="defaultCategoryIcon" alt="category" class="category-fallback-icon">
              </div>
            </el-image>
          </div>
          <div class="category-image-cell" v-if="scope.row.typePicType == '1'">
            <el-image style="width: 28px;height: 28px; border-radius: 50%;" :src="scope.row.typePic || defaultCategoryIcon" lazy :preview-src-list="[scope.row.typePic || defaultCategoryIcon]">
              <div slot="error" class="image-slot">
                <img :src="defaultCategoryIcon" alt="category" class="category-fallback-icon">
              </div>
            </el-image>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Category Name" align="center" prop="typeName" />
      <el-table-column label="Blog Count" align="center" prop="blogNum" />
      <el-table-column label="Creator" align="center" prop="createBy" />
      <el-table-column label="Creation Time" align="center" prop="createTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Actions" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:type:edit']">Edit</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['cms:type:remove']">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改分类管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" :before-close="cancel" width="620px" custom-class="category-editor-dialog" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-position="top" class="category-editor-form">
        <el-form-item label="Category Name" prop="typeName" class="category-title-field">
          <el-input v-model="form.typeName" placeholder="Enter category name" />
        </el-form-item>
        <el-form-item label="Category Image" class="category-panel category-image-panel">
          <div class="category-image-intro">Pick an icon-style image for the category card and list view.</div>
          <el-radio-group v-model="form.typePicType">
            <el-radio-button label="0">URL</el-radio-button>
            <el-radio-button label="1">Upload</el-radio-button>
          </el-radio-group>
          <div v-show="form.typePicType == '0'" class="tabBlock">
            <el-input v-model="form.typePicLink" :placeholder="defaultCategoryPlaceholder" class="category-image-input" />
            <div class="category-image-preview-card">
              <el-image :src="form.typePicLink || defaultCategoryIcon" :preview-src-list="[form.typePicLink || defaultCategoryIcon]" fit="cover" class="typePic" >
                <div slot="error" class="image-slot">
                  <img :src="defaultCategoryIcon" alt="category" class="category-fallback-icon category-fallback-icon--large">
                </div>
              </el-image>
            </div>
          </div>
          <div v-show="form.typePicType == '1'" class="tabBlock">
            <div class="category-upload-surface">
              <imageUpload v-model="form.typePic" :limit="1" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer category-editor-footer">
        <el-button class="category-action-btn category-action-btn--confirm" type="success" plain @click="submitForm">Confirm</el-button>
        <el-button class="category-action-btn category-action-btn--cancel" type="danger" plain @click="cancel">Cancel</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
  const defaultCategoryIcon = `data:image/svg+xml;utf8,${encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%235a6474"><path d="M9 2.00318V2H19.9978C20.5513 2 21 2.45531 21 2.9918V21.0082C21 21.556 20.5551 22 20.0066 22H3.9934C3.44476 22 3 21.5501 3 20.9932V8L9 2.00318ZM5.82918 8H9V4.83086L5.82918 8ZM11 4V9C11 9.55228 10.5523 10 10 10H5V20H19V4H11Z"/></svg>')}`;
  const defaultCategoryPlaceholder = "Default category icon (file-line.svg)";
  import {
    listType,
    getType,
    delType,
    addType,
    updateType,
    cancelType
  } from "@/api/cms/type";

  export default {
    name: "Type",
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
        // 分类管理表格数据
        typeList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          typeName: null,
          typePic: null,
          createBy: null
        },
        // 表单参数
        form: {},
        // 表单校验
        rules: {
          typeName: [{
            required: true,
            message: "Category name cannot be empty",
            trigger: "blur"
          }],
        },
        defaultCategoryIcon,
        defaultCategoryPlaceholder
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询分类管理列表 */
      getList() {
        this.loading = true;
        listType(this.queryParams).then(response => {
          for (let i = 0; i < response.rows.length; i++) {
            let typeInfo = response.rows[i];
            if (typeInfo.typePic.length > 0) {
              response.rows[i].typePic = process.env.VUE_APP_BASE_API + typeInfo.typePic
            }
          };
          this.typeList = response.rows;
          this.total = response.total;
          this.loading = false;
        });
      },
      // 取消按钮
      cancel() {
            cancelType(this.form).then(response => {
              this.open = false;
              this.reset();
            });
      },
      // 表单重置
      reset() {
        this.form = {
          typeId: null,
          createBy: null,
          createTime: null,
          updateBy: null,
          updateTime: null,
          typeName: null,
          typePicType: '0',
          typePic: null,
          typePicLink: null
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
        this.ids = selection.map(item => item.typeId)
        this.names = selection.map(item => item.typeName)
        this.single = selection.length !== 1
        this.multiple = !selection.length
      },
      /** 新增按钮操作 */
      handleAdd() {
        this.reset();
        this.open = true;
        this.title = "Add Category Management";
      },
      /** 修改按钮操作 */
      handleUpdate(row) {
        this.reset();
        const typeId = row.typeId || this.ids
        getType(typeId).then(response => {
          this.form = response.data;
          this.open = true;
          this.title = "Edit Category Management";
        });
      },
      /** 提交按钮 */
      submitForm() {
        this.$refs["form"].validate(valid => {
          if (valid) {
            if (this.form.typePicType === '0' && !this.form.typePicLink) {
              this.form.typePicLink = defaultCategoryIcon;
            }
            if (this.form.typeId != null) {
              updateType(this.form).then(response => {
                this.$modal.msgSuccess("Update successful");
                this.open = false;
                this.getList();
              });
            } else {
              addType(this.form).then(response => {
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
        const typeIds = row.typeId || this.ids;
        let name = row.typeName || this.names;
        this.$modal.confirm('Are you sure you want to delete"' + name + '"?').then(function() {
          return delType(typeIds);
        }).then(() => {
          this.getList();
          this.$modal.msgSuccess("Deletion successful");
        }).catch(() => {});
      },
      /** 导出按钮操作 */
      handleExport() {
        this.download('cms/type/export', {
          ...this.queryParams
        }, `type_${new Date().getTime()}.xlsx`)
      }
    }
  };
</script>

<style scoped lang="scss">
  .category-editor-form {
    padding: 8px 6px 0;
  }

  .category-title-field {
    margin-bottom: 18px;
  }

  .category-panel :deep(.el-form-item__content) {
    padding: 18px 18px 14px;
    border: 1px solid rgba(23, 32, 51, 0.08);
    border-radius: 28px;
    background: rgba(255, 252, 245, 0.72);
    box-shadow: 0 24px 60px rgba(18, 27, 43, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.72);
  }

  .category-image-intro {
    margin-bottom: 14px;
    color: rgba(29, 36, 51, 0.52);
    font-size: 13px;
    line-height: 1.6;
  }

  .tabBlock {
    min-height: 180px;
    margin-top: 18px;
  }

  .category-image-input {
    margin-bottom: 14px;
  }

  .category-image-preview-card {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 88px;
    height: 88px;
    border-radius: 24px;
    background: rgba(245, 246, 247, 0.82);
    border: 1px solid rgba(23, 32, 51, 0.06);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  }

  .category-image-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 28px;
  }

  .category-upload-surface {
    padding: 12px;
    border-radius: 20px;
    background: rgba(245, 246, 247, 0.72);
    border: 1px dashed rgba(23, 32, 51, 0.12);
  }

  .typePic {
    width: 42px;
    height: 42px;
    border-radius: 16px;
  }

  .category-fallback-icon {
    width: 18px;
    height: 18px;
    object-fit: contain;
    opacity: 0.9;
  }

  .category-fallback-icon--large {
    width: 42px;
    height: 42px;
  }

  .category-editor-form :deep(.el-form-item__label) {
    color: #3f4c61;
    font-size: 13px;
    font-weight: 700;
    line-height: 1.2;
    padding-bottom: 10px;
  }

  .category-editor-form :deep(.el-input__inner) {
    height: 42px;
    border-radius: 14px;
    border-color: rgba(23, 32, 51, 0.1);
    background: rgba(255, 255, 255, 0.9);
  }

  .category-editor-form :deep(.el-input__inner:focus) {
    border-color: rgba(174, 123, 50, 0.42);
    box-shadow: 0 0 0 4px rgba(174, 123, 50, 0.12);
  }

  .category-image-panel :deep(.el-radio-group) {
    display: inline-flex;
    padding: 4px;
    background: rgba(23, 32, 51, 0.06);
    border-radius: 14px;
  }

  .category-image-panel :deep(.el-radio-button__inner) {
    border: none;
    border-radius: 10px;
    box-shadow: none !important;
    background: transparent;
    color: #5a6474;
  }

  .category-image-panel :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
    background: rgba(255, 255, 255, 0.96);
    color: #1d2433;
  }

  .category-editor-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding-top: 18px;
  }

  .category-editor-footer :deep(.el-button) {
    min-width: 108px;
    height: 40px;
    padding: 0 22px;
    border-radius: 14px;
    font-weight: 700;
    letter-spacing: 0.01em;
    box-shadow: 0 10px 20px rgba(18, 27, 43, 0.05);
  }

  .category-editor-footer :deep(.category-action-btn--confirm.el-button--success.is-plain) {
    background: rgba(47, 143, 116, 0.1);
    border-color: rgba(47, 143, 116, 0.18);
    color: #236b56;
  }

  .category-editor-footer :deep(.category-action-btn--cancel.el-button--danger.is-plain) {
    background: rgba(192, 54, 57, 0.08);
    border-color: rgba(192, 54, 57, 0.15);
    color: #b02e33;
  }

  .category-editor-form :deep(.el-form-item__error) {
    position: static;
    margin-top: 8px;
    line-height: 1.35;
  }
</style>

<style lang="scss">
  .category-editor-dialog {
    border-radius: 30px;
    overflow: hidden;
    background: linear-gradient(180deg, rgba(255, 253, 249, 0.98) 0%, rgba(251, 248, 241, 0.98) 100%);
    box-shadow: 0 40px 100px rgba(18, 27, 43, 0.16);
  }

  .category-editor-dialog .el-dialog__header {
    padding: 28px 30px 20px;
    border-bottom: 1px solid rgba(23, 32, 51, 0.06);
  }

  .category-editor-dialog .el-dialog__title {
    color: #172033;
    font-size: 22px;
    font-weight: 800;
    letter-spacing: -0.02em;
  }

  .category-editor-dialog .el-dialog__headerbtn {
    top: 26px;
    right: 28px;
  }

  .category-editor-dialog .el-dialog__headerbtn .el-dialog__close {
    color: rgba(29, 36, 51, 0.42);
    font-size: 18px;
  }

  .category-editor-dialog .el-dialog__body {
    padding: 20px 30px 16px;
  }

  .category-editor-dialog .el-dialog__footer {
    padding: 0 30px 28px;
  }
</style>
