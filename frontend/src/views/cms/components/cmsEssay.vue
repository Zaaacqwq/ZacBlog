<!-- notes page -->
<template>
  <div class="no-caret">
    <section class="essay-shell">
      <div class="essay-grid">
        <div class="essay-main">
          <el-container class="essay-container">
          <el-timeline class="animate__animated animate__fadeInLeft essay-list">
            <el-timeline-item :color="color" v-for="essay in essayList" :key="essay.id" :timestamp="essay.createTime"
              placement="top">
              <el-card class="essay-card">
                <div class="essay-stamp">{{ essay.createTime }}</div>
                <h2 v-if="essay.title">{{ essay.title }}</h2>
                <div v-if="essay.contentType === '1'" class="typo ql-editor essay-body" v-html="essay.content"></div>
                <div v-else class="essay-body" v-html="essay.content"></div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          </el-container>
        </div>
        <div class="essay-side">
          <RightSidebar />
        </div>
      </div>
      <!-- 设置底部距离的 -->
      <el-backtop :bottom="60">
        <div class="backtop-icon">
          <svg-icon icon-class="top" style="color: black;"/>
        </div>
      </el-backtop>
    </section>
  </div>
</template>

<script>
// import 'cherry-markdown/dist/cherry-markdown.min.css'
//使用prism.js代码高亮
import '@/views/cms/plugins/prism.js'
import '@/views/cms/plugins/prism.css'
import {
  cmsEssayList,
} from "@/api/cms/blog";
import RightSidebar from "./rightSidebar/rightSidebar.vue";
export default {
  components: {
    RightSidebar,
  },
  name: 'essay',
  data() {
    return {
      essayList: [],
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
      },
      color: "#787878",
    }
  },
  created() {
    this.getEssayList()
  },
  methods: {
    async getEssayList() {
      cmsEssayList(this.queryParams).then(response => {
        this.essayList = response.rows;
      });
    }
  }
}
</script>

<style scoped>
.essay-shell {
  width: min(1360px, calc(100vw - 24px));
  margin: 0 auto;
  padding-top: 132px;
}

.essay-grid {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.essay-main {
  min-width: 0;
}

.essay-side {
  width: 320px;
  background: transparent;
}

.essay-container {
  gap: 18px;
}

.el-timeline {
  font: 16px/1.5 'Microsoft Yahei', 'PingFang SC', 'Hiragino Sans GB', sans-serif !important;
  width: 88%;
  margin: 0 auto;
}

.essay-list {
  width: 100%;
}

.essay-card {
  border-radius: 26px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 50px rgba(20, 28, 43, 0.08);
}

.essay-card h2 {
  margin: 0 0 10px;
  color: #1d2433;
  font-family: "Iowan Old Style", "Palatino Linotype", Georgia, serif;
}

.essay-stamp {
  margin-bottom: 12px;
  color: rgba(29, 36, 51, 0.52);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.essay-body {
  color: rgba(29, 36, 51, 0.78);
}

@media screen and (max-width: 768px) {
  .essay-shell {
    width: calc(100vw - 12px);
    padding-top: 112px;
  }

  .essay-grid {
    grid-template-columns: 1fr;
  }

  .essay-side {
    width: 100%;
  }

  .el-timeline {
    width: 98%;
    padding: 2px;
  }

  .el-timeline /deep/ .el-timeline-item__wrapper {
    padding-left: 15px !important;
  }
}
</style>
