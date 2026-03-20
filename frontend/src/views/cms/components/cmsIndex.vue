<template>
  <div class="cms-home no-caret">
    <el-row
      :gutter="20"
      class="full-page-image hero-section"
      :style="`background-image: url(${backgroundImage});`"
    >
      <el-col :xs="24" :sm="24" :lg="24" class="full-page-content">
        <h1 class="blog-title">Zac's Blog</h1>
        <p class="blog-description">A beautiful, customized, personal blog</p>
      </el-col>
      <WaveComponent />
      <div
        style="
          position: absolute;
          bottom: -100px;
          width: 100%;
          height: 100px;
          background: linear-gradient(
            to bottom,
            rgba(255, 255, 255, 1) 0%,
            rgba(255, 255, 255, 0) 100%
          );
        "
      ></div>
    </el-row>
    <section id="index" class="content home-content">
      <div class="home-grid">
        <div class="home-main">
          <el-card class="left-item content-panel">
            <div slot="header" class="total">
              <div class="titleIndex">
                <i
                  v-if="selected"
                  class="el-icon-back"
                  @click="updateBlogList"
                ></i>
                <span>{{ selectMethod }}</span>
              </div>
              <!-- <span>共 <span style="color: #3a8ee6; font-size: 20px">{{totalcount}}</span> 篇</span> -->
            </div>
            <el-row
              type="flex"
              align="middle"
              style="flex-wrap: wrap"
              :gutter="20"
              v-for="blog in blogList"
              :key="blog.id"
              shadow="never"
              class="blog-content"
            >
              <div @click="getBlogInfo(blog.id)">
                <el-col class="img" :xs="24" :sm="6">
                  <el-image
                    v-if="blog.blogPicType == '0'"
                    lazy
                    :src="blog.blogPicLink"
                  >
                    <div slot="error" class="image-slot">
                      <el-image src="/errorImg.jpg" fit="cover" class="blogPic"
                        >></el-image
                      >
                    </div>
                  </el-image>
                  <el-image
                    v-if="blog.blogPicType == '1'"
                    lazy
                    :src="blog.blogPic"
                  ></el-image>
                </el-col>
                <el-col
                  :xs="24"
                  :sm="18"
                  style="
                    padding-left: 10px;
                    padding-right: 10px;
                    margin-bottom: 5px;
                    margin-top: -5px;
                  "
                >
                  <div>
                    <h3>
                      <svg-icon icon-class="top" v-show="blog.top == 1" />
                      {{ blog.title }}
                    </h3>
                    <div style="margin-bottom: 10px">
                      <span style="color: rgba(0, 0, 0, 0.4)">
                        {{ blog.blogDesc }}</span
                      >
                    </div>
                    <div style="margin-bottom: 10px">
                      <el-tag
                        effect="plain"
                        size="mini"
                        v-for="tag in blog.tags"
                        :key="tag.tagId"
                        type="success"
                      >
                        {{ tag.tagName }}
                      </el-tag>
                    </div>
                    <div class="blog-info">
                      <div class="user-info">
                        <i class="el-icon-user"></i>
                        <span class="header"> {{ blog.createBy }}</span>
                      </div>
                      <div class="blog-date">
                        <i class="el-icon-date"></i>
                        <span> {{ blog.createTime }}</span>
                      </div>
                      <div>
                        <i class="el-icon-view"></i>
                        <span> {{ blog.views }}</span>
                      </div>
                      <div class="blog-type">
                        <el-tag
                          size="mini"
                          v-for="tag in blog.types"
                          :key="tag.typeId"
                          type="info"
                        >
                          {{ tag.typeName }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                </el-col>
              </div>
            </el-row>
            <pagination
              class="home-pagination"
              v-show="total > 0"
              :total="total"
              :page.sync="queryParams.pageNum"
              :limit.sync="queryParams.pageSize"
              background
              layout="total, sizes, prev, pager, next, jumper"
              @pagination="getBlogList"
            />
          </el-card>
        </div>
        <div class="home-side">
          <div class="side-stack">
            <RightSidebar />
            <el-card class="right-item side-panel unified-side-card">
              <div slot="header" class="attributes">
                <span class="side-eyebrow">Category</span>
              </div>
              <ul class="blog-type-ul side-list">
                <li
                  class="blog-type-li"
                  v-for="cmsType in typeList"
                  :key="cmsType.typeId"
                  @click="selectType(cmsType)"
                  :class="cmsType.typeId === typeId ? 'activeType' : ''"
                >
                  <div class="category-name">
                    {{ cmsType.typeName }}
                  </div>
                  <div>{{ cmsType.blogNum }}</div>
                </li>
              </ul>
              <div class="more" @click="dealType">
                <i v-if="moreType" class="el-icon-arrow-down"></i>
                <i v-else class="el-icon-arrow-up"></i>
              </div>
            </el-card>
            <el-card class="right-item side-panel unified-side-card">
              <div slot="header" class="attributes">
                <span class="side-eyebrow">Tags</span>
              </div>
              <div class="tags side-tags tag-cloud">
                <div
                  class="tag-item"
                  v-for="tag in tagList"
                  :key="tag.tagId"
                  @click="selectTag(tag)"
                  :class="tag.tagId === tagId ? 'activeTag' : ''"
                  :style="tagCloudStyle(tag)"
                >
                  <div class="tag tag-cloud-item">
                    {{ tag.tagName }}
                    {{ tag.blogNum }}
                  </div>
                </div>
              </div>
              <div class="more" @click="dealTag">
                <i v-if="moreTag" class="el-icon-arrow-down"></i>
                <i v-else class="el-icon-arrow-up"></i>
              </div>
            </el-card>
            <el-card class="right-item side-panel unified-side-card">
              <div slot="header" class="attributes">
                <span class="side-eyebrow">Latest Recommend</span>
              </div>
              <div
                class="recommend-blog l-text"
                v-for="blog in recommendList"
                :key="blog.id"
                @click="getBlogInfo(blog.id)"
              >
                <a class="recommend-a">{{ blog.title }}</a>
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import "element-ui/lib/theme-chalk/display.css";
import WaveComponent from "../WaveComponent.vue";
import backgroundImage from "@/assets/images/background.png";
import avatar from "@/assets/images/avatar.png";
import RightSidebar from "./rightSidebar/rightSidebar.vue";
import { Loading } from "element-ui";
import {
  cmsListBlog,
  getBlogDetail,
  cmsListByTypeId,
  cmsListByTagId,
  cmsListRecommend,
} from "@/api/cms/blog";
export default {
  name: "cmsIndex",
  data() {
    return {
      backgroundImage,
      avatar,
      latestAnnouncement: {},
      totalViews: 0,
      totalBlogs: 0,
      totalComments: 0,
      totalMessages: 0,
      // totalcount: 0,
      queryInfo: {
        query: "",
        pagenum: 1,
        pagesize: 8,
      },
      intro: "",
      blogList: [],
      typeList: [],
      tagList: [],
      fullTypeList: [],
      fullTagList: [],
      recommendList: [],
      selectMethod: "All Blogs",
      typeId: -1,
      tagId: -1,
      selected: false,
      moreType: true,
      moreTag: true,
      value: new Date(),
      timer: null,
      start: false,
      screenWidth: document.documentElement.clientWidth, //实时屏幕宽度
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        type: 1,
        content: null,
        top: null,
        views: null,
        status: null,
        createBy: null,
      },
      // 总条数
      total: 0,
    };
  },
  components: {
    WaveComponent,
    RightSidebar,
  },
  computed: {
    pagSmall() {
      return this.screenWidth <= 768;
    },
    // 计算分页栏样式
    pagLayout() {
      if (this.screenWidth < 768) {
        return "prev, pager, next";
      } else {
        return "total, prev, pager, next, jumper";
      }
    },
  },
  created() {
    window.addEventListener("resize", this.screenAdapter);
  },
  mounted() {
    this.$nextTick(function () {
      // 仅在整个视图都被渲染之后才会运行的代码
      this.getTypeList();
      this.getBlogList();
      this.getTagList();
      this.getRecommendList();
      let str =
        "This is my personal blog. I will share some content about programming, development and other aspects. I hope it can be helpful to you...";
      let idx = 0;
      let that = this;
      let timer = setTimeout(function fn() {
        // console.log(this.intro)
        that.intro = that.intro + str.substring(idx, idx + 1);
        idx++;
        if (idx > str.length) {
          that.intro = "";
          idx = 0;
        }
        setTimeout(fn, 200);
      }, 2000);

      this.screenWidth = document.documentElement.clientWidth;
    });
  },
  methods: {
    /** 获取博客列表 */
    getBlogList() {
      let loadingInstance = Loading.service({
        target: ".left-item",
      });
      cmsListBlog(this.queryParams)
        .then((response) => {
          this.blogList = this.picSrc(response.rows);
          this.total = response.total;
        })
        .finally(() => {
          loadingInstance.close();
        });
    },
    //首图地址修改
    picSrc(blogList) {
      for (let i = 0; i < blogList.length; i++) {
        let blogInfo = blogList[i];
        if (blogInfo.blogPic.length > 0) {
          blogList[i].blogPic = process.env.VUE_APP_BASE_API + blogInfo.blogPic;
        } else {
          blogList[i].blogPic = "/errorImg.jpg";
        }
      }
      return blogList;
    },
    // 开始进入主页
    startRead() {
      this.$nextTick(() => {
        document.getElementById("index").scrollIntoView({
          behavior: "smooth",
          block: "start",
          // inline: 'nearest'
        });
      });
    },
    compare(property) {
      return function (a, b) {
        let value1 = a[property].length;
        let value2 = b[property].length;
        return value2 - value1;
      };
    },
    // 获取推荐博客列表
    async getRecommendList() {
      cmsListRecommend(this.queryParams).then((response) => {
        const { data: res } = response;
        this.recommendList = response.rows.slice(0, 4);
        this.total = response.total;
      });
    },
    // 获取博客类型列表
    async getTypeList() {
      getBlogDetail(this.$route.query.id).then((response) => {
        for (let i = 0; i < response.types.length; i++) {
          let typeInfo = response.types[i];
          if (typeInfo.typePic.length > 0) {
            response.types[i].typePic =
              process.env.VUE_APP_BASE_API + typeInfo.typePic;
          }
        }
        const { data: res } = response;
        // Sort by blogNum desc, then typeName asc (case-insensitive)
        const sortedTypes = this.sortByCountThenAlpha(
          response.types,
          "blogNum",
          "typeName"
        );
        this.fullTypeList = sortedTypes;
        this.typeList = sortedTypes.slice(0, 4);
      });
    },
    // 获取博客标签列表
    async getTagList() {
      getBlogDetail(this.$route.query.id).then((response) => {
        const { data: res } = response;
        // Sort by blogNum desc, then tagName asc (case-insensitive)
        const sortedTags = this.sortByCountThenAlpha(
          response.tags,
          "blogNum",
          "tagName"
        );
        this.fullTagList = sortedTags;
        this.tagList = sortedTags.slice(0, 6);
      });
    },
    // 跳转到博客详情页
    getBlogInfo(blogId) {
      let routeUrl = this.$router.resolve({
        path: "/cms/main/blog",
        query: {
          id: blogId,
        },
      });
      window.open(routeUrl.href, "_blank");
    },
    // 修改当前页码
    handleCurrentChange(newSize) {
      this.queryInfo.pagenum = newSize;
      this.getBlogList();
    },
    // 修改当前页大小
    handleSizeChange(newSize) {
      this.queryInfo.pagesize = newSize;
    },
    // 按分类筛选博客
    async selectType(cmsType) {
      let loadingInstance = Loading.service({
        target: ".left-item",
      });
      this.typeId = cmsType.typeId;
      cmsListByTypeId(this.typeId)
        .then((response) => {
          this.blogList = this.picSrc(response.rows);
          this.total = response.total;
          // this.totalcount = res.data.totalElements
          this.selectMethod = "Category: " + cmsType.typeName;
          this.selected = true;
        })
        .finally(() => {
          loadingInstance.close();
        });
    },
    // 按标签筛选博客
    async selectTag(tag) {
      let loadingInstance = Loading.service({
        target: ".left-item",
      });
      this.tagId = tag.tagId;
      cmsListByTagId(this.tagId)
        .then((response) => {
          this.blogList = this.picSrc(response.rows);
          this.total = response.total;
          // this.totalcount = res.data.totalElements
          this.selectMethod = "Tag: " + tag.tagName;
          this.selected = true;
        })
        .finally(() => {
          loadingInstance.close();
        });
    },
    // 更新博客列表
    updateBlogList() {
      this.selected = false;
      this.typeId = -1;
      this.tagId = -1;
      this.selectMethod = "All Blogs";
      this.getBlogList();
    },
    // 得到所有的标签
    async getFullTagList() {
      this.tagList = this.fullTagList;
    },
    async dealType() {
      if (this.moreType) {
        this.typeList = this.fullTypeList;
      } else {
        this.typeList = this.fullTypeList.slice(0, 4);
      }
      this.moreType = !this.moreType;
    },
    async dealTag() {
      if (this.moreTag) {
        await this.getFullTagList();
      } else {
        this.tagList = this.fullTagList.slice(0, 6);
      }
      this.moreTag = !this.moreTag;
    },
    // 屏幕尺寸变化的监听函数
    screenAdapter() {
      this.screenWidth = document.documentElement.clientWidth;
    },
    // Generic sort: by count desc, then name asc (case-insensitive)
    sortByCountThenAlpha(list, countKey, nameKey) {
      if (!Array.isArray(list)) return [];
      // Create a shallow copy to avoid mutating the original
      return list.slice().sort((a, b) => {
        const ca = Number(a && a[countKey]) || 0;
        const cb = Number(b && b[countKey]) || 0;
        if (cb !== ca) return cb - ca;
        const na = (a && a[nameKey] ? String(a[nameKey]) : "").toLowerCase();
        const nb = (b && b[nameKey] ? String(b[nameKey]) : "").toLowerCase();
        return na.localeCompare(nb);
      });
    },
    tagCloudStyle(tag) {
      const max = this.fullTagList.length
        ? Math.max(...this.fullTagList.map((item) => Number(item.blogNum) || 0))
        : 1;
      const count = Number(tag && tag.blogNum) || 0;
      const ratio = max > 0 ? count / max : 0;
      const fontSize = 12 + Math.round(ratio * 10);
      const shade = 130 - Math.round(ratio * 113);
      return {
        "--tag-size": `${fontSize}px`,
        "--tag-color": `rgb(${shade}, ${shade}, ${shade})`,
      };
    },
  },
};
</script>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Chakra+Petch:ital,wght@0,300;0,400;0,500;0,600;0,700;1,300;1,400;1,500;1,600;1,700&family=Titillium+Web:ital,wght@0,200;0,300;0,400;0,600;0,700;0,900;1,200;1,300;1,400;1,600;1,700&display=swap");

