<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="Announcement Title" prop="noticeTitle">
        <el-input v-model="queryParams.noticeTitle" placeholder="Enter announcement title" clearable size="small"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="Operator" prop="createBy">
        <el-input v-model="queryParams.createBy" placeholder="Enter operator" clearable size="small"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="Type" prop="noticeType">
        <el-select v-model="queryParams.noticeType" placeholder="Announcement type" clearable size="small">
          <el-option v-for="dict in dict.type.sys_notice_type" :key="dict.value" :label="dict.label"
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
          v-hasPermi="['system:notice:add']">Add</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['system:notice:edit']">Edit</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['system:notice:remove']">Delete</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="noticeId" width="100" />
      <el-table-column label="Announcement Title" align="center" prop="noticeTitle" :show-overflow-tooltip="true" />
      <el-table-column label="Type" align="center" prop="noticeType" width="130">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_notice_type" :value="scope.row.noticeType" />
        </template>
      </el-table-column>
      <el-table-column label="Status" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_notice_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="Creator" align="center" prop="createBy" width="100" />
      <el-table-column label="Creation Time" align="center" prop="createTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Actions" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['system:notice:edit']">Edit</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['system:notice:remove']">Delete</el-button>
          <el-button size="mini" type="text" icon="el-icon-folder-opened" @click="fileList(scope.row)"
            v-hasPermi="['system:notice:query']">Resource List</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改公告对话框 -->
    <el-dialog :title="title" :visible.sync="open" :before-close="cancel" width="780px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="170px">
        <el-row>
          <el-col>
            <el-form-item label="Announcement Title" prop="noticeTitle">
              <el-input v-model="form.noticeTitle" placeholder="Enter announcement title" />
            </el-form-item>
          </el-col>
          <el-col>
            <el-form-item label="Type" prop="noticeType">
              <el-select v-model="form.noticeType" placeholder="Select">
                <el-option v-for="dict in dict.type.sys_notice_type" :key="dict.value" :label="dict.label"
                  :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col>
            <el-form-item label="Status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_notice_status" :key="dict.value" :label="dict.value">
                  {{dict.label}}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col>
            <el-form-item label="Content">
              <cmsEditor v-model="form.noticeContent" @getFileId="getFileId" :min-height="192" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">Confirm</el-button>
        <el-button @click="cancel">Cancel</el-button>
      </div>
    </el-dialog>

    <!-- 资源列表对话框 -->
    <el-dialog :title="title" :visible.sync="fileListOpen" width="1000px" append-to-body>
      <el-table v-loading="loading" :data="fileInfoList">
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
    listNotice,
    getNotice,
    delNotice,
    addNotice,
    updateNotice
  } from "@/api/system/notice";
  import {
    delFileInfo
  } from "@/api/cms/fileInfo";
  import {
    addFileNoticeInfo,
    delFileNoticeInfo,
    getFileList
  } from "@/api/cms/fileNoticeInfo";

  export default {
    name: "Notice",
    dicts: ['sys_notice_status', 'sys_notice_type'],
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
        // 公告表格数据
        noticeList: [],
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
          noticeTitle: undefined,
          createBy: undefined,
          status: undefined
        },
        // 表单参数
        form: {},
        // 表单校验
        rules: {
          noticeTitle: [{
            required: true,
            message: "Announcement title cannot be empty",
            trigger: "blur"
          }],
          noticeType: [{
            required: true,
            message: "Announcement type cannot be empty",
            trigger: "change"
          }]
        },
        fileIds: [],
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询公告列表 */
      getList() {
        this.loading = true;
        listNotice(this.queryParams).then(response => {
          this.noticeList = response.rows;
          this.total = response.total;
          this.loading = false;
        });
      },
      // 取消按钮
      cancel() {
        this.$confirm('Are you sure you want to discard this edit?', 'System Alert', {
          confirmButtonText: 'Confirm',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
          let fileids = this.fileIds;
          if (fileids.length > 0) {
            delFileInfo(fileids);
          };
          this.fileIds.length = 0;
          this.open = false;
          this.reset();
        }).catch(() => {});
      },
      // 表单重置
      reset() {
        this.form = {
          noticeId: undefined,
          noticeTitle: undefined,
          noticeType: undefined,
          noticeContent: undefined,
          status: "0"
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
        this.ids = selection.map(item => item.noticeId)
        this.single = selection.length != 1
        this.multiple = !selection.length
      },
      /** 新增按钮操作 */
      handleAdd() {
        this.reset();
        this.open = true;
        this.title = "Add Announcement";
      },
      /** 修改按钮操作 */
      handleUpdate(row) {
        this.reset();
        const noticeId = row.noticeId || this.ids
        getNotice(noticeId).then(response => {
          this.form = response.data;
          this.open = true;
          this.title = "Edit Announcement";
        });
      },
      /** 提交按钮 */
      submitForm: function() {
        this.$refs["form"].validate(valid => {
          if (valid) {
            if (this.form.noticeId != undefined) {
              updateNotice(this.form).then(response => {
                if(this.fileIds.length>0){
                  let fileNoticeInfo = {
                    noticeId: this.form.noticeId,
                    fileIds: this.fileIds
                  };
                  addFileNoticeInfo(fileNoticeInfo).then(response => {});
                }
                this.$modal.msgSuccess("Edit successful");
                this.fileIds.length = 0;
                this.open = false;
                this.getList();
              });
            } else {
              addNotice(this.form).then(response => {
                if(this.fileIds.length>0){
                  let fileNoticeInfo = {
                    noticeId: response.data,
                    fileIds: this.fileIds
                  };
                  addFileNoticeInfo(fileNoticeInfo).then(response => {});
                }
                this.$modal.msgSuccess("Add successful");
                this.fileIds.length = 0;
                this.open = false;
                this.getList();
              });
            }
          }
        });
      },
      /** 删除按钮操作 */
      handleDelete(row) {
        const noticeIds = row.noticeId || this.ids
        this.$modal.confirm('Are you sure you want to delete the announcement with ID "' + noticeIds + '"?').then(function() {
          delFileNoticeInfo(noticeIds).then().then(response => {});
          return delNotice(noticeIds);
        }).then(() => {
          this.getList();
          this.$modal.msgSuccess("Delete successful");
        }).catch(() => {});
      },
      getFileId(data) {
        this.fileIds.push(data);
      },
      /** 资源列表按钮操作 */
      fileList(row) {
        this.loading = true;
        this.reset();
        const noticeId = row.noticeId || this.ids
        getFileList(noticeId).then(response => {
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
          this.loading = false;
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
    }
  };
</script>
