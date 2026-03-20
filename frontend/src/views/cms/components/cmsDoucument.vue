<!-- about page -->
<template>
  <div class="no-caret">
    <section class="document-shell">
      <div class="document-grid">
        <div class="document-main">
        <el-container style="opacity: 0.9" class="document-container">
          <el-card class="animate__animated animate__fadeInLeft content-card">
            <div class="markdown-content">
              <div v-html="html"></div>
            </div>
          </el-card>
        </el-container>
        </div>
      <div class="document-side right-sidebar">
        <RightSidebar />
      </div>
      <el-backtop :bottom="60">
        <div class="backtop-icon">
          <svg-icon icon-class="top" style="color: black;"/>
        </div>
      </el-backtop>
      </div>
    </section>
  </div>
</template>

<script>
// Render markdown file content with markdown-it to avoid HMR issues from vue-markdown-loader
import MarkdownIt from 'markdown-it';
// Force using file-loader to emit the md file and give us a URL (bypasses custom .md rule)
import mdUrl from '!!file-loader!./my/aboutMe.md';
import RightSidebar from "./rightSidebar/rightSidebar.vue";
export default {
  name: 'cmsDoucument',
  components: {
    RightSidebar,
  },
  data() {
    return {
      html: ''
    }
  },
  mounted() {
    // Load and render markdown at runtime
    const md = new MarkdownIt({
      html: true,
      linkify: true,
      // Do not automatically convert single newlines to <br/>,
      // so badges/images on separate lines stay inline within a paragraph.
      breaks: false
    });
    fetch(mdUrl)
      .then(r => r.text())
      .then(text => {
        this.html = md.render(text || '');
      })
      .catch(() => {
        this.html = '<p>Failed to load content.</p>';
      });
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
.document-shell {
  width: min(1360px, calc(100vw - 24px));
  margin: 0 auto;
  padding-top: 132px;
  padding-bottom: 40px;
}

.document-grid {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.document-main {
  min-width: 0;
}

.document-side {
  width: 320px;
}

.document-container {
  width: 100%;
  display: block;
}

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

@media screen and (max-width: 768px) {
  .document-shell {
    width: calc(100vw - 12px);
    padding-top: 112px;
    padding-bottom: 32px;
  }

  .document-grid {
    grid-template-columns: 1fr;
  }

  .document-side {
    width: 100%;
  }
}
</style>
