<template>
  <div class="app-container home">
  
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>Zac Admin Framework</h2>
        <p>
          I always wanted to create an admin management system. After exploring many excellent open-source projects, I couldn't find one that suited my needs. So, during my spare time, I started developing my own admin system. Thus, the Ruoyi Admin System was born. It can be used for various web applications such as website admin panels, member centers, CMS, CRM, OA, etc. Of course, you can deeply customize it to build a more powerful system. All front-end and back-end code is highly streamlined and easy to learn, minimizing error rates. It also supports mobile client access. The system will continue to be updated with practical features.        </p>
        <p>
          <b>Current Version:</b> <span>v{{ version }}</span>
        </p>
        <p>
          <el-tag type="danger">&yen;</el-tag>
        </p>
        <p>
           
          <el-button
            size="mini"
            icon="el-icon-s-home"
            plain
            @click="goTarget('https://zaaacqwq.github.io/')"
            >Visit Homepage</el-button
          >
        </p>
      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>Technology Stack</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>Backend Technologies</h4>
            <ul>
              <li>SpringBoot</li>
              <li>Spring Security</li>
              <li>JWT</li>
              <li>MyBatis</li>
              <li>Druid</li>
              <li>Fastjson</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>Frontend Technologies</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Sass</li>
              <li>Quill</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
 
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "3.8.0",
    };
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    },
  },
};
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}
</style>
