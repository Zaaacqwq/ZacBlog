<template>
  <el-container style="opacity: 0.9">
    <div class="composer-head author">
      <el-avatar v-if="token==null" icon="el-icon-user-solid" size="large" class="composer-avatar">
      </el-avatar>
      <el-avatar v-else :src="avatar" size="large" class="composer-avatar"></el-avatar>
      <div class="composer-meta">
        <div class="nkname">
          <span class="name" v-if="token==null">Anonymous User</span>
          <span class="name" v-else>{{name}} </span>
        </div>
        <div class="composer-caption">Leave a comment on this article.</div>
      </div>
    </div>
    <el-form :model="messageForm" :rules="messageFormRules" ref="messageFormRef">
      <el-form-item prop="content" class="composer-field">
        <div class="composer-surface">
          <el-input @blur="blur" :rows="4" v-model="messageForm.content" type="textarea" maxlength="100" show-word-limit
          placeholder="Leave your comment"></el-input>
        </div>
      </el-form-item>
      <el-form-item class="publish-actions">
        <div class="publish-action-row">
          <span class="composer-tip">Thoughtful comments work best.</span>
          <el-button class="publish-button" @click="publish">Publish Comment</el-button>
        </div>
      </el-form-item>
    </el-form>
    <el-divider v-if="messageList.length>0"><span style="color: #999;font-size: small;">Latest Comment</span></el-divider>
    <comment :comments="messageList" @replyConfirm="commitComment"></comment>
    <pagination class="blog-comment-pagination" v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getMessageList" />
  </el-container>
</template>

<script>
  import {
    mapGetters
  } from 'vuex'
  import {
    getToken
  } from '@/utils/auth'
  import {
    cmsListComment,
    cmsAddComment,
  } from "@/api/cms/comment"
  import comment from './comments.vue'
  export default {
    name: 'Ipcomment',
    data() {
      return {
        picList: [],
        editing: false,
        messageList: [],
        // userInfo: null,
        message: {
          userId: -1,
          content: ''
        },
        messageForm: {},
        // 总条数
        total: 0,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          parentId: null,
          mainId: null,
          likeNum: null,
          content: null,
          type: null,
          blogId: this.$route.query.id,
          userId: null,
          delFlag: null,
          createBy: null,
        },
        messageFormRules: {
          content: [{
            min: 0,
            max: 100,
            message: "Message should not exceed 100 characters!"
          }]
        },
        cursorIndexStart: null,//光标选中开始的位置
        cursorIndexEnd: null,//光标选中结束的位置
      }
    },
    created() {
      this.getMessageList()
      this.reset();
    },
    updated: function() {
      this.$nextTick(function() {
        // 仅在整个视图都被渲染之后才会运行的代码
        this.to();
      })
    },
    computed: {
      ...mapGetters([
        'token',
        'avatar',
        'name'
      ]),
    },
    components: {
      comment
    },
    methods: {
      // 表单重置
      reset() {
        this.messageForm = {
          id: null,
          parentId: null,
          mainId: null,
          likeNum: null,
          content: null,
          type: null,
          blogId: this.$route.query.id,
          userId: null,
          delFlag: null,
          createBy: null,
          createTime: null,
          updateBy: null,
          updateTime: null
        };
        this.resetForm("messageForm");
      },
      // 评论发表
      publish() {
        let token = getToken();
        this.$refs.messageFormRef.validate(async valid => {
          if (!valid) return
          if (this.messageForm.content == null || this.messageForm.content == '') {
            this.$modal.msgError("Comment content cannot be empty!");
            return;
          }
          if (token == null || token == '') {
            this.messageForm.createBy = "Anonymous User"
            this.messageForm.type = '0'
          } else {
            this.messageForm.createBy = this.$store.getters.name
            this.messageForm.type = '0'
          }
          cmsAddComment(this.messageForm).then(response => {
            this.$modal.msgSuccess("Comment published successfully");
            this.reset();
            this.getMessageList();
          });
        })
      },
      /**
       * 提交评论
       */
      commitComment(value) {
        this.reset();
        this.messageForm.content = value.inputComment;
        this.messageForm.parentId = value.id;
        this.messageForm.mainId = value.mainId;
        let token = getToken();
        this.$refs.messageFormRef.validate(async valid => {
          if (!valid) return
          if (this.messageForm.content == null || this.messageForm.content == '') {
            this.$modal.msgError("Comment content cannot be empty!");
            return;
          }
          if (token == null || token == '') {
            this.messageForm.createBy = "Anonymous User"
            this.messageForm.type = '1'
          } else {
            this.messageForm.createBy = this.$store.getters.name
            this.messageForm.type = '1'
          }
          cmsAddComment(this.messageForm).then(response => {
            this.$modal.msgSuccess("Comment published successfully");
            this.reset();
            this.getMessageList();
          });
        })
      },
      // 获取评论列表
      async getMessageList() {
        let token = getToken();
        if (token != null && token != '') {
          this.queryParams.createBy = this.$store.getters.name
        }
        cmsListComment(this.queryParams).then(response => {
          for (let i = 0; i < response.rows.length; i++) {
            let mesInfo = response.rows[i];
            if (mesInfo.avatar != null && mesInfo.avatar != "") {
              response.rows[i].avatar = process.env.VUE_APP_BASE_API + mesInfo.avatar
            }
            if (mesInfo.children != null && mesInfo.children != "") {
              for (let j = 0; j < response.rows[i].children.length; j++) {
                let children = response.rows[i].children;
                if (children.avatar != null && children.avatar != "") {
                  response.rows[i].children[j].avatar = process.env.VUE_APP_BASE_API + children.avatar
                }
              };
            }
          };
          this.messageList = response.rows;
          this.total = response.total;
        });
      },
      blur(e){
        this.cursorIndexStart = e.srcElement.selectionStart  // 获取input输入框失去焦点时光标选中开始的位置
        this.cursorIndexEnd = e.srcElement.selectionEnd  // 获取input输入框失去焦点时光标选中结束的位置
      },
      //跳转到相应位置
      to() {
        if (this.$route.query.commentId != null) {
          var toEl = document.getElementById(this.$route.query.commentId);
          if (toEl != null) {
            if (toEl != null && toEl != "") {
              // toEl 为指定跳转到该位置的DOM节点
              let bridgeCms = toEl;
              let bodyTop = document.body;
              let heightCms = 0;
              // 计算该 DOM 节点到 bodyTop 顶部距离
              do {
                heightCms += bridgeCms.offsetTop;
                bridgeCms = bridgeCms.offsetParent;
              } while (bridgeCms !== bodyTop)
              // 滚动到指定位置
              window.scrollTo({
                top: heightCms,
                behavior: 'smooth'
              })
            }
          }
        }
      },

    },
  }
