<template>
  <div class="no-caret">
    <el-row :gutter="20" style="margin: 100px 0px;">
      <el-col :sm="2" :lg="5" class="hidden-xs-only" style="opacity:0;">Left PlaceHolder</el-col>
      <el-col :xs="24" :sm="15" :lg="11">
        <el-container style="opacity: 0.9" class="message">
          <el-card class="animate__animated animate__fadeInLeft publish">
            <div class="author">
              <el-avatar v-if="token == null" icon="el-icon-user-solid" size="large">
                <!-- style="background-color: #666" -->
              </el-avatar>
              <el-avatar v-else :src="avatar" size="large"></el-avatar>
              <div>
                <div class="nkname">
                  <span class="name" v-if="token == null">Anonymous User</span>
                  <span class="name" v-else>{{ name }} </span>
                </div>
              </div>
            </div>
            <el-form :model="messageForm" :rules="messageFormRules" ref="messageFormRef">
              <el-form-item prop="content">
                <el-input @blur="blur" :rows="5" v-model="messageForm.content" type="textarea" maxlength="100"
                  show-word-limit placeholder="Enter your message"></el-input>
              </el-form-item>
              <el-form-item>
                <el-row>
                  <el-col :span="12" style="text-align: left">
                    <Emoji @output="output"></Emoji>
                  </el-col>
                  <el-col :span="12" style="text-align: right">
                    <el-button type="primary" @click="publish">Click to Publish</el-button>
                  </el-col>
                </el-row>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card v-if="messageList.length > 0" class="animate__animated animate__fadeInLeft">
            <comment :comments="messageList" @replyConfirm="commitComment"></comment>
            <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
              :limit.sync="queryParams.pageSize" @pagination="getMessageList" />
          </el-card>
        </el-container>
      </el-col>
      <el-col :xs="24" :sm="5" :lg="3" class="right-sidebar">
        <RightSidebar />
      </el-col>
      <el-col :sm="2" :lg="5" class="hidden-xs-only" style="opacity:0;">Right PlaceHolder</el-col>
      <!-- 设置底部距离的 -->
      <el-backtop :bottom="60">
        <div class="backtop-icon">
          <svg-icon icon-class="top" style="color: black;"/>
        </div>
      </el-backtop>
    </el-row>
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
import Emoji from '@/components/Emoji'
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
    Emoji,
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
.el-container {
  display: block;
}

.publish {
  margin-bottom: 20px;
}

.author {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  width: 100%;
  margin-bottom: 20px;
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
</style>
