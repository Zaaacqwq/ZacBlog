<!-- comments page -->
<template>
  <div class="no-caret">
    <section class="message-shell">
      <div class="message-grid">
        <div class="message-main">
        <el-container style="opacity: 0.9" class="message">
          <el-card class="animate__animated animate__fadeInLeft publish">
            <div class="composer-head author">
              <el-avatar v-if="token == null" icon="el-icon-user-solid" size="large" class="composer-avatar">
              </el-avatar>
              <el-avatar v-else :src="avatar" size="large" class="composer-avatar"></el-avatar>
              <div class="composer-meta">
                <div class="nkname">
                  <span class="name" v-if="token == null">Anonymous User</span>
                  <span class="name" v-else>{{ name }} </span>
                </div>
                <div class="composer-caption">Share a thought, quick note, or reply.</div>
              </div>
            </div>
            <el-form :model="messageForm" :rules="messageFormRules" ref="messageFormRef">
              <el-form-item prop="content" class="composer-field">
                <div class="composer-surface">
                  <el-input @blur="blur" :rows="4" v-model="messageForm.content" type="textarea" maxlength="100"
                  show-word-limit placeholder="Enter your message"></el-input>
                </div>
              </el-form-item>
              <el-form-item class="publish-actions">
                <div class="publish-action-row">
                  <span class="composer-tip">Keep it concise and readable.</span>
                  <el-button class="publish-button" @click="publish">Publish Message</el-button>
                </div>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card v-if="messageList.length > 0" class="animate__animated animate__fadeInLeft">
            <comment :comments="messageList" @replyConfirm="commitComment"></comment>
            <pagination class="message-pagination" v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
              :limit.sync="queryParams.pageSize" @pagination="getMessageList" />
          </el-card>
        </el-container>
        </div>
      <div class="message-side right-sidebar">
        <RightSidebar />
      </div>
      <!-- 设置底部距离的 -->
      <el-backtop :bottom="60">
        <div class="backtop-icon">
          <svg-icon icon-class="top" style="color: black;"/>
        </div>
      </el-backtop>
      </div>
    </section>
  </div>
</template>

<script>
import {
  mapGetters
} from 'vuex'
import {
  getToken
} from '@/utils/auth'
import {
  cmsListMessage,
  cmsAddMessage,
} from "@/api/cms/message"
import comment from './messages/messages.vue'
import RightSidebar from "./rightSidebar/rightSidebar.vue";
export default {
  name: 'message',
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
        blogId: null,
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
  updated: function () {
    this.$nextTick(function () {
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
    comment,
    RightSidebar
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
        blogId: null,
        userId: null,
        delFlag: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.resetForm("messageForm");
    },
    // 留言发表
    publish() {
      let token = getToken();
      this.$refs.messageFormRef.validate(async valid => {
        if (!valid) return
        if (this.messageForm.content == null || this.messageForm.content == '') {
          this.$modal.msgError("Message content cannot be empty!");
          return;
        }
        if (token == null || token == '') {
          this.messageForm.createBy = "Anonymous User"
          this.messageForm.type = '0'
        } else {
          this.messageForm.createBy = this.$store.getters.name
          this.messageForm.type = '0'
        }
        cmsAddMessage(this.messageForm).then(response => {
          this.$modal.msgSuccess("Message published successfully");
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
        cmsAddMessage(this.messageForm).then(response => {
          this.$modal.msgSuccess("Comment published successfully");
          this.reset();
          this.getMessageList();
        });
      })
    },
    // 获取留言列表
    async getMessageList() {
      let token = getToken();
      if (token != null && token != '') {
        this.queryParams.createBy = this.$store.getters.name
      }
      cmsListMessage(this.queryParams).then(response => {
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
    blur(e) {
      this.cursorIndexStart = e.srcElement.selectionStart  // 获取input输入框失去焦点时光标选中开始的位置
      this.cursorIndexEnd = e.srcElement.selectionEnd  // 获取input输入框失去焦点时光标选中结束的位置
    },
    output(val) {
      if (this.cursorIndexStart !== null && this.messageForm.content) {
        //如果 文本域获取了焦点, 则在光标位置处插入对应字段内容
        this.messageForm.content = this.messageForm.content.substring(0, this.cursorIndexStart) + val + this.messageForm.content.substring(this.cursorIndexEnd)
      } else {
        // 如果 文本域未获取焦点, 则在字符串末尾处插入对应字段内容
        this.messageForm.content = this.messageForm.content ? this.messageForm.content : '' + val
      }
    },
    //跳转到相应位置
    to() {
      if (this.$route.query.id != null) {
        var toEl = document.getElementById(this.$route.query.id);
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
.message-shell {
  width: min(1360px, calc(100vw - 24px));
  margin: 0 auto;
  padding-top: 132px;
  padding-bottom: 40px;
}

.message-grid {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.message-main {
  min-width: 0;
}

.message-side {
  width: 320px;
}

.el-container {
  display: block;
  width: 100%;
}

.publish {
  margin-bottom: 20px;
  border-radius: 28px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  box-shadow: 0 20px 50px rgba(20, 28, 43, 0.08);
}

.publish /deep/ .el-card__body {
  padding: 22px;
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

.composer-field /deep/ .el-form-item__content {
  line-height: normal;
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
  transform: none;
}

.publish-button:active {
  transform: translateY(0);
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

.message-pagination /deep/ .pagination-container {
  padding: 22px 12px 10px;
  background: transparent;
}

.message-pagination /deep/ .el-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: rgba(29, 36, 51, 0.6);
  font-weight: 500;
}

.message-pagination /deep/ .btn-prev,
.message-pagination /deep/ .btn-next,
.message-pagination /deep/ .el-pager li,
.message-pagination /deep/ .el-pagination__jump .el-input__inner,
.message-pagination /deep/ .el-pagination__sizes .el-input__inner {
  border: 1px solid rgba(29, 36, 51, 0.07);
  border-radius: 14px !important;
  background: #f5f5f5 !important;
  color: #1d2433 !important;
  box-shadow: none !important;
}

.message-pagination /deep/ .el-pager li {
  min-width: 36px;
  height: 36px;
  line-height: 34px;
  margin: 0 2px;
}

.message-pagination /deep/ .btn-prev,
.message-pagination /deep/ .btn-next {
  width: 36px;
  height: 36px;
  padding: 0;
}

.message-pagination /deep/ .el-pager li.active {
  background: #e8e8e8 !important;
  border-color: rgba(29, 36, 51, 0.1);
  color: #111827 !important;
  font-weight: 700;
}

.message-pagination /deep/ .btn-prev:hover,
.message-pagination /deep/ .btn-next:hover,
.message-pagination /deep/ .el-pager li:hover,
.message-pagination /deep/ .el-pagination__jump .el-input__inner:hover,
.message-pagination /deep/ .el-pagination__sizes .el-input__inner:hover {
  background: #eeeeee !important;
}

@media screen and (max-width: 768px) {
  .message-shell {
    width: calc(100vw - 12px);
    padding-top: 112px;
  }

  .message-grid {
    grid-template-columns: 1fr;
  }

  .message-side {
    width: 100%;
  }
}
</style>
