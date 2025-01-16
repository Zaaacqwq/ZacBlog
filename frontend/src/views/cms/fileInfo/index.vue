<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="File Name" prop="fileOriginName">
        <el-input
          v-model="queryParams.fileOriginName"
          placeholder="Enter file name"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="File Type" prop="fileSuffix">
        <el-input
          v-model="queryParams.fileSuffix"
          placeholder="Enter file type, e.g., txt"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <!-- <el-form-item label="文件大小" prop="fileSizeInfo">
        <el-input
          v-model="queryParams.fileSizeInfo"
          placeholder="请输入文件大小"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item> -->
      <el-form-item label="Storage Name" prop="fileObjectName">
        <el-input
          v-model="queryParams.fileObjectName"
          placeholder="Enter storage file name"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <!-- <el-form-item label="存储路径" prop="filePath">
        <el-input
          v-model="queryParams.filePath"
          placeholder="请输入存储路径"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item> -->
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">Search</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">Reset</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <!-- <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['cms:fileInfo:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['cms:fileInfo:edit']"
        >修改</el-button>
      </el-col> -->
      <el-col :span="1.5">
      <el-button
        type="primary"
        plain
        icon="el-icon-upload"
        size="mini"
        @click="uploadFile"
        v-hasPermi="['cms:fileInfo:add']"
      >Upload File</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cms:fileInfo:remove']"
        >Delete</el-button>
      </el-col>
      <!-- <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['cms:fileInfo:export']"
        >导出</el-button>
      </el-col> -->
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="fileInfoList" @selection-change="handleSelectionChange">
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
      <el-table-column label="Storage File Name" align="center" prop="fileObjectName" />
      <!-- <el-table-column label="存储路径" align="center" prop="filePath" /> -->
      <el-table-column label="Creator" align="center" prop="createBy" />
      <el-table-column label="Creation Time" align="center" prop="createTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Actions" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <!-- <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:fileInfo:edit']"
          >修改</el-button> -->
          <el-button
            size="mini"
            type="text"
            icon="el-icon-download"
            @click="handleDownload(scope.row)"
          >Download</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['cms:fileInfo:remove']"
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

    <!-- 添加或修改文件管理对话框 -->
    <!-- <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="文件名称" prop="fileOriginName">
          <el-input v-model="form.fileOriginName" placeholder="请输入文件名称" />
        </el-form-item>
        <el-form-item label="文件类型" prop="fileSuffix">
          <el-input v-model="form.fileSuffix" placeholder="请输入文件类型，例如txt" />
        </el-form-item>
        <el-form-item label="文件大小" prop="fileSizeInfo">
          <el-input v-model="form.fileSizeInfo" placeholder="请输入文件大小" />
        </el-form-item>
        <el-form-item label="存储名称" prop="fileObjectName">
          <el-input v-model="form.fileObjectName" placeholder="请输入存储文件名称" />
        </el-form-item>
        <el-form-item label="存储路径" prop="filePath">
          <el-input v-model="form.filePath" placeholder="请输入存储路径" />
        </el-form-item>
        <el-form-item label="是否删除" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入是否删除：Y-被删除，N-未删除" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog> -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-upload
        style="min-height: 200px;"
        ref="upload"
        :limit="5"
        accept=".jpg, .png,.txt,.doc,.docx,.xls,.xlsx,.ppt,.zip,.pdf"
        :action="upload.url"
        :headers="upload.headers"
        :file-list="upload.fileList"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false">
        <el-button slot="trigger" size="small" type="primary">Select Files</el-button>
        <el-button style="margin-left: 10px;" size="small" type="success" :loading="upload.isUploading" @click="submitUpload">Upload to server</el-button>
        <div slot="tip" class="el-upload__tip">Only jpg/png/txt/doc/docx/xls/xlsx/ppt/zip/pdf files are allowed, each file should not exceed 50MB</div>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>
import { listFileInfo, getFileInfo, delFileInfo, addFileInfo, updateFileInfo } from "@/api/cms/fileInfo";
import { getToken } from "@/utils/auth";
import image from './image.js';
import store from '@/store';

export default {
  name: "FileInfo",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      fileNames: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文件管理表格数据
      fileInfoList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 上传参数
      upload: {
        // 是否禁用上传
        isUploading: false,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/common/upload",
        // 上传的文件列表
        fileList: []
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        fileOriginName: null,
        fileSuffix: null,
        fileSizeInfo: null,
        fileObjectName: null,
        filePath: null,
        createBy: store.getters.name
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        fileOriginName: [
          { required: true, message: "File name cannot be empty", trigger: "blur" }
        ],
        delFlag: [
          //{ required: true, message: "是否删除：Y-被删除，N-未删除不能为空", trigger: "blur" }
          { required: true, message: "Deletion status cannot be empty: Y-Deleted, N-Not deleted", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
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
    /** 查询文件管理列表 */
    getList() {
      this.loading = true;
      listFileInfo(this.queryParams).then(response => {
        for(let i = 0; i<response.rows.length; i++){
          let fileInfo = response.rows[i];
          switch (fileInfo.fileSuffix){
            case 'png': case 'jpg': case 'jpeg': case 'bmp': case 'gif':
              response.rows[i].pic = process.env.VUE_APP_BASE_API + fileInfo.filePath;
              break;
            default:
              response.rows[i].pic = image.bg1;
              break;
          };
        };
        this.fileInfoList = response.rows;
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
        fileId: null,
        fileOriginName: null,
        fileSuffix: null,
        fileSizeInfo: null,
        fileObjectName: null,
        filePath: null,
        delFlag: null,
        createBy: store.getters.name,
        createTime: null,
        updateBy: null,
        updateTime: null
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
      this.ids = selection.map(item => item.fileId)
      this.fileNames = selection.map(item => item.fileOriginName)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 上传文件按钮操作 */
    uploadFile() {
      this.reset();
      this.open = true;
      this.title = "Add File";
    },
    // 文件提交处理
    submitUpload() {
      this.$refs.upload.submit();
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.isUploading = false;
      this.form.filePath = response.url;
      this.$modal.msgSuccess(response.msg);
      this.getList()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "Add File Management";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const fileId = row.fileId || this.ids
      getFileInfo(fileId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "Edit File Management";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.fileId != null) {
            updateFileInfo(this.form).then(response => {
              this.$modal.msgSuccess("Update successful");
              this.open = false;
              this.getList();
            });
          } else {
            addFileInfo(this.form).then(response => {
              this.$modal.msgSuccess("Added successfully");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const fileIds = row.fileId || this.ids;
      let fileOriginName = row.fileOriginName || this.fileNames;
      this.$modal.confirm('Are you sure you want to delete the file named"' + fileOriginName + '"?').then(function() {
        return delFileInfo(fileIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("Deleted successfully");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('cms/fileInfo/export', {
        ...this.queryParams
      }, `fileInfo_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