.cms-home {
  margin-bottom: 56px;
}

.welcome {
  background-color: rgba(0, 0, 0, 0.1);
  border: none;
  height: 90%;
  position: relative;
}

.border {
  width: 812px;
  height: 112px;
  position: absolute;
  top: -6px;
  left: -6px;
  border: 3px solid white;
  box-sizing: border-box;
  animation: clipMe 5s linear infinite;
}

.tit {
  box-sizing: border-box;
  position: relative;
  width: 800px;
  height: 100px;
  line-height: 100px;
  box-shadow: inset 0 0 0 1px white;
  margin: 40px auto;
  margin-top: 80px;
  color: white;
  text-align: center;
  font-size: 50px;
  font-weight: normal;
  letter-spacing: 10px;
}

.intro {
  letter-spacing: 5px;
  line-height: 50px;
  width: 80%;
  margin: 0 auto;
  text-align: center;
  font-weight: normal;
  color: white;
}

.down {
  animation: bounce 2s infinite;
  animation-duration: 3s;
  font-size: 25px;
  position: absolute;
  bottom: 5px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 2px solid #fff;
}

.down:hover {
  animation: none;
  cursor: pointer;
  box-shadow: 0 0 20px 0 white;
  transition: all 0.2s;
}

.left-item .pagination-container {
  background: rgb(255, 255, 255, 0);
}

