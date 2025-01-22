<template>
  <div class="no-caret">
    <el-row :gutter="20" style="margin: 100px 0px;">
      <el-col :sm="2" :lg="5" class="hidden-xs-only" style="opacity:0;">Left PlaceHolder</el-col>
      <el-col :xs="24" :sm="15" :lg="11">
        <el-container>
          <el-timeline class="animate__animated animate__fadeInLeft">
            <el-timeline-item :color="color" v-for="essay in essayList" :key="essay.id" :timestamp="essay.createTime"
              placement="top">
              <el-card
                style="letter-spacing: 1px;border: 1px solid rgba(220, 220, 220,0.9); background-color: rgba(255,255,255,0.9);box-shadow: 0 0 30px -10px ">
                <h2 v-if="essay.title" style="color: rgba(0,0,0,1);">{{ essay.title }}</h2>
                <div v-if="essay.contentType === '1'" class="typo ql-editor" v-html="essay.content"></div>
                <div v-else v-html="essay.content"></div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
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
    },
  }
}
</script>

<style scoped>
.el-timeline {
  font: 16px/1.5 'Microsoft Yahei', 'PingFang SC', 'Hiragino Sans GB', sans-serif !important;

  width: 80%;
  margin: 0 auto;
}

.el-card {
  border-radius: 20px;
  box-shadow: 0 0 15px 5px white;
}

@media screen and (max-width: 768px) {
  .el-timeline {
    width: 98%;
    padding: 2px;
  }

  .el-timeline /deep/ .el-timeline-item__wrapper {
    padding-left: 15px !important;
  }
}
</style>
