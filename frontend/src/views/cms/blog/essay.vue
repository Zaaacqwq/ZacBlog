<!-- admin note management -->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="Title" prop="title">
        <el-input v-model="queryParams.title" placeholder="Enter title" clearable size="small"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="Status" prop="status">
        <el-select v-model="queryParams.status" placeholder="Select status" clearable size="small">
          <el-option v-for="dict in dict.type.cms_blog_status" :key="dict.value" :label="dict.label"
            :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">Search</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">Reset</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
          v-hasPermi="['cms:blog:add']">Add</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['cms:blog:edit']">Edit</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['cms:blog:remove']">Delete</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
          v-hasPermi="['cms:blog:export']">Export</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="blogList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <!-- <el-table-column label="ID" align="center" prop="id" /> -->
      <el-table-column label="Title" align="center" prop="title" />
      <!-- <el-table-column label="内容" align="center" prop="content" /> -->
      <!-- <el-table-column label="置顶" align="center" prop="top" /> -->
      <!-- <el-table-column label="阅读量" align="center" prop="views" /> -->
      <el-table-column label="Status" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.cms_blog_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="Creator" align="center" prop="createBy" />
      <el-table-column label="Creation Time" align="center" prop="createTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Actions" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:blog:edit']">Edit</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['cms:blog:remove']">Delete</el-button>
          <el-button size="mini" type="text" icon="el-icon-folder-opened" @click="fileList(scope.row)"
            v-hasPermi="['system:notice:query']">Resource List</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改随笔管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" :before-close="cancel" width="1200px" custom-class="note-editor-dialog" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-position="top" class="note-editor-form">
        <el-form-item label="Title" prop="title" class="note-title-field">
          <el-input v-model="form.title" placeholder="Enter title" />
        </el-form-item>
        <el-form-item label="Content" class="note-content-panel">
          <!-- Only Vditor Markdown editor is supported -->
          <el-row>
            <el-col>
              <VditorEditor ref="VditorEditor" v-model="form.contentMarkdown" :height='400' />
            </el-col>
          </el-row>
        </el-form-item>
        <!-- <el-form-item>
          <el-checkbox v-model="top">置顶</el-checkbox>
        </el-form-item> -->
      </el-form>
      <div slot="footer" class="dialog-footer note-editor-footer">
        <el-button class="note-action-btn note-action-btn--publish" type="success" plain @click="releaseForm">Publish</el-button>
        <el-button class="note-action-btn note-action-btn--save" type="warning" plain @click="saveForm">Save</el-button>
        <el-button class="note-action-btn note-action-btn--cancel" type="danger" plain @click="cancel">Cancel</el-button>
      </div>
    </el-dialog>

    <!-- 资源列表对话框 -->
    <el-dialog :title="title" :visible.sync="fileListOpen" width="1000px" append-to-body>
      <el-table class="file-list" :data="fileInfoList">
        <el-table-column type="selection" width="55" align="center" />
        <!-- <el-table-column label="文件主键id" align="center" prop="fileId" /> -->
        <el-table-column label="Image Preview" align="center" prop="pic" >
          <template slot-scope="scope">
            <el-image
                style="width: 120px;height: 60px;"
                :src="scope.row.pic"
                lazy
                :preview-src-list="[scope.row.pic]">
              </el-image>
          </template>
        </el-table-column>
        <el-table-column label="File Name" align="center" prop="fileOriginName" />
        <el-table-column label="File Type" align="center" prop="fileSuffix" />
        <el-table-column label="File Size" align="center" prop="fileSizeInfo" />
        <!-- <el-table-column label="存储文件名称" align="center" prop="fileObjectName" /> -->
        <!-- <el-table-column label="存储路径" align="center" prop="filePath" /> -->
        <!-- <el-table-column label="创建者" align="center" prop="createBy" /> -->
        <!-- <el-table-column label="创建时间" align="center" prop="createTime" width="100">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column> -->
        <el-table-column label="Action" align="center" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-download"
              @click="handleDownload(scope.row)"
            >Download</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script>
  import {
    listBlog,
    getBlog,
    delBlog,
    addBlog,
    updateBlog
  } from "@/api/cms/blog";
  import {
    delFileInfo
  } from "@/api/cms/fileInfo";
  import {
    addFileBlogInfo,
    delFileBlogInfo,
    getFileList
  } from "@/api/cms/fileBlogInfo";
  import {
    Loading
  } from 'element-ui';
  import VditorEditor from '@/components/VditorEditor'

  export default {
    name: "Blog",
    dicts: ['cms_blog_status'],
    components: {
      VditorEditor
    },
    data() {
      return {
        // 遮罩层
        loading: true,
        // 选中数组
        ids: [],
        // 非单个禁用
        single: true,
        // 非多个禁用
        multiple: true,
        // 显示搜索条件
        showSearch: true,
        // 总条数
        total: 0,
        // 随笔管理表格数据
        blogList: [],
        // 资源列表表格数据
        fileInfoList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        fileListOpen: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          title: null,
          type: 2,
          content: null,
          top: null,
          views: null,
          status: null,
          createBy: null
        },
        // 表单参数
        form: {},
        top: false,
        // 表单校验
        rules: {
          title: [{
            required: true,
            message: "Title cannot be empty",
            trigger: "blur"
          }],
          type: [{
            required: true,
            message: "Type cannot be empty",
            trigger: "change"
          }],
        },
        fileIds: [],
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询随笔管理列表 */
      getList() {
        this.loading = true;
        listBlog(this.queryParams).then(response => {
          this.blogList = response.rows;
          this.total = response.total;
          this.loading = false;
        });
      },
      // 取消按钮
      cancel() {
        this.$confirm('Do you want to abandon this edit?', 'System Prompt', {
          confirmButtonText: 'Confirm',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
          let fileids = this.fileIds;
          if (fileids.length > 0) {
            delFileInfo(fileids);
          };
          this.fileIds.length = 0;
          this.top = false;
          this.open = false;
          this.reset();
        }).catch(() => {});
      },
      // 表单重置
      reset() {
        this.form = {
          id: null,
          createBy: null,
          createTime: null,
          updateBy: null,
          updateTime: null,
          title: null,
          type: 2,
          content: null,
          top: "0",
          views: null,
          status: "0",
          contentType: "4",
          contentMarkdown: null
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
        this.ids = selection.map(item => item.id)
        this.single = selection.length !== 1
        this.multiple = !selection.length
      },
      /** 新增按钮操作 */
      handleAdd() {
        this.reset();
        this.open = true;
        this.title = "Add Note";
      },
      /** 修改按钮操作 */
      handleUpdate(row) {
        this.reset();
        const id = row.id || this.ids
        getBlog(id).then(response => {
          this.form = response.data;
          // Force Vditor as the only editor
          this.form.contentType = '4'
          if (!this.form.contentMarkdown && this.form.content) {
            // Fallback: if only HTML/content exists, seed markdown field with it
            this.form.contentMarkdown = this.form.content
          }
          if (this.form.top == 1) {
            this.top = true;
          };
          this.open = true;
          this.title = "Edit Note";
        });
      },
      /** 发布按钮 */
      releaseForm() {
        this.$refs["form"].validate(valid => {
          if (valid) {
            this.$modal.confirm('Are you sure you want to publish?').then(()=>{
              this.form.type = 2;
              this.form.status = 1;
              if (this.top) {
                this.form.top = 1;
              } else {
                this.form.top = 0;
              }
              // Always sync content from the active editor (Vditor)
              this.setFormContent()
              if (this.form.id != null) {
                updateBlog(this.form).then(response => {
                  if(this.fileIds.length>0){
                    let fileBlogInfo = {
                      blogId: this.form.id,
                      fileIds: this.fileIds
                    };
                    addFileBlogInfo(fileBlogInfo).then(response => {});
                  }
                  this.$modal.msgSuccess("Published successfully");
                  this.fileIds.length = 0;
                  this.open = false;
                  this.getList();
                });
              } else {
                addBlog(this.form).then(response => {
                  if(this.fileIds.length>0){
                    let fileBlogInfo = {
                      blogId: response.data,
                      fileIds: this.fileIds
                    };
                    addFileBlogInfo(fileBlogInfo).then(response => {});
                  }
                  this.$modal.msgSuccess("Published successfully");
                  this.fileIds.length = 0;
                  this.open = false;
                  this.getList();
                });
              }
            }).catch(() => {})
          }
        });
      },
      /** 暂存按钮 */
      saveForm() {
        this.$refs["form"].validate(valid => {
          if (valid) {
            this.$modal.confirm('Are you sure you want to save?').then(()=>{
              this.form.type = 2;
              this.form.status = 0;
              if (this.top) {
                this.form.top = 1;
              } else {
                this.form.top = 0;
              }
              // Always sync content from the active editor (Vditor)
              this.setFormContent()
              if (this.form.id != null) {
                updateBlog(this.form).then(response => {
                  if(this.fileIds.length>0){
                    let fileBlogInfo = {
                      blogId: this.form.id,
                      fileIds: this.fileIds
                    };
                    addFileBlogInfo(fileBlogInfo).then(response => {});
                  }
                  this.$modal.msgSuccess("Saved successfully");
                  this.fileIds.length = 0;
                  this.open = false;
                  this.getList();
                });
              } else {
                addBlog(this.form).then(response => {
                  if(this.fileIds.length>0){
                    let fileBlogInfo = {
                      blogId: response.data,
                      fileIds: this.fileIds
                    };
                    addFileBlogInfo(fileBlogInfo).then(response => {});
                  }
                  this.$modal.msgSuccess("Saved successfully");
                  this.fileIds.length = 0;
                  this.open = false;
                  this.getList();
                });
              }
            }).catch(() => {})
          }
        });
      },
      /** 删除按钮操作 */
      handleDelete(row) {
        const ids = row.id || this.ids;
        this.$modal.confirm('Are you sure you want to delete the journal management entry with ID \"' + ids + '\"?').then(function() {
          delFileBlogInfo(ids).then().then(response => {});
          return delBlog(ids);
        }).then(() => {
          this.getList();
          this.$modal.msgSuccess("Deleted successfully");
        }).catch(() => {});
      },
      /** 导出按钮操作 */
      handleExport() {
        this.download('cms/blog/export', {
          ...this.queryParams
        }, `blog_${new Date().getTime()}.xlsx`)
      },
      getFileId(data) {
        this.fileIds.push(data);
      },
      /** 资源列表按钮操作 */
      fileList(row) {
        let loadingInstance = Loading.service({
          target: ".file-list"
        });
        this.reset();
        const blogId = row.id || this.ids
        getFileList(blogId).then(response => {
          for(let i = 0; i<response.data.length; i++){
            let fileInfo = response.data[i];
            switch (fileInfo.fileSuffix){
              case 'png': case 'jpg': case 'jpeg': case 'bmp': case 'gif':
                response.data[i].pic = process.env.VUE_APP_BASE_API + fileInfo.filePath;
                break;
              default:
                response.data[i].pic = image.bg1;
                break;
            };
          };
          this.fileInfoList = response.data;
          this.fileListOpen = true;
          this.title = "Resource List";
          setTimeout(() => {
            loadingInstance.close();
          }, 100);
        });
      },
      // 文件下载处理
      handleDownload(row) {
        var name = row.fileOriginName;
        var url = row.filePath;
        var suffix = url.substring(url.lastIndexOf("."), url.length);
        const a = document.createElement('a')
        a.setAttribute('download', name)
        a.setAttribute('target', '_blank')
        a.setAttribute('href', process.env.VUE_APP_BASE_API + url)
        a.click()
      },
      setFormContent(){
        // Only Vditor: try to get HTML first, fall back to markdown
        if (this.$refs.VditorEditor) {
          try {
            this.form.content = this.$refs.VditorEditor.getHtml() || this.$refs.VditorEditor.getMarkdown()
          } catch (e) {
            this.form.content = this.form.contentMarkdown
          }
        } else {
          this.form.content = this.form.contentMarkdown
        }
      },
    }
  };
</script>

<style scoped lang="scss">
.note-editor-form {
  padding: 8px 6px 0;
}

.note-title-field {
  margin-bottom: 18px;
}

.note-content-panel {
  margin-bottom: 0;
}

.note-editor-form :deep(.el-form-item__label) {
  color: #3f4c61;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
  padding-bottom: 10px;
}

.note-editor-form :deep(.el-input__inner) {
  height: 42px;
  border-radius: 14px;
  border-color: rgba(23, 32, 51, 0.1);
  background: rgba(255, 255, 255, 0.9);
}

.note-editor-form :deep(.el-input__inner:focus) {
  border-color: rgba(174, 123, 50, 0.42);
  box-shadow: 0 0 0 4px rgba(174, 123, 50, 0.12);
}

.note-content-panel :deep(.el-form-item__content) {
  padding: 18px 18px 10px;
  border: 1px solid rgba(23, 32, 51, 0.08);
  border-radius: 28px;
  background: rgba(255, 252, 245, 0.72);
  box-shadow: 0 24px 60px rgba(18, 27, 43, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.note-content-panel :deep(.vditor) {
  box-shadow: none;
}

.note-editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 18px;
}

.note-editor-footer :deep(.el-button) {
  min-width: 104px;
  height: 40px;
  padding: 0 22px;
  border-radius: 14px;
  font-weight: 700;
  letter-spacing: 0.01em;
  box-shadow: 0 10px 20px rgba(18, 27, 43, 0.05);
}

.note-editor-footer :deep(.note-action-btn--publish.el-button--success.is-plain) {
  background: rgba(47, 143, 116, 0.1);
  border-color: rgba(47, 143, 116, 0.18);
  color: #236b56;
}

.note-editor-footer :deep(.note-action-btn--save.el-button--warning.is-plain) {
  background: rgba(254, 193, 113, 0.18);
  border-color: rgba(174, 123, 50, 0.22);
  color: #9a6719;
}

.note-editor-footer :deep(.note-action-btn--cancel.el-button--danger.is-plain) {
  background: rgba(192, 54, 57, 0.08);
  border-color: rgba(192, 54, 57, 0.15);
  color: #b02e33;
}

.note-editor-form :deep(.el-form-item__error) {
  position: static;
  margin-top: 8px;
  line-height: 1.35;
}
</style>

<style lang="scss">
.note-editor-dialog {
  border-radius: 30px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 253, 249, 0.98) 0%, rgba(251, 248, 241, 0.98) 100%);
  box-shadow: 0 40px 100px rgba(18, 27, 43, 0.16);
}

.note-editor-dialog .el-dialog__header {
  padding: 28px 30px 20px;
  border-bottom: 1px solid rgba(23, 32, 51, 0.06);
}

.note-editor-dialog .el-dialog__title {
  color: #172033;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.note-editor-dialog .el-dialog__headerbtn {
  top: 26px;
  right: 28px;
}

.note-editor-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(29, 36, 51, 0.42);
  font-size: 18px;
}

.note-editor-dialog .el-dialog__body {
  padding: 20px 30px 16px;
}

.note-editor-dialog .el-dialog__footer {
  padding: 0 30px 28px;
}
</style>
