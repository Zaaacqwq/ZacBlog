<template>
  <el-row :gutter="20" style="margin: 100px 0px;">
    <el-col :sm="2" :lg="5" class="hidden-xs-only" style="opacity:0;">Left PlaceHolder</el-col>
    <el-col :xs="24" :sm="15" :lg="11">
      <el-card style="background-color: rgba(255, 255, 255,1)" class="first-card">
        <div slot="header" class="total blog-info">
          <div class="user-info">
            <i class="el-icon-user"></i>
            <span class="header">  {{blog.createBy}}</span>
          </div>
          <div class="blog-date">
            <i class="el-icon-date"></i>
            <span>  {{blog.createTime}}</span>
          </div>
          <div class="blog-views">
            <i class="el-icon-view"></i>
            <span>  {{blog.views}}</span>
          </div>
        </div>
        <h2 class="blog-title header">{{blog.title}}
          <el-tag size="mini" v-for="tag in blog.types" :key="tag.typeId" type="info">{{tag.typeName}}</el-tag>
        </h2>
        <div  v-if="blog.contentType ==='1'" class="typo m-padded-lr-responsive m-padded-tb-large ql-editor" v-html="blog.content"></div>
        <div  v-if="blog.contentType ==='3'" v-html="blog.content"></div>
        <CherryMarkdown ref="CherryMarkdown" v-if="blog.contentType ==='2'" v-model='blog.contentMarkdown' :defaultModel="'previewOnly'"></CherryMarkdown>
        <div class="tags">
          <div class="tag-item" v-for="tag in blog.tags" :key="tag.tagId">
            <div class="sjx-outer">
              <div class="sjx-inner"></div>
            </div>
            <div class="tag">
              {{tag.tagName}}
            </div>
          </div>
        </div>
        <!-- <div class="appreciate">
          <el-popover
                  placement="bottom"
                  title=""
                  width="300"
                  trigger="hover"
                  content="这是一段内容,这是一段内容,这是一段内容,这是一段内容。">
            <el-button class="zanshang" slot="reference" type="danger" round plain>赞赏</el-button>
          </el-popover>
        </div> -->
        <el-table :data="blog.blogFilesNew" :border="true" style="width: 99.99%;">
          <el-table-column align="center" min-width="30%" prop="remark" label="Attachment">
            <template slot-scope="scope">
              <el-row>
                <el-col :span="6"><div class="blogFilesInfoName">Name:</div></el-col>
                <el-col :span="18"><el-input v-model="scope.row.fileOriginName" disabled/></el-col>
              </el-row>
              <el-row style="margin-top: 4px;">
                <el-col :span="6"><div class="blogFilesInfoName">Size:</div></el-col>
                <el-col :span="18"><el-input v-model="scope.row.fileSize" disabled/></el-col>
              </el-row>
              <el-row style="margin-top: 4px;">
                <el-col :span="6"><div class="blogFilesInfoName">Type:</div></el-col>
                <el-col :span="18"><el-input v-model="scope.row.fileSuffix" disabled/></el-col>
              </el-row>
            </template>
          </el-table-column>
          <el-table-column align="center" min-width="50%" prop="remark" label="Remark">
            <template slot-scope="scope">
              <el-input v-model="scope.row.remark" type="textarea" :rows="6" size="small" disabled />
            </template>
          </el-table-column>
          <el-table-column align="center" min-width="20%" label="Action">
            <template slot-scope="scope">
              <el-button size="mini" plain @click="handleDownload(scope.row)">Download</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="author no-caret">
          <ul>
            <li>Author: {{blog.createBy}}</li>
            <li>Time Published: {{blog.createTime}}</li>
          </ul>
        </div>
        <el-card shadow="never" class="comments no-caret">
          <div class="header" style="padding-bottom: 10px;">
            Comments
          </div>
          <comment></comment>
        </el-card>
      </el-card>
    </el-col>
    <el-col :xs="24" :sm="5" :lg="3" class="right-sidebar">
      <RightSidebar />
    </el-col>
    <el-col :sm="2" :lg="5" class="hidden-xs-only" style="opacity:0;">Right PlaceHolder</el-col>
    <!-- 设置底部距离的 -->
    <!-- <el-backtop :bottom="60">
        <div style="
            height: 50px;
            width: 50px;
            background-color: rgba(240,239,241,1);
            box-shadow: 0 0 6px rgba(0,0,0, .12);
            text-align: center;
            line-height: 40px;
            border-radius:2px;
            color: #686868;
          ">
          <svg-icon icon-class="top" />
        </div>
      </el-backtop> -->
    <el-backtop :bottom="60">
        <div class="backtop-icon">
          <svg-icon icon-class="top" style="color: black;"/>
        </div>
      </el-backtop>
  </el-row>
</template>