.left-item /deep/ .el-card__body {
  padding-bottom: 72px;
}

@keyframes clipMe {
  0%,
  100% {
    clip: rect(0px, 806px, 6px, 0px);
  }

  25% {
    clip: rect(0px, 6px, 112px, 0px);
  }

  50% {
    clip: rect(112px, 812px, 112px, 0px);
  }

  75% {
    clip: rect(0px, 812px, 112px, 806px);
  }
}

@keyframes bounce {
  0%,
  20%,
  50%,
  80%,
  100% {
    transform: translate(-50%, 0);
  }

  40% {
    transform: translate(-50%, -30px);
  }

  60% {
    transform: translate(-50%, -15px);
  }
}

.blog-type-ul {
  padding-left: 0;
  padding-right: 0;
  margin-bottom: 0;
  border-radius: 5px;
  list-style: none;
}

.hero-section {
  min-height: 100vh;
}

.full-page-image {
  height: 100vh;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  overflow: hidden;
}

.full-page-image::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    180deg,
    rgba(10, 16, 27, 0.34) 0%,
    rgba(10, 16, 27, 0.58) 100%
  );
  z-index: 1;
}

.full-page-content {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  text-align: center;
  color: white;
  z-index: 2;
}

.blog-title {
  font-family: "Chakra Petch", "Titillium Web", "Avenir Next", "Segoe UI", sans-serif;
  font-size: clamp(3.6rem, 9vw, 7.2rem);
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  text-shadow: 0 14px 40px rgba(0, 0, 0, 0.4);
  margin: 0;
  padding-bottom: 28px;
}