</script>

<style scoped>
  .el-container {
    display: block;
  }

  .el-container /deep/ .el-form-item__content {
    line-height: normal;
  }

  .author {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    width: 100%;
    margin-bottom: 16px;
  }

  .composer-head {
    gap: 14px;
  }

  .composer-avatar {
    flex-shrink: 0;
  }

  .composer-meta {
    min-width: 0;
  }

  .composer-caption {
    margin-top: 4px;
    color: rgba(29, 36, 51, 0.54);
    font-size: 13px;
  }

  .composer-field {
    margin-bottom: 14px;
  }

  .composer-surface {
    padding: 12px;
    border: 1px solid rgba(29, 36, 51, 0.07);
    border-radius: 22px;
    background: #f5f6f7;
  }

  .composer-surface /deep/ .el-textarea__inner {
    min-height: 132px !important;
    padding: 0;
    border: 0;
    background: transparent;
    box-shadow: none;
    resize: none;
  }

  .composer-surface /deep/ .el-input__count {
    background: transparent;
    color: rgba(29, 36, 51, 0.42);
    right: 0;
    bottom: 0;
  }

  .publish-actions {
    margin-bottom: 0;
  }

  .publish-action-row {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 14px;
    padding-top: 4px;
  }

  .composer-tip {
    margin-right: auto;
    color: rgba(29, 36, 51, 0.46);
    font-size: 12px;
  }

  .publish-button {
    min-width: 152px;
    padding: 12px 20px;
    border: 1px solid rgba(29, 36, 51, 0.08);
    border-radius: 999px;
    background: #eceff2;
    color: #1d2433;
    font-weight: 500;
    letter-spacing: 0.04em;
    box-shadow: none;
  }

  .publish-button:hover,
  .publish-button:focus {
    background: #e3e7eb;
    color: #111827;
  }

  .comment {
    border-bottom: 1px dashed #ccc;
    margin: 30px 0;
    display: flex;
  }

  .content {
    text-align: left;
    font-size: 14px;
    flex-grow: 1;
  }

  .nkname {
    margin: 10px;
    max-width: 530px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .date {
    color: #999;
    margin-left: 10px;
  }

  .reply {
    margin-left: 10px;
  }

  .blog-comment-pagination /deep/ .pagination-container {
    padding: 22px 0 10px;
    background: transparent;
  }

  .blog-comment-pagination /deep/ .el-pagination {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    color: rgba(29, 36, 51, 0.6);
    font-weight: 500;
  }

  .blog-comment-pagination /deep/ .btn-prev,
  .blog-comment-pagination /deep/ .btn-next,
  .blog-comment-pagination /deep/ .el-pager li,
  .blog-comment-pagination /deep/ .el-pagination__jump .el-input__inner,
  .blog-comment-pagination /deep/ .el-pagination__sizes .el-input__inner {
    border: 1px solid rgba(29, 36, 51, 0.07);
    border-radius: 14px !important;
    background: #f5f5f5 !important;
    color: #1d2433 !important;
    box-shadow: none !important;
  }

  .blog-comment-pagination /deep/ .el-pager li {
    min-width: 36px;
    height: 36px;
    line-height: 34px;
    margin: 0 2px;
  }

  .blog-comment-pagination /deep/ .btn-prev,
  .blog-comment-pagination /deep/ .btn-next {
    width: 36px;
    height: 36px;
    padding: 0;
  }

  .blog-comment-pagination /deep/ .el-pager li.active {
    background: #e8e8e8 !important;
    border-color: rgba(29, 36, 51, 0.1);
    color: #111827 !important;
    font-weight: 700;
  }

  .blog-comment-pagination /deep/ .btn-prev:hover,
  .blog-comment-pagination /deep/ .btn-next:hover,
  .blog-comment-pagination /deep/ .el-pager li:hover,
  .blog-comment-pagination /deep/ .el-pagination__jump .el-input__inner:hover,
  .blog-comment-pagination /deep/ .el-pagination__sizes .el-input__inner:hover {
    background: #eeeeee !important;
  }
</style>
