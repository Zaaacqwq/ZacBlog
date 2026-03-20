<template>
  <transition name="fade">
    <div class="input-wrapper" v-if="show" @click.stop>
      <div class="composer-surface">
        <el-input class="gray-bg-input"
        maxlength="100" show-word-limit
                  v-model="inputComment"
                  type="textarea"
                  :rows="3"
                  @focus="inputFocus"
                  @blur="blur"
                  :placeholder="toComment">
        </el-input>
      </div>
      <transition name="fade2">
        <div class="btn-control" v-show="controlShow">
          <div class="action-row">
            <el-button class="action-button cancel-button" @click.stop="cancel">Cancel</el-button>
            <el-button class="action-button confirm-button" @click.stop="commitComment">Confirm</el-button>
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script>
  export default {
    props: {
      //控制整个组件是否显示
      show: {
        type: Boolean,
        required: true,
      },
      //传入input框的默认值
      value: {
        type: String
      },
      //传入input框的默认值
      toComment: {
        type: String
      },
      //传入input框的默认值
      toId: {
        type: Number
      },
      //类型，end(文章末尾处), comment(评论里),
      type: {
        type: String,
        // default: 'comment'
      }
    },
    data() {
      return {
        inputComment: '',
        //确定取消按钮是否显示
        controlShow: false,
        cursorIndexStart: null,//光标选中开始的位置
        cursorIndexEnd: null,//光标选中结束的位置
      }
    },
    computed: {},
    methods: {
      /**
       * 点击取消按钮
       */
      cancel() {
        if (this.type === 'end') {
          this.controlShow = false
        }
        this.$emit("cancel")
      },

      /**
       * 提交评论
       */
      commitComment() {
        this.$emit("confirm", {'inputComment':this.inputComment,'id':this.toId})
        this.inputComment = ""
      },

      //input活得焦点时调用
      inputFocus() {
        // console.log("focus");
        if (this.type === 'end') {
          this.controlShow = true
        }
      },
      blur(e){
        this.cursorIndexStart = e.srcElement.selectionStart  // 获取input输入框失去焦点时光标选中开始的位置
        this.cursorIndexEnd = e.srcElement.selectionEnd  // 获取input输入框失去焦点时光标选中结束的位置
      }

    },
    watch: {
      //监听toComment更新，赋值给name
      toComment: function () {
        this.inputComment = ''
      },
      toId: function () {
        this.inputComment = ''
      }
    },
    mounted() {
      if (this.type === 'end') {
        this.controlShow = false
      } else {
        this.controlShow = true
      }
      // console.log(this.controlShow)
    }
  }
</script>

<style>
.fade-enter-active, fade-leave-active {
  transition: opacity 0.5s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}

.input-wrapper {
  padding: 8px 0 4px;
}

.input-wrapper .fade2-enter-active,
.input-wrapper .fade2-leave-active {
  transition: opacity 0.5s;
}

.input-wrapper .fade2-enter,
.input-wrapper .fade2-leave-to {
  opacity: 0;
}

.input-wrapper .composer-surface {
  padding: 12px;
  border: 1px solid rgba(29, 36, 51, 0.06);
  border-radius: 18px;
  background: rgba(245, 246, 247, 0.72);
}

.input-wrapper .composer-surface .el-textarea__inner,
.input-wrapper .composer-surface textarea {
  min-height: 132px !important;
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
  outline: none;
  resize: none;
}

.input-wrapper .composer-surface .el-textarea__inner:focus,
.input-wrapper .composer-surface textarea:focus {
  border: 0;
  box-shadow: none;
  outline: none;
}

.input-wrapper .composer-surface .el-input__count {
  background: transparent;
  color: rgba(29, 36, 51, 0.42);
  right: 0;
  bottom: 0;
}

.input-wrapper .btn-control {
  padding-top: 10px;
}

.input-wrapper .btn-control .action-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.input-wrapper .btn-control .action-button {
  min-width: 92px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 16px;
  background: #f2f2f2;
  color: #1d2433;
  font-weight: 500;
  box-shadow: none;
}

.input-wrapper .btn-control .action-button:hover,
.input-wrapper .btn-control .action-button:focus {
  background: #ebebeb;
  color: #111827;
}
</style>