<script>
// import 'cherry-markdown/dist/cherry-markdown.min.css'
//使用prism.js代码高亮
import '@/views/cms/plugins/prism.js'
import '@/views/cms/plugins/prism.css'
import comment from "./comment/Ipcomment"
import {
    getBlogDetail,
    addBlogViews,
  } from "@/api/cms/blog";
import {mapState} from 'vuex'
import CherryMarkdown from '@/components/CherryMarkdown'
import RightSidebar from "./rightSidebar/rightSidebar.vue";
export default {
  components: {
    comment,
    CherryMarkdown,
    RightSidebar,
  },
  data() {
    return {
      blog: {},
      commentForm: {
        content: ''
      },
    }
  },
  watch: {
    '$route' (to, from) {
      this.$router.go(0);
    }
  },
  created() {
    this.getBlogInfomation()
  },
  computed: {
    ...mapState([
      'userInfo',
      'administrator',
    ])
  },
  methods: {
    // 获取博客详情信息
    async getBlogInfomation() {
      // 增加阅读量
      addBlogViews(this.$route.query.id);
      getBlogDetail(this.$route.query.id).then(response => {
        const {data: res} = response;
          this.blog = response.data
          this.blog.blogFilesNew = []
          if (response.data.blogFiles !== null) {
            this.blog.blogFilesNew = JSON.parse(response.data.blogFiles)
          }
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
  },

}
</script>

<style scoped>

  .el-card {
    width: 100%;
  }

  .el-popper /deep/ {
    box-shadow: 0 2px 4px 0 rgb(34 36 38 / 12%),
  }

  .first-card {
    border-radius: 10px 10px 10px 10px;
    position: relative;
    padding-bottom: 10px;
    /*text-align: center;*/
    font: 300 1em/1.8 PingFang SC, Lantinghei SC, Microsoft Yahei, Hiragino Sans GB, Microsoft Sans Serif, WenQuanYi Micro Hei, sans-serif;

  }

  hr.style-one {
    width: 100%;
    background-image: linear-gradient(to right, rgba(64, 158, 255, 0), rgba(64, 158, 255, 0.75), rgba(64, 158, 255, 0));
  }

  .appreciate {
    text-align: center;
  }

  .tags {
    display: flex;
    align-items: center;
    margin-left: 50px;
    margin-top: 20px;
  }

  .tag-item {
    display: flex;
    justify-content: space-around;
    align-items: center;
    margin-left: 10px;
    margin-bottom: 20px;
  }

  .tag {
    padding-left: 10px;
    padding-right: 10px;
    border-radius: 5px;
    background-color: #ecf5ff;
    border: 1px solid #686868;
    color: #686868;
    display: flex;
  }

  .sjx-outer {
    width: 0;
    height: 0;
    border-top: 7px solid transparent;
    border-bottom: 7px solid transparent;
    border-right: 7px solid #686868;
    position: relative;
  }

  .sjx-inner {
    border-top: 7px solid transparent;
    border-bottom: 7px solid transparent;
    border-right: 7px solid #ecf5ff;
    top: -7px;
    left: 1px;
    position: absolute;
  }

  .author {
    text-align: left;
    background-color: #fcfff5;
    box-shadow: 0 0 0 1px #a3c293 inset;
    color: #2c662d;
    width: 100%;
    position: absolute;
    left: 0;
    margin: 20px 0;
    padding: 20px 0;
    font-size: small;
    font-family: PingFang SC, Lantinghei SC, Microsoft Yahei, Hiragino Sans GB, Microsoft Sans Serif, WenQuanYi Micro Hei, sans-serif;
  }

  .comments {
    margin-top: 150px;
    box-shadow: 0 1px 2px 0 rgb(34 36 38 / 15%);
    border: 1px solid rgba(34, 36, 38, .15);
    border-top: 2px solid #686868;
    text-align: left;
  }
  .blog-title {
    text-align: center;
  }

  .blog-info {
    display: flex;
    align-items: center;
    color: rgba(0, 0, 0, .4);
    font-size: 13px;
  }
  .blog-date {
    margin-right: 5px;
    float: right;
  }

  .blog-views {
    margin-right: 5px;
    float: right;
  }

  .user-info {
    justify-content: space-around;
    align-items: center;
    margin-right: 15px;
    float: left;
  }

  .header {
    text-decoration: none;
    color: #686868;
    font-weight: bold;
  }

  @media screen and (max-width: 768px) {
    .tags {
      margin-left: 0;
      margin-top: 20px;
    }

    hr {
      display: none;
    }

    .comment-content {
      font-size: 12px !important;
    }
  }

  @media only screen and (max-width: 480px) {
    h2 {
      font-weight: normal;
    }

    code, pre {
      font-size: 13px !important;
    }
  }
  .blogFilesInfoName {
    text-align: center;
    padding-top: 5px;
  }
</style>