.blog-description {
  max-width: 760px;
  padding: 0 24px 120px;
  font-family: "Avenir Next", "Segoe UI", sans-serif;
  font-size: clamp(1.1rem, 2.6vw, 1.8rem);
  line-height: 1.6;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-shadow: 0 6px 24px rgba(0, 0, 0, 0.36);
  margin: 0;
}

.el-pagination {
  padding-bottom: 20px;
}

.home-pagination /deep/ .pagination-container {
  padding: 18px 12px 36px;
  background: transparent;
}

.home-pagination /deep/ .el-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: rgba(29, 36, 51, 0.6);
  font-weight: 500;
}

.home-pagination /deep/ .btn-prev,
.home-pagination /deep/ .btn-next,
.home-pagination /deep/ .el-pager li,
.home-pagination /deep/ .el-pagination__jump .el-input__inner,
.home-pagination /deep/ .el-pagination__sizes .el-input__inner {
  border: 1px solid rgba(29, 36, 51, 0.07);
  border-radius: 14px !important;
  background: #f5f5f5 !important;
  color: #1d2433 !important;
  box-shadow: none !important;
}

.home-pagination /deep/ .el-pager li {
  min-width: 36px;
  height: 36px;
  line-height: 34px;
  margin: 0 2px;
}

.home-pagination /deep/ .btn-prev,
.home-pagination /deep/ .btn-next {
  width: 36px;
  height: 36px;
  padding: 0;
}

