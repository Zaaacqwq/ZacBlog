<!--评论模块-->
<template>
  <div class="container">
    <div class="comment" v-for="item in comments" :key="item.id">
      <div class="comment-main" @click="showCommentInput(item)">
        <div class="info" :id="item.id">
          <el-avatar v-if="item.avatar !== '' && item.avatar != null" :src="item.avatar"></el-avatar>
          <el-avatar v-else icon="el-icon-user-solid"></el-avatar>
          <div class="right">
            <div class="name">{{ item.createBy }}</div>
            <div class="date">{{ item.createTime }}</div>
          </div>
        </div>
        <div class="content">{{ item.content }}</div>
        <div class="control">
          <span class="like" :class="{ active: isLiked(item) }" @click.stop="likeClick(item)">
            <img
              class="heart-icon"
              :src="isLiked(item) ? heartFillIcon : heartLineIcon"
              alt=""
            >
            <span class="like-num">{{ item.likeNum || 0 }}</span>
          </span>
          <span class="reply-hint">Click comment to reply</span>
        </div>
      </div>

      <div
        v-if="(item.children && item.children.length > 0) || showItemId === item.id"
        class="reply"
      >
        <div
          class="item"
          v-for="reply in item.children"
          :key="reply.id"
          :id="reply.id"
          @click="showCommentInput(item, reply)"
        >
          <div class="reply-content">
            <span class="from-name">{{ reply.createBy }}</span><span>: </span>
            <span class="to-name" v-show="reply.parentId != reply.mainId">@{{ reply.pcreateBy }}</span>
            <span v-if="reply.delFlag !== '1'">{{ reply.content }}</span>
            <span v-else style="color: #909399;">The comment has been deleted!</span>
          </div>
          <div class="reply-bottom">
            <span>{{ reply.createTime }}</span>
          </div>
          <input-component
            :show="showItemId === item.id && showReplyId === reply.id"
            :value="inputComment"
            :toComment="name"
            :toId="id"
            @cancel="cancelInput"
            @confirm="commitComment"
          />
        </div>
        <input-component
          :show="showItemId === item.id && showReplyId === null"
          :value="inputComment"
          :toComment="name"
          :toId="id"
          @cancel="cancelInput"
          @confirm="commitComment"
        />
      </div>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import InputComponent from './InputComponent'
import heartLineIcon from '@/assets/images/heart-line.svg'
import heartFillIcon from '@/assets/images/heart-fill.svg'
import { getToken } from '@/utils/auth'
import { addCmsCommentLike, delCmsCommentLike } from '@/api/cms/comment'

