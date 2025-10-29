<template>
  <div class="no-caret">
    <el-row :gutter="20" style="margin: 100px 0px;">
      <el-col :sm="1" :lg="3" class="hidden-xs-only" style="opacity: 0;">Left PlaceHolder</el-col>
      <el-col :xs="24" :sm="16" :lg="13">
        <el-container style="opacity: 0.9" class="document-container">
          <el-card class="animate__animated animate__fadeInLeft content-card">
            <div class="markdown-content">
              <markdown-content />
            </div>
          </el-card>
        </el-container>
      </el-col>
      <el-col :xs="24" :sm="6" :lg="5" class="right-sidebar">
        <RightSidebar />
      </el-col>
      <el-col :sm="1" :lg="3" class="hidden-xs-only" style="opacity: 0;">Right PlaceHolder</el-col>
      <el-backtop :bottom="60">
        <div class="backtop-icon">
          <svg-icon icon-class="top" style="color: black;"/>
        </div>
      </el-backtop>
    </el-row>
  </div>
</template>

<script>
import markdownContent from './my/aboutMe.md';
import RightSidebar from "./rightSidebar/rightSidebar.vue";
export default {
  name: 'cmsDoucument',
  components: {
    markdownContent,
    RightSidebar,
  },
  mounted() {
    // Adjust iframe width and height (if applicable)
    function changeqtIframe() {
      const qt = document.getElementById('qt');
      const deviceWidth = document.body.clientWidth;
      const deviceHeight = document.body.clientHeight;
      if (qt) {
        qt.style.width = `${deviceWidth}px`;
        qt.style.height = `${deviceHeight}px`;
      }
    }
    changeqtIframe();
    window.onresize = function () {
      changeqtIframe();
    };
  },
};
</script>

<style scoped lang="scss">

.content-container {
  // Central container with shadow and transparency
  padding: 20px;
  background: rgba(255, 255, 255, 0.9); // Semi-transparent white background
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.1); // Drop shadow
  border-radius: 12px; // Rounded corners
  backdrop-filter: blur(10px); // Optional: Adds a glass effect
  transition: transform 0.2s ease-in-out;
}

.content-container:hover {
  transform: scale(1.02); // Slight zoom effect on hover
}

.markdown-content {
  // Style for the markdown content
  font-size: 16px;
  line-height: 1.6;
  color: #333;
}
</style>