.home-pagination /deep/ .el-pager li.active {
  background: #e8e8e8 !important;
  border-color: rgba(29, 36, 51, 0.1);
  color: #111827 !important;
  font-weight: 700;
}

.home-pagination /deep/ .btn-prev:hover,
.home-pagination /deep/ .btn-next:hover,
.home-pagination /deep/ .el-pager li:hover,
.home-pagination /deep/ .el-pagination__jump .el-input__inner:hover,
.home-pagination /deep/ .el-pagination__sizes .el-input__inner:hover {
  background: #eeeeee !important;
}

.el-card /deep/ .el-card__body {
  padding: 0;
}

.right-sidebar {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.right-item {
  margin-bottom: 20px;
  width: 100%;
}

.side-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.side-stack > .right-item {
  margin-bottom: 0;
}

.home-content {
  width: min(1360px, calc(100vw - 24px));
  margin: -92px auto 0;
  position: relative;
  z-index: 3;
}

.home-grid {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
  justify-content: center;
}

.home-main {
  min-width: 0;
}

.home-side {
  width: 320px;
}

.home-side,
.side-stack {
  width: 100%;
}

.content-panel,
.side-panel {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(29, 36, 51, 0.08);
  backdrop-filter: blur(18px);
}

.unified-side-card /deep/ .el-card__header {
  padding: 20px 20px 14px;
  border-bottom: 1px solid rgba(29, 36, 51, 0.08);
}

.unified-side-card /deep/ .el-card__body {
  padding: 0 20px 14px;
}

.side-eyebrow {
  display: inline-block;
  color: rgba(29, 36, 51, 0.5);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.left-item {
  overflow: hidden;
  padding-bottom: 36px;
}

.profile-card {
  text-align: center;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  margin: 10px 0px;
}

.stats {
  display: flex;
  justify-content: space-around;
  margin: 10px 0;
}

.social-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin: 10px 0;
}

.social-links a {
  margin: 0px 10px;
  text-decoration: none;
  color: #007bff;
}

.social-icon {
  width: 40px;
  height: 40px;
}

.custom-section {
  text-align: center;
  padding: 10px;
  font-size: 24px;
  line-height: 1.6;
}

.announcement-section {
  text-align: center;
  padding: 10px;
  font-size: 24px;
  line-height: 1.6;
  border-radius: 5px;
}

.site-info {
  padding: 10px;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 5px;
}

.blog-type-li:first-child {
  border-top: 0;
}

.blog-type-li {
  border-bottom: 1px solid rgba(29, 36, 51, 0.08);
}

.blog-type-li > div:first-child {
  color: rgba(29, 36, 51, 0.58);
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.blog-type-li > div:last-child {
  color: rgba(29, 36, 51, 0.82);
  font-size: 1.05rem;
  font-weight: 500;
  letter-spacing: normal;
  text-transform: none;
}

.more {
  text-align: center;
  color: #686868;
  padding: 8px;
}

.more:hover {
  cursor: pointer;
  color: #111111;
}

.blog-type-li:hover {
  background-color: rgba(230, 235, 245, 0.3);
  cursor: pointer;
}

.activeType {
  background-color: rgba(200, 200, 200, 0.3);
  cursor: pointer;
}

.category-name {
  display: inline-flex;
  align-items: center;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin: 15px 0 0;
  border-bottom: 1px solid rgba(29, 36, 51, 0.08);
  padding-bottom: 12px;
}

.tag-item {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-left: 0;
  margin-right: 10px;
  margin-bottom: 10px;
  box-sizing: border-box;
}

.tag {
  background-color: #686868;
  box-sizing: border-box;
  display: inline-block;
  height: 22px;
  padding: 0 10px;
  line-height: 22px;
  font-size: 10px;
  color: #fff;
  border-radius: 4px;
  white-space: nowrap;
  border: 1px solid #686868;
  transition: 0.2s;
  font-weight: 400;
}

.tag-item:hover,
.activeTag {
  box-sizing: border-box;
}

.tag {
  cursor: pointer;
}

.tag-cloud {
  gap: 8px 10px;
}

.tag-cloud .tag-item {
  margin-right: 0;
  margin-bottom: 0;
}

.tag-cloud-item {
  height: auto;
  padding: 0;
  line-height: 1.25;
  border-radius: 0;
  background: transparent;
  border-color: transparent;
  color: var(--tag-color);
  font-size: var(--tag-size);
  letter-spacing: 0.01em;
  font-weight: 500;
}

.tag-cloud .activeTag .tag-cloud-item,
.tag-cloud .tag-item:hover .tag-cloud-item {
  color: #111;
  transform: translateY(-1px);
}

.blog-type-li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  line-height: 40px;
}

.recommend-blog {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  padding-left: 0;
  padding-right: 0;
  margin-bottom: 0;
  border-radius: 5px;
}

.recommend-a {
  border-bottom: 1px solid rgba(34, 36, 38, 0.1);
  line-height: 44px;
  display: block;
  text-decoration: none;
  color: #1d2433;
  font-size: 15px;
  font-weight: 400;
}

.side-list {
  margin-top: 10px;
}

.side-tags {
  padding-bottom: 10px;
}

.attributes {
  display: flex;
  align-items: center;
}

.unified-side-card .more {
  padding: 10px 0 2px;
  color: rgba(29, 36, 51, 0.42);
}

.unified-side-card .more:hover {
  color: rgba(29, 36, 51, 0.72);
}

.recommend-a:hover {
  color: #686868;
}

.total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.titleIndex {
  display: flex;
  align-items: center;
}

.el-icon-back {
  font-weight: bolder;
  color: #686868;
  margin-right: 10px;
}

.el-icon-back:hover {
  cursor: pointer;
}

.blog-content:hover {
  border-left: 5px solid rgba(17, 17, 17, 0.32);
  border-right: 5px solid rgba(17, 17, 17, 0.12);
  background-color: rgba(17, 17, 17, 0.06);
  cursor: pointer;
}

.blog-content {
  padding: 18px 14px;
  height: auto;
  border-bottom: 1px solid rgba(29, 36, 51, 0.08);
  transition: 0.3s;
}

.el-image {
  border-radius: 5px;
  box-sizing: border-box;
  flex-shrink: 0;
}

.blog-info {
  display: flex;
  align-items: center;
  color: rgba(29, 36, 51, 0.58);
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.user-info {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-right: 15px;
  float: left;
}

.header {
  text-decoration: none;
  color: rgba(29, 36, 51, 0.72);
  font-weight: 500;
}

.blog-date {
  float: right;
  margin-right: 15px;
}

.blog-type {
  float: right;
  margin-left: auto;
}

.blog-tag {
  float: right;
  margin-left: auto;
}

@media screen and (max-width: 768px) {
  .blog-date {
    display: none;
  }

  .welcome {
    width: 100%;
  }

  .border {
    display: none;
  }

  .tit {
    font-size: 2rem;
    width: 100%;
    line-height: 50px;
    letter-spacing: 2px;
    height: auto;
  }

  .intro {
    font-size: 1rem;
    line-height: 30px;
  }

  .el-pagination {
    width: 100%;
  }

  .home-content {
    width: calc(100vw - 12px);
    margin-top: -72px;
  }

  .home-grid {
    grid-template-columns: 1fr;
  }

  .home-side {
    width: 100%;
  }
}
</style>
