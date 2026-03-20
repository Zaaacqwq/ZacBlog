<!-- admin blog management -->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="Title" prop="title">
        <el-input v-model="queryParams.title" placeholder="Enter title" clearable size="small"
          @clear="handleQuery"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="Status" prop="status">
        <el-select v-model="queryParams.status" placeholder="Select status" clearable size="small"
          @change="handleQuery" @clear="handleQuery">
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

    <el-table v-loading="loading" :data="blogList" :empty-text="tableEmptyText" @selection-change="handleSelectionChange"
      :row-class-name="tableRowClassName">
      <el-table-column type="selection" width="55" align="center" />
      <!-- <el-table-column label="ID" align="center" prop="id" /> -->
      <el-table-column label="Cover Image Preview" align="center" prop="blogPic" >
        <template slot-scope="scope">
          <el-image
            v-if="scope.row.blogPicType == '0'"
            style="width: 120px;height: 60px;"
            :src="scope.row.blogPicLink"
            lazy
            :preview-src-list="[scope.row.blogPicLink]">
              <div slot="error" class="image-slot">
                <el-image src="/errorImg.jpg" fit="cover" class="blogPic"></el-image>
              </div>
          </el-image>
          <el-image
            v-if="scope.row.blogPicType == '1'"
            style="width: 120px;height: 60px;"
            :src="scope.row.blogPic"
            lazy
            :preview-src-list="[scope.row.blogPic]">
          </el-image>
        </template>
      </el-table-column>
      <el-table-column label="Title" align="center" prop="title" />
      <!-- <el-table-column label="内容" align="center" prop="content" /> -->
      <!-- <el-table-column label="置顶" align="center" prop="top" /> -->
      <el-table-column label="Category" align="center" prop="types">
        <template slot-scope="scope">
          <el-tag size="mini" v-for="tag in scope.row.types" :key="tag.typeId" type="info">{{tag.typeName}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Tags" align="center" prop="tags">
        <template slot-scope="scope">
          <el-tag effect="plain" size="mini" v-for="tag in scope.row.tags" :key="tag.tagId" type="success">
            {{tag.tagName}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Views" align="center" prop="views" />
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
            <el-button size="mini" type="text" icon="el-icon-folder-opened" @click="blogFiles(scope.row)"
              v-hasPermi="['cms:blog:edit']">Manage Attachments</el-button>
          <!-- <el-button size="mini" type="text" icon="el-icon-folder-opened" @click="fileList(scope.row)"
            v-hasPermi="['cms:blog:edit']">资源列表</el-button> -->
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改文章管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" :before-close="cancel" width="1200px" custom-class="blog-editor-dialog" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-position="top" class="blog-editor-form">
        <el-form-item label="Title" prop="title" class="editor-title-field">
          <el-input v-model="form.title" placeholder="Enter title" />
        </el-form-item>
        <el-row :gutter="20" class="editor-meta-grid">
          <el-col :span="8">
            <el-form-item label="Cover Image" class="editor-panel editor-cover-panel">
              <el-radio-group v-model="form.blogPicType">
                <el-radio-button label="0">Url</el-radio-button>
                <el-radio-button label="1">Upload</el-radio-button>
              </el-radio-group>
              <div v-show="form.blogPicType == '0'" class="tabBlock">
                <el-input v-model="form.blogPicLink" placeholder="Enter image URL https://" style="margin-bottom: 10px;" />
                <el-image :src="form.blogPicLink" :preview-src-list="[form.blogPicLink]" fit="cover" class="blogPic" >
                  <div slot="error" class="image-slot">
                    <el-image src="/errorImg.jpg" fit="cover" class="blogPic"></el-image>
                  </div>
                </el-image>
              </div>
              <div v-show="form.blogPicType == '1'" class="tabBlock">
                <imageUpload v-model="form.blogPic" :limit="1" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="16">
        <el-form-item label="Description" class="editor-panel editor-description-panel">
          <div class="editor-comment-hint">Write a short summary for the post.</div>
          <div class="description-surface">
            <textarea
              v-model="form.blogDesc"
              class="description-textarea"
              maxlength="50"
              placeholder="Enter Description"
            />
            <div class="description-count">{{ (form.blogDesc || '').length }}/50</div>
          </div>
        </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Content" class="editor-content-panel">
          <!-- Only Vditor Markdown editor is supported -->
          <el-row>
            <el-col>
              <VditorEditor ref="VditorEditor" v-model="form.contentMarkdown" :height='400' />
            </el-col>
          </el-row>
        </el-form-item>
        <el-row :gutter="20" class="editor-bottom-grid editor-meta-bottom-grid">
          <el-col :span="12">
            <el-form-item label="Tags" class="editor-panel editor-tags-panel">
              <el-select v-model="form.tagIds" multiple filterable clearable collapse-tags reserve-keyword
                placeholder="Search and add tags">
                <el-option v-for="item in tagOptions" :key="item.tagId" :label="item.tagName" :value="item.tagId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Category" class="editor-panel editor-category-panel">
              <el-select v-model="form.typeIds" multiple filterable clearable collapse-tags reserve-keyword
                placeholder="Search and add categories">
                <el-option v-for="item in typeOptions" :key="item.typeId" :label="item.typeName" :value="item.typeId">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="editor-page-actions">
          <button type="button" class="pin-toggle" :class="{ 'is-active': top }" @click="top = !top">
            <span class="pin-toggle__dot"></span>
            <span class="pin-toggle__label">{{ top ? 'Pinned' : 'Pin Post' }}</span>
          </button>
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer editor-footer">
        <el-button class="editor-action-btn editor-action-btn--publish" type="success" plain @click="releaseForm">Publish</el-button>
        <el-button class="editor-action-btn editor-action-btn--save" type="warning" plain @click="saveForm">Save</el-button>
        <el-button class="editor-action-btn editor-action-btn--cancel" type="danger" plain @click="cancel">Cancel</el-button>
      </div>
    </el-dialog>

    <!-- 资源列表对话框 -->
    <el-dialog :title="title" :visible.sync="fileListOpen" width="1000px" append-to-body>
      <el-table class="file-list" :data="fileInfoList">
        <el-table-column type="selection" width="55" align="center" />
        <!-- <el-table-column label="文件主键id" align="center" prop="fileId" /> -->
        <el-table-column label="Image Preview" align="center" prop="pic">
          <template slot-scope="scope">
            <el-image style="width: 120px;height: 60px;" :src="scope.row.pic" lazy :preview-src-list="[scope.row.pic]">
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
        <el-table-column label="Actions" align="center" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-download" @click="handleDownload(scope.row)">Download</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 附件管理对话框 -->
    <el-dialog title="Attachment Manage" :visible.sync="blogFilesOpen" :before-close="cancel" width="1200px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-position="top" label-width="80px">
        <el-form-item>
          <el-row>
            <el-button size="mini" type="primary" @click="addFiles">Add File</el-button>
            <el-table :data="form.blogFilesNew" :border="true" style="width: 99.99%;">
              <el-table-column align="center" min-width="20%" prop="pic" label="Attachment">
                <template slot-scope="scope">
                  <filesUpload v-model="scope.row.fileId" @handleFilesSuccess="filesSuccess" :is-show-tip="false" />
                </template>
              </el-table-column>
              <el-table-column align="center" min-width="20%" prop="remark" label="Attachment Info">
                <template slot-scope="scope">
                  <el-row>
                    <el-col :span="6"><div class="blogFilesInfoName">Name:</div></el-col>
                    <el-col :span="18"><el-input v-model="scope.row.fileOriginName" disabled/></el-col>
                  </el-row>
                  <el-row style="margin-top: 4px;">
                    <el-col :span="6"><div class="blogFilesInfoName">Size:</div></el-col>
                    <el-col :span="18"><el-input v-model="scope.row.fileSize" disabled/></el-col>
                  </el-row>
                  <el-row style="margin-top: 4px;">
                    <el-col :span="6"><div class="blogFilesInfoName">Type:</div></el-col>
                    <el-col :span="18"><el-input v-model="scope.row.fileSuffix" disabled/></el-col>
                  </el-row>
                </template>
              </el-table-column>
              <el-table-column align="center" min-width="40%" prop="remark" label="Remark">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.remark" type="textarea" :rows="6" size="small" />
                </template>
              </el-table-column>
              <el-table-column align="center" min-width="20%" label="Action">
                <template slot-scope="scope">
                  <el-button v-show="scope.row.fileId!==''" size="mini" plain @click="handleDownload(scope.row)">Download</el-button>
                  <el-button size="mini" type="danger" plain @click="delFiles(scope.$index, scope.row)">Delete</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-row></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="saveBlogFiles">Save</el-button>
        <el-button @click="cancel">Cancel</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
  import filesUpload from './components/filesUpload'
  import VditorEditor from '@/components/VditorEditor'
  import {
    listBlog,
    getBlog,
    delBlog,
    addBlog,
    updateBlog,
    cancelBlog
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

  export default {
    name: "Blog",
    dicts: ['cms_blog_status'],
    components: {
      filesUpload,
      VditorEditor
    },
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
        // 文章管理表格数据
        blogList: [],
        // 资源列表表格数据
        fileInfoList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        fileListOpen: false,
        blogFilesOpen: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          title: null,
          type: 1,
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
        // 类型选项
        typeOptions: [],
        // 标签选项
        tagOptions: [],
      };
    },
    created() {
      this.getList();
    },
    computed: {
      tableEmptyText() {
        const hasTitle = this.queryParams.title && this.queryParams.title.trim() !== "";
        const hasStatus = this.queryParams.status !== null && this.queryParams.status !== undefined && this.queryParams.status !== "";

        if (hasTitle || hasStatus) {
          return "No matching blog posts for the current filters";
        }

        return "No blog posts yet";
      }
    },
    methods: {
      /** 查询文章管理列表 */
      getList() {
        this.loading = true;
        listBlog(this.queryParams).then(response => {
          for (let i = 0; i < response.rows.length; i++) {
            let blogInfo = response.rows[i];
            if (blogInfo.blogPic.length > 0) {
              response.rows[i].blogPic = process.env.VUE_APP_BASE_API + blogInfo.blogPic
            }else{
              response.rows[i].blogPic = '/errorImg.jpg'
            }
            if (blogInfo.blogPicLink.length == 0) {
              response.rows[i].blogPicLink = '/errorImg.jpg'
            }
          };
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
          cancelBlog(this.form).then(response => {});
          this.top = false;
          this.open = false;
          this.blogFilesOpen = false;
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
          type: 1,
          content: null,
          top: "0",
          views: null,
          status: "0",
          blogDesc: null,
          blogFiles: null,
          blogPicType: "0",
          blogPic: null,
          blogPicLink: null,
          tagIds: [],
          typeIds: [],
          blogFilesNew: [],
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
        this.names = selection.map(item => item.title)
        this.single = selection.length !== 1
        this.multiple = !selection.length
      },
      /** 新增按钮操作 */
      handleAdd() {
        getBlog().then(response => {
          this.typeOptions = response.types;
          this.tagOptions = response.tags;
          this.reset();
          this.open = true;
          this.title = "Add Blog";
        });
      },
      /** 修改按钮操作 */
      handleUpdate(row) {
        this.reset();
        const id = row.id || this.ids
        getBlog(id).then(response => {
          this.typeOptions = response.types;
          this.tagOptions = response.tags;
          this.form = response.data;
          // Force Vditor as the only editor
          this.form.contentType = '4'
          if (!this.form.contentMarkdown && this.form.content) {
            // Fallback: seed markdown field with existing content
            this.form.contentMarkdown = this.form.content
          }
          if (this.form.top == 1) {
            this.top = true;
          };
          this.open = true;
          this.title = "Edit Blog";
        });
      },
      /** 发布按钮 */
      releaseForm() {
        this.$refs["form"].validate(valid => {
          if (valid) {
            this.$modal.confirm('Are you sure you want to publish?').then(()=>{
              this.form.type = 1;
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
                  if (this.fileIds.length > 0) {
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
                  if (this.fileIds.length > 0) {
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
              this.form.type = 1;
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
                  if (this.fileIds.length > 0) {
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
                  if (this.fileIds.length > 0) {
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
        let name = row.title || this.names;
        this.$modal.confirm('Are you sure you want to delete \"' + name + '\"?').then(function() {
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
          for (let i = 0; i < response.data.length; i++) {
            let fileInfo = response.data[i];
            switch (fileInfo.fileSuffix) {
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
      /** 附件管理按钮操作 */
      blogFiles(row) {
        this.reset();
        const id = row.id || this.ids
        getBlog(id).then(response => {
          this.typeOptions = response.types;
          this.tagOptions = response.tags;
          this.form = response.data;
          this.form.blogFilesNew = []
          if (response.data.blogFiles !== null) {
            this.form.blogFilesNew = JSON.parse(response.data.blogFiles)
          }
          if (this.form.top == 1) {
            this.top = true;
          };
          this.blogFilesOpen = true;
        });
      },
      // 附件管理添加按钮
      addFiles() {
        this.form.blogFilesNew.push(
          {
            id: this.uuid(),
            fileId: '',
            fileOriginName: '',
            fileSuffix: '',
            fileSize: '',
            filePath: '',
            remark: ''
          })
      },
      delFiles(index, row) {
        this.$confirm('Confirm deletion?', 'Prompt', {
          confirmButtonText: 'Confirm',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
          // 点击确定的操作(调用接口)
          var hasmembers = this.form.blogFilesNew
          for (var i = 0; i < hasmembers.length; i++) {
            if (row.id === hasmembers[i].id) {
              this.form.blogFilesNew.splice(i, 1)
            }
          }
        }).catch(() => {
          // 点取消的提示
          return
        })
      },
      // 生成uuid
      uuid() {
        var d = new Date().getTime()
        if (window.performance && typeof window.performance.now === 'function') {
          d += performance.now() // use high-precision timer if available
        }
        var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
          var r = (d + Math.random() * 16) % 16 | 0 // d是随机种子
          d = Math.floor(d / 16)
          return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
        })
        return uuid
      },
      filesSuccess(value){
        this.form.blogFilesNew.forEach(item => {
          if(item.fileId===value.fileId){
            item.fileOriginName = value.fileOriginName
            item.fileSuffix = value.fileSuffix
            item.fileSize = value.fileSize
            item.filePath = value.filePath
          }
        });
      },
      //保存文件
      saveBlogFiles(){
        if (this.form.blogFilesNew.length > 0) {
          for (let i = 0; i < this.form.blogFilesNew.length; i++) {
            const fileInfo = this.form.blogFilesNew[i]
            if (fileInfo.fileId === '') {
              this.$message.warning('Please add a file or delete empty rows!')
              return false
            } else if (fileInfo.remark === '') {
              this.$message.warning('Please add a file description!')
              return false
            }
          }
        }
        this.form.blogFiles = JSON.stringify(this.form.blogFilesNew)
        updateBlog(this.form).then(response => {
          this.$modal.msgSuccess("Saved successfully");
          this.blogFilesOpen = false;
          this.getList();
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
      tableRowClassName({
        row,
        rowIndex
      }) {
        if (row.top == 1) {
          return 'warning-row';
        }
        return '';
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
  .el-tag+.el-tag {
    margin-left: 10px;
  }
  .el-table .warning-row {
      background: #f8f8f9;
    }
  .blogFilesInfoName {
    text-align: center;
    padding-top: 5px;
  }
  .tabBlock {
    min-height: 220px;
    margin-top: 16px;
  }
  .blogPic {
    width: 100%;
    max-width: 260px;
    height: 148px;
    border-radius: 18px;
    object-fit: cover;
  }

  .blog-editor-form {
    padding: 4px 8px 0;
  }

  .editor-title-field {
    margin-bottom: 20px;
  }

  .editor-meta-grid,
  .editor-bottom-grid {
    margin-bottom: 8px;
  }

  .editor-panel {
    padding: 18px 20px 20px;
    border: 1px solid rgba(23, 32, 51, 0.08);
    border-radius: 24px;
    background: rgba(255, 253, 249, 0.84);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
  }

  .editor-content-panel {
    margin: 18px 0 22px;
    padding: 20px 22px 24px;
  }

  .editor-comment-hint {
    margin-bottom: 10px;
    color: rgba(29, 36, 51, 0.5);
    font-size: 13px;
  }

  .editor-tags-panel :deep(.el-select),
  .editor-bottom-grid :deep(.el-select) {
    width: 100%;
  }

  .editor-tags-panel :deep(.el-select__tags),
  .editor-bottom-grid :deep(.el-select__tags) {
    max-width: calc(100% - 64px) !important;
  }

  .editor-pin-panel :deep(.el-form-item__content) {
    min-height: 42px;
    display: flex;
    align-items: center;
  }

  .editor-inline-toggle {
    display: flex;
    align-items: flex-end;
    justify-content: flex-start;
    margin-top: 18px;
    padding-top: 16px;
    border-top: 1px solid rgba(23, 32, 51, 0.08);
  }

  .editor-page-actions {
    display: flex;
    justify-content: flex-start;
    margin-top: 18px;
    margin-bottom: 4px;
  }

  .pin-toggle {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    height: 40px;
    padding: 0 16px;
    border: 1px solid rgba(29, 36, 51, 0.1);
    border-radius: 999px;
    background: rgba(29, 36, 51, 0.06);
    color: #1d2433;
    font-size: 13px;
    font-weight: 700;
    box-shadow: 0 10px 20px rgba(18, 27, 43, 0.05);
    transition: background .18s ease, border-color .18s ease, color .18s ease, transform .18s ease;
    cursor: pointer;
  }

  .pin-toggle:hover {
    transform: translateY(-1px);
  }

  .pin-toggle__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: rgba(29, 36, 51, 0.32);
  }

  .pin-toggle.is-active {
    background: rgba(174, 123, 50, 0.16);
    border-color: rgba(174, 123, 50, 0.24);
    color: #8f6327;
  }

  .pin-toggle.is-active .pin-toggle__dot {
    background: #ae7b32;
  }

  .editor-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding-top: 18px;
  }

  .editor-footer :deep(.el-button) {
    min-width: 104px;
    height: 40px;
    padding: 0 22px;
    border-radius: 16px;
    font-weight: 700;
    box-shadow: 0 10px 20px rgba(18, 27, 43, 0.05);
  }

  .editor-footer :deep(.editor-action-btn) {
    letter-spacing: 0.01em;
  }

  .editor-footer :deep(.editor-action-btn--publish.el-button--success.is-plain) {
    background: rgba(47, 143, 116, 0.1);
    border-color: rgba(47, 143, 116, 0.18);
    color: #236b56;
  }

  .editor-footer :deep(.editor-action-btn--save.el-button--warning.is-plain) {
    background: rgba(254, 193, 113, 0.18);
    border-color: rgba(174, 123, 50, 0.22);
    color: #9a6719;
  }

  .editor-footer :deep(.editor-action-btn--cancel.el-button--danger.is-plain) {
    background: rgba(192, 54, 57, 0.08);
    border-color: rgba(192, 54, 57, 0.15);
    color: #b02e33;
  }

  .blog-editor-form :deep(.el-form-item__label) {
    color: #3f4c61;
    font-size: 13px;
    font-weight: 700;
    line-height: 1.2;
    padding-bottom: 10px;
  }

  .blog-editor-form :deep(.el-input__inner),
  .blog-editor-form :deep(.el-textarea__inner),
  .blog-editor-form :deep(.el-select .el-input__inner) {
    border-radius: 14px;
    border-color: rgba(23, 32, 51, 0.1);
    background: rgba(255, 255, 255, 0.9);
  }

  .blog-editor-form :deep(.el-input__inner) {
    height: 42px;
  }

  .blog-editor-form :deep(.el-textarea__inner) {
    min-height: 180px !important;
    line-height: 1.7;
    padding: 14px 16px;
  }

  .editor-description-panel :deep(.el-form-item__content) {
    display: flex;
    flex-direction: column;
  }

  .description-surface {
    padding: 12px;
    border: 1px solid rgba(29, 36, 51, 0.06);
    border-radius: 18px;
    background: rgba(245, 246, 247, 0.72);
  }

  .description-textarea {
    display: block;
    width: 100%;
    min-height: 156px;
    padding: 0;
    border: none;
    outline: none;
    resize: none;
    background: transparent;
    box-shadow: none;
    color: #1d2433;
    font-size: 18px;
    line-height: 1.8;
    font-family: inherit;
    -webkit-appearance: none;
    appearance: none;
  }

  .description-textarea:focus,
  .description-textarea:active {
    border: none;
    outline: none;
    box-shadow: none;
  }

  .description-textarea::placeholder {
    color: rgba(29, 36, 51, 0.36);
  }

  .description-count {
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
    color: rgba(29, 36, 51, 0.42);
    font-size: 12px;
    line-height: 1;
  }

  .editor-description-panel :deep(.el-textarea) {
    flex: 1;
  }

  .editor-description-panel :deep(.el-textarea__inner) {
    border: none;
    background: transparent;
    box-shadow: none;
    padding: 0;
    min-height: 156px !important;
    font-size: 18px;
    line-height: 1.8;
    resize: none;
  }

  .editor-description-panel :deep(.el-textarea__inner:focus) {
    border: none;
    box-shadow: none;
    outline: none;
  }

  .editor-description-panel :deep(.el-input__count) {
    bottom: 6px;
    right: 0;
    background: transparent;
  }

  .editor-cover-panel :deep(.el-radio-group) {
    display: inline-flex;
    padding: 4px;
    background: rgba(23, 32, 51, 0.06);
    border-radius: 14px;
  }

  .editor-cover-panel :deep(.el-radio-button__inner) {
    border: none;
    border-radius: 10px;
    box-shadow: none !important;
    background: transparent;
    color: #5a6474;
  }

  .editor-cover-panel :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
    background: rgba(255, 255, 255, 0.96);
    color: #1d2433;
  }

  .editor-content-panel :deep(.vditor) {
    box-shadow: none;
  }

  .editor-content-panel :deep(.vditor-toolbar) {
    padding: 14px 18px;
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;
  }

  .editor-content-panel :deep(.vditor-toolbar::-webkit-scrollbar) {
    height: 0;
  }

  .editor-content-panel :deep(.vditor-toolbar__item),
  .editor-content-panel :deep(.vditor-toolbar__divider) {
    flex: 0 0 auto;
  }

  .editor-content-panel :deep(.vditor-reset) {
    padding: 36px 38px 42px;
  }

  .editor-content-panel :deep(.vditor-ir pre.vditor-reset),
  .editor-content-panel :deep(.vditor-sv__editor),
  .editor-content-panel :deep(.vditor-wysiwyg) {
    padding: 36px 38px 42px !important;
  }

  .blog-editor-form :deep(.el-form-item__error) {
    position: static;
    margin-top: 8px;
    padding-left: 2px;
  }
</style>

<style lang="scss">
.blog-editor-dialog {
  border-radius: 30px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(251, 248, 241, 0.98) 0%, rgba(245, 247, 251, 0.98) 100%);
  box-shadow: 0 34px 90px rgba(13, 19, 32, 0.2);
}

.blog-editor-dialog .el-dialog__header {
  padding: 22px 26px 16px;
  border-bottom: 1px solid rgba(23, 32, 51, 0.06);
}

.blog-editor-dialog .el-dialog__title {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #1a2335;
}

.blog-editor-dialog .el-dialog__headerbtn {
  top: 24px;
  right: 22px;
}

.blog-editor-dialog .el-dialog__body {
  padding: 22px 24px 12px;
}

.blog-editor-dialog .el-dialog__footer {
  padding: 0 24px 24px;
}
</style>