export default {
  props: {
    comments: {
      type: Array,
      required: true
    }
  },
  components: {
    'input-component': InputComponent
  },
  data() {
    return {
      inputComment: '',
      name: '',
      id: null,
      mainId: null,
      showItemId: '',
      showReplyId: null,
      heartLineIcon,
      heartFillIcon,
      commentLikeForm: {},
    }
  },
  methods: {
    isLiked(item) {
      return !!item.isLike
    },
    reset() {
      this.commentLikeForm = {
        commentId: null,
        userId: null,
        likeNum: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.resetForm('commentLikeForm');
    },
    addCommentLike(item) {
      let token = getToken();
      this.reset();
      if (token == null || token == '') {
        this.commentLikeForm.createBy = 'Anonymous User'
        this.commentLikeForm.commentId = item.id
        this.commentLikeForm.likeNum = item.likeNum
      } else {
        this.commentLikeForm.createBy = this.$store.getters.name
        this.commentLikeForm.commentId = item.id
        this.commentLikeForm.likeNum = item.likeNum
      }
      addCmsCommentLike(this.commentLikeForm).then(() => {
        this.reset();
      });
    },
    delCommentLike(item) {
      let token = getToken();
      this.reset();
      if (token == null || token == '') {
        this.commentLikeForm.createBy = 'Anonymous User'
        this.commentLikeForm.commentId = item.id
        this.commentLikeForm.likeNum = item.likeNum
      } else {
        this.commentLikeForm.createBy = this.$store.getters.name
        this.commentLikeForm.commentId = item.id
        this.commentLikeForm.likeNum = item.likeNum
      }
      delCmsCommentLike(this.commentLikeForm).then(() => {
        this.reset();
      });
    },
    likeClick(item) {
      if (item.isLike === null || item.isLike === undefined) {
        Vue.$set(item, 'isLike', true);
        item.likeNum++
        this.addCommentLike(item)
      } else {
        if (item.isLike) {
          item.likeNum--
          this.delCommentLike(item)
        } else {
          item.likeNum++
          this.addCommentLike(item)
        }
        item.isLike = !item.isLike;
      }
    },
    cancelInput() {
      this.showItemId = ''
      this.showReplyId = null
      this.mainId = null
    },
    commitComment(value) {
      this.$emit('replyConfirm', {
        ...value,
        mainId: this.mainId
      })
    },
    showCommentInput(item, reply) {
      this.mainId = item.id
      if (reply) {
        this.inputComment = ''
        this.name = 'Reply@' + reply.createBy + ':'
        this.id = reply.id
        this.showReplyId = reply.id
      } else {
        this.inputComment = ''
        this.name = 'Leave your comment'
        this.id = item.id
        this.showReplyId = null
      }
      this.showItemId = item.id
    }
  }
}
</script>

<style scoped rel="stylesheet/scss" lang="scss">
.container {
  padding: 0 10px;
  box-sizing: border-box;

  .comment {
    display: flex;
    flex-direction: column;
    padding: 18px 18px 12px;
    margin-bottom: 14px;
    border: 1px solid rgba(29, 36, 51, 0.06);
    border-radius: 22px;
    box-shadow: 0 8px 20px rgba(17, 24, 39, 0.035);
    transition: background-color 0.2s ease, transform 0.2s ease;

    &:hover {
      background: rgba(17, 24, 39, 0.02);
      box-shadow: 0 10px 24px rgba(17, 24, 39, 0.05);
    }

    .comment-main {
      cursor: pointer;
    }

    .info {
      display: flex;
      align-items: center;

      .right {
        display: flex;
        flex-direction: column;
        margin-left: 10px;

        .name {
          font-size: 16px;
          color: #303133;
          margin-bottom: 5px;
          font-weight: 500;
        }

        .date {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .content {
      font-size: 16px;
      color: #303133;
      line-height: 20px;
      padding: 10px 0;
    }

    .control {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 14px;
      color: #909399;

      .like {
        min-width: 56px;
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 6px 10px;
        border-radius: 999px;
        background: rgba(17, 24, 39, 0.04);
        cursor: pointer;
        user-select: none;

        &.active,
        &:hover {
          color: #d598a8;
          background: rgba(213, 152, 168, 0.1);
        }

        .heart-icon {
          width: 14px;
          height: 14px;
          display: block;
          object-fit: contain;
        }

        .like-num {
          font-size: 13px;
          font-weight: 600;
        }
      }

      .reply-hint {
        font-size: 12px;
        letter-spacing: 0.04em;
        color: #9ca3af;
      }
    }

    .reply {
      margin: 14px 0 4px;
      padding: 12px 14px;
      border-radius: 18px;
      background: rgba(17, 24, 39, 0.025);
      border: 1px solid rgba(29, 36, 51, 0.05);

      .item {
        margin: 0;
        padding: 12px 14px;
        border-radius: 14px;
        border-bottom: 1px solid rgba(29, 36, 51, 0.06);
        cursor: pointer;
        transition: color 0.2s ease, background-color 0.2s ease;

        &:hover {
          color: #111827;
          background: rgba(255, 255, 255, 0.58);
        }

        &:first-child {
          padding-top: 4px;
        }

        &:last-child {
          border-bottom: 0;
          padding-bottom: 4px;
        }

        .reply-content {
          display: block;
          font-size: 14px;
          color: #303133;
          line-height: 1.7;

          .from-name {
            color: #111827;
            font-weight: 600;
          }

          .to-name {
            color: #6b7280;
            margin-left: 6px;
            margin-right: 6px;
          }
        }

        .reply-bottom {
          display: flex;
          align-items: center;
          margin-top: 8px;
          font-size: 12px;
          color: #9ca3af;
        }
      }
    }
  }
}
</style>
