<!-- admin panel group -->
<template>
  <el-row :gutter="40" class="panel-group">
    <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
      <div class="card-panel" @click="handleSetLineChartData('views')">
        <div class="card-panel-icon-wrapper icon-people">
          <i class="el-icon-reading card-panel-icon"></i>
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">
            Total Views
          </div>
          <count-to :key="'views-'+Number(views)" :start-val="0" :end-val="Number(views)" :duration="2600" class="card-panel-num" />
        </div>
      </div>
    </el-col>
    <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
      <div class="card-panel" @click="handleSetLineChartData('blog')">
        <div class="card-panel-icon-wrapper icon-message">
          <i class="el-icon-document card-panel-icon"></i>
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">
            Total Blogs
          </div>
          <count-to :key="'blog-'+Number(blog)" :start-val="0" :end-val="Number(blog)" :duration="3000" class="card-panel-num" />
        </div>
      </div>
    </el-col>
    <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
      <div class="card-panel" @click="handleSetLineChartData('comment')">
        <div class="card-panel-icon-wrapper icon-money">
          <i class="el-icon-chat-dot-square card-panel-icon"></i>
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">
            Total Comments
          </div>
          <count-to :key="'comment-'+Number(comment)" :start-val="0" :end-val="Number(comment)" :duration="3200" class="card-panel-num" />
        </div>
      </div>
    </el-col>
    <el-col :xs="12" :sm="12" :lg="6" class="card-panel-col">
      <div class="card-panel" @click="handleSetLineChartData('message')">
        <div class="card-panel-icon-wrapper icon-shopping">
          <i class="el-icon-message card-panel-icon"></i>
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">
            Total Messages
          </div>
          <count-to :key="'message-'+Number(message)" :start-val="0" :end-val="Number(message)" :duration="3600" class="card-panel-num" />
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script>
import CountTo from 'vue-count-to'
import {
    total,
  } from "@/api/cms/charts";
export default {
  components: {
    CountTo
  },
  data() {
    return{
      views: 0,
      blog: 0,
      comment: 0,
      message: 0,
    }
  },
  mounted() {
    // Ensure DOM is ready so CountTo paints reliably
    this.$nextTick(() => this.getTotal())
  },
  methods: {
    getTotal(){
      total().then(response => {
        this.views = response.views;
        this.blog = response.blog;
        this.comment = response.comment;
        this.message = response.message;
      });
    },
    handleSetLineChartData(type) {
      this.$emit('handleSetLineChartData', type)
    }
  }
}
</script>

<style lang="scss" scoped>
.panel-group {
  margin: 0 0 28px;

  .card-panel-col {
    margin-bottom: 20px;
  }

  .card-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 138px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    color: #5e6777;
    background: linear-gradient(180deg, rgba(255, 253, 249, 0.98) 0%, rgba(249, 246, 239, 0.96) 100%);
    border: 1px solid rgba(23, 32, 51, 0.08);
    border-radius: 28px;
    box-shadow: 0 24px 60px rgba(18, 27, 43, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.78);
    transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;

    &::after {
      content: '';
      position: absolute;
      inset: auto -20% -45% auto;
      width: 180px;
      height: 180px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(174, 123, 50, 0.08), transparent 68%);
      pointer-events: none;
    }

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 28px 70px rgba(18, 27, 43, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.78);
      border-color: rgba(174, 123, 50, 0.18);

      .card-panel-icon-wrapper {
        transform: scale(1.04);
      }

      .icon-people {
        background: rgba(64, 201, 198, 0.14);
      }

      .icon-message {
        background: rgba(54, 163, 247, 0.14);
      }

      .icon-money {
        background: rgba(244, 81, 108, 0.14);
      }

      .icon-shopping {
        background: rgba(52, 191, 163, 0.14)
      }
    }

    .icon-people {
      color: #40c9c6;
    }

    .icon-message {
      color: #36a3f7;
    }

    .icon-money {
      color: #f4516c;
    }

    .icon-shopping {
      color: #34bfa3
    }

    .card-panel-icon-wrapper {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 72px;
      width: 72px;
      height: 72px;
      margin-left: 22px;
      border-radius: 24px;
      background: rgba(23, 32, 51, 0.04);
      transition: transform .2s ease, background .2s ease;
    }

    .card-panel-icon {
      font-size: 34px;
    }

    .card-panel-description {
      display: flex;
      flex: 1;
      flex-direction: column;
      align-items: flex-end;
      margin: 0;
      padding: 0 24px 0 18px;
      text-align: right;

      .card-panel-text {
        margin-bottom: 10px;
        color: rgba(29, 36, 51, 0.52);
        font-size: 13px;
        font-weight: 700;
        letter-spacing: 0.08em;
        line-height: 1.2;
        text-transform: uppercase;
      }

      .card-panel-num {
        color: #172033;
        font-size: 34px;
        font-weight: 700;
        line-height: 1;
        letter-spacing: -0.04em;
      }
    }
  }
}

@media (max-width:550px) {
  .card-panel-description {
    display: none;
  }

  .card-panel-icon-wrapper {
    float: none !important;
    width: 100%;
    height: 100%;
    margin: 0 !important;

    .svg-icon {
      display: block;
      margin: 14px auto !important;
      float: none !important;
    }
  }
}
</style>
