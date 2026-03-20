<!-- blog page -->
<template>
  <div class="blog-detail-shell no-caret">
    <div class="blog-detail-grid">
      <div class="blog-detail-main">
        <el-card class="first-card">
          <div class="story-hero">
            <div class="story-kicker">Essay</div>
            <h1 class="blog-title">{{ blog.title }}</h1>
            <div class="hero-meta">
              <span><i class="el-icon-user"></i>{{ blog.createBy }}</span>
              <span><i class="el-icon-date"></i>{{ blog.createTime }}</span>
              <span><i class="el-icon-view"></i>{{ blog.views }} views</span>
            </div>
            <div class="hero-types">
              <el-tag v-for="tag in blog.types" :key="tag.typeId" size="mini" effect="plain" type="info">
                {{ tag.typeName }}
              </el-tag>
            </div>
          </div>

          <div v-if="blog.contentType === '1'" class="story-body typo m-padded-lr-responsive m-padded-tb-large ql-editor" v-html="blog.content"></div>
          <div v-if="blog.contentType === '3'" class="story-body" v-html="blog.content"></div>
          <CherryMarkdown ref="CherryMarkdown" v-if="blog.contentType === '2'" v-model="blog.contentMarkdown" :defaultModel="'previewOnly'"></CherryMarkdown>
          <div v-if="blog.contentType === '4'" ref="vditorPreview" class="story-body mvditor-reset md-preview math-wrap typo m-padded-lr-responsive m-padded-tb-large"></div>

          <div class="tags" v-if="blog.tags && blog.tags.length">
            <span class="tag-item" v-for="tag in blog.tags" :key="tag.tagId">
              {{ tag.tagName }}
            </span>
          </div>

          <section v-if="blog.blogFilesNew && blog.blogFilesNew.length" class="attachment-section">
            <div class="section-label">Attachments</div>
            <el-table :data="blog.blogFilesNew" :border="false" class="attachment-table">
              <el-table-column align="center" min-width="30%" prop="remark" label="Attachment">
                <template slot-scope="scope">
                  <el-row>
                    <el-col :span="6"><div class="blogFilesInfoName">Name</div></el-col>
                    <el-col :span="18"><el-input v-model="scope.row.fileOriginName" disabled /></el-col>
                  </el-row>
                  <el-row style="margin-top: 4px;">
                    <el-col :span="6"><div class="blogFilesInfoName">Size</div></el-col>
                    <el-col :span="18"><el-input v-model="scope.row.fileSize" disabled /></el-col>
                  </el-row>
                  <el-row style="margin-top: 4px;">
                    <el-col :span="6"><div class="blogFilesInfoName">Type</div></el-col>
                    <el-col :span="18"><el-input v-model="scope.row.fileSuffix" disabled /></el-col>
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
          </section>

          <div class="author">
            <div class="section-label">Article Notes</div>
            <ul>
              <li>Written by {{ blog.createBy }}</li>
              <li>Published on {{ blog.createTime }}</li>
            </ul>
          </div>

          <el-card shadow="never" class="comments">
            <div class="section-label">Comments</div>
            <comment></comment>
          </el-card>
        </el-card>
      </div>
      <div class="blog-detail-side right-sidebar">
        <RightSidebar />
      </div>
      <el-backtop :bottom="60">
        <div class="backtop-icon">
          <svg-icon icon-class="top" style="color: black;" />
        </div>
      </el-backtop>
    </div>
  </div>
</template>

<script>
// import 'cherry-markdown/dist/cherry-markdown.min.css'
//使用prism.js代码高亮
import '@/views/cms/plugins/prism.js'
import '@/views/cms/plugins/prism.css'
import comment from "./comment/Ipcomment"
import Vditor from 'vditor'
import 'vditor/dist/index.css'
// markdown-it as a fallback renderer when Vditor.preview fails to load external scripts
// --- 高亮：用 core + 手动注册常见语言（稳定 & 可控）
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import bash from 'highlight.js/lib/languages/bash'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import java from 'highlight.js/lib/languages/java'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('c', c)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('java', java)
hljs.registerLanguage('json', json)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)

import 'highlight.js/styles/github.css'

// --- KaTeX 保持
import 'katex/dist/katex.min.css'
import renderMathInElement from 'katex/dist/contrib/auto-render.js'

// --- Markdown 渲染
import MarkdownIt from 'markdown-it'
import markdownItFootnote from 'markdown-it-footnote'

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
    // 仅当文章 id 变更时，才重新拉取数据；避免 hash(#fn1) 变化触发整页刷新，导致锚点跳转失败
    '$route'(to, from) {
      if (to.query && from && to.query.id !== from.query?.id) {
        this.getBlogInfomation()
      }
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
    async getBlogInfomation() {
      addBlogViews(this.$route.query.id)
      getBlogDetail(this.$route.query.id).then((response) => {
        this.blog = response.data
        this.blog.blogFilesNew = []
        if (response.data.blogFiles) {
          this.blog.blogFilesNew = JSON.parse(response.data.blogFiles)
        }

        // 你说今后只发 Markdown：统一走 Markdown 渲染
        if (this.blog.contentType === '4' || this.blog.contentType === '2') {
          this.$nextTick(() => {
            const mdSrc = (this.blog.contentMarkdown || '').trim()
            const htmlSrc = (this.blog.content || '').trim()

            if (mdSrc) {
              // 1) 明确有 Markdown，渲染它
              this.renderMarkdownTo(this.$refs.vditorPreview, mdSrc)
            } else if (this.looksLikeHtml(htmlSrc)) {
              // 2) 没有 Markdown，但 content 看起来真的是 HTML，直接塞
              this.$refs.vditorPreview.innerHTML = htmlSrc
              this.decorateMathAndCode()
            } else {
              // 3) 兜底：把 content 当 Markdown 渲（历史数据也能显示）
              this.renderMarkdownTo(this.$refs.vditorPreview, htmlSrc)
            }
          })
        }
      })
    },

    // 比之前更严格：只在“明显是 HTML 块”的情况下才返回 true
    looksLikeHtml(str) {
      if (!str) return false
      const s = str.trim().slice(0, 512).toLowerCase()
      // 常见块级标签开头（避免被 <img ...> / <br> 这类行内标签误判）
      const blockStarters = [
        '<article','<section','<div','<p','<h1','<h2','<h3','<h4','<h5','<h6',
        '<ul','<ol','<pre','<table','<blockquote','<figure','<code'
      ]
      if (blockStarters.some(t => s.startsWith(t))) return true
      // 简单成对标签检查
      if (s.includes('</p>') || s.includes('</div>') || s.includes('</table>') || s.includes('</pre>')) return true
      return false
    },

    renderMarkdownTo(el, mdText) {
      try {
        // 预处理：把显示数学块（$$...$$、\[...\]）替换为占位符，避免 breaks:true 在其中插入 <br>
        // 这样渲染后的 HTML 我们再把占位符替换回原始数学文本，让 KaTeX auto-render 正确识别。
        const holders = []
        const save = (raw) => {
          const i = holders.length
          holders.push(raw)
          return `::MATH${i}::`
        }
        let src = mdText || ''
        // 匹配形如：行首 $$ \n ... \n $$（允许前面有换行）
        src = src.replace(/(^|\n)\$\$([\s\S]*?)\$\$(?=\s*($|\n))/g, (m, p1, body) => {
          return `${p1}${save('$$' + body + '$$')}`
        })
        // 匹配形如：行首 \[ ... \]
        src = src.replace(/(^|\n)\\\[([\s\S]*?)\\\](?=\s*($|\n))/g, (m, p1, body) => {
          return `${p1}${save('\\[' + body + '\\]')}`
        })

        const md = new MarkdownIt({
          html: true,
          linkify: true,
          typographer: true,
          breaks: true, // 单个换行符直接换行，符合“正常 line break”
          // 把“奇怪的语言名”兜底到 plaintext，避免控制台警告
          highlight: (code, lang) => {
            const raw = (lang || '').trim().toLowerCase()
            // 只接受安全的语言名（字母数字和少量符号）
            const safe = /^[a-z0-9+#.\-]+$/.test(raw) ? raw : ''
            // 常见映射
            const alias = {
              js: 'javascript', jsx: 'javascript',
              ts: 'typescript', tsx: 'typescript',
              py: 'python',
              shell: 'bash', zsh: 'bash',
              'c++': 'cpp'
            }
            const mapped = alias[safe] || safe

            try {
              if (mapped && hljs.getLanguage(mapped)) {
                // 标记 language-xxx 并带上 data-processed，避免后续再次高亮
                return `<pre class=\"hljs-container\"><code class=\"hljs language-${mapped}\" data-processed=\"1\">` +
                  hljs.highlight(code, { language: mapped, ignoreIllegals: true }).value +
                  `</code></pre>`
              }
            } catch (_) { /* 忽略 */ }

            // 兜底：纯文本
            return `<pre class=\"hljs-container\"><code class=\"hljs\">` +
              md.utils.escapeHtml(code) +
              `</code></pre>`
          },
  })
  // 启用脚注支持
  md.use(markdownItFootnote)

        let html = md.render(src)
        // 渲染后把占位符替换回原始数学文本（不被 <br> 打断）
        html = html.replace(/::MATH(\d+)::/g, (m, idx) => holders[Number(idx)] || m)

        el.innerHTML = html
        this.decorateMathAndCode()
      } catch (err) {
        console.error('Markdown 渲染失败：', err)
        el.textContent = mdText || ''
      }
    },

    decorateMathAndCode() {
      // KaTeX 自动渲染（把严格模式静音，去掉告警）
      try {
        renderMathInElement(this.$refs.vditorPreview, {
          delimiters: [
            { left: '$$', right: '$$', display: true },
            { left: '\\[', right: '\\]', display: true },
            { left: '$', right: '$', display: false },
            { left: '\\(', right: '\\)', display: false },
          ],
          throwOnError: false,
          strict: 'ignore',
        })
      } catch (e) {
        console.warn('KaTeX auto-render 警告：', e)
      }

      // 老版本 HLJS 兼容：优先用 highlightElement，否则用 highlightBlock
      try {
        const blocks = this.$refs.vditorPreview.querySelectorAll('pre code')
        blocks.forEach((b) => {
          // 跳过已经处理过的（我们在 markdown-it 阶段生成了高亮 HTML）
          if (b.dataset && (b.dataset.processed === '1' || b.dataset.highlighted === 'yes')) return
          if (b.classList && b.classList.contains('hljs')) return
          // 跳过 mermaid 等非代码语言
          if (b.className && /language-?mermaid/.test(b.className)) return
          if (typeof hljs.highlightElement === 'function') {
            hljs.highlightElement(b)
          } else if (typeof hljs.highlightBlock === 'function') {
            hljs.highlightBlock(b)
          }
        })
      } catch (e) {
        console.warn('highlight.js 警告：', e)
      }

      // 绑定本页锚点平滑滚动（footnote 跳转 #fn1/#fnref1）
      try { this.wireAnchorScroll() } catch (_) {}
    },

    // 在预览容器内代理 a[href^="#"] 的点击，阻止路由刷新，仅做平滑滚动
    wireAnchorScroll() {
      const root = this.$refs.vditorPreview
      if (!root) return
      if (this._anchorHandler) {
        root.removeEventListener('click', this._anchorHandler)
      }
      this._anchorHandler = (e) => {
        const a = e.target.closest ? e.target.closest('a') : null
        if (!a) return
        const href = a.getAttribute && a.getAttribute('href')
        if (!href || href.charAt(0) !== '#') return
        e.preventDefault()
        const id = decodeURIComponent(href.slice(1))
        const target = document.getElementById(id)
        if (target) {
          // Prefer native center if supported
          if (typeof target.scrollIntoView === 'function') {
            try {
              target.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
            } catch (_) {
              // Fallback: manual centering
              const rect = target.getBoundingClientRect()
              const y = window.pageYOffset + rect.top - (window.innerHeight - rect.height) / 2
              window.scrollTo({ top: Math.max(0, y), behavior: 'smooth' })
            }
          } else {
            // Legacy fallback
            const rect = target.getBoundingClientRect()
            const y = window.pageYOffset + rect.top - (window.innerHeight - rect.height) / 2
            window.scrollTo({ top: Math.max(0, y), behavior: 'smooth' })
          }
        }
        // 仅更新 hash，不刷新页面
        const base = location.pathname + location.search
        history.replaceState(null, '', base + '#' + id)
      }
      root.addEventListener('click', this._anchorHandler)
    },

    // 初次渲染后如果 URL 已带 hash，居中滚动到对应位置
    scrollToHashIfPresent() {
      if (!location.hash) return
      const id = decodeURIComponent(location.hash.slice(1))
      const target = document.getElementById(id)
      if (!target) return
      try {
        target.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
      } catch (_) {
        const rect = target.getBoundingClientRect()
        const y = window.pageYOffset + rect.top - (window.innerHeight - rect.height) / 2
        window.scrollTo({ top: Math.max(0, y), behavior: 'smooth' })
      }
    },


    // 其余方法保持不变（handleDownload 等）

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
<style>
  /* Put in a global CSS (recommended), or use ::v-deep below inside this SFC */
  .md-preview table { border-collapse: collapse; }
  .md-preview th,
  .md-preview td { border: 1px solid #e5e7eb; padding: 6px 10px; }

  /* optional nicer table header */
  .md-preview thead th { background: #f7f7f7; }

  .md-preview .katex { white-space: normal; }              /* inline / display 都生效 */
  .md-preview .katex-display { white-space: normal; }      /* display 块 */
  .md-preview .katex-display > .katex { white-space: normal; }
  .md-preview .katex { word-break: break-word; }
  
  /* Footnotes styling */
  .md-preview sup.footnote-ref {
    font-size: 0.75em;
    line-height: 1;
    vertical-align: super;
    margin-left: 2px;
  }
  .md-preview .footnotes {
    margin-top: 1.25rem;
    border-top: 1px solid #e5e7eb;
    color: #555;
    font-size: 0.95em;
  }
  .md-preview .footnotes-list { padding-left: 1.25rem; }
  .md-preview .footnote-item { margin: 0.4rem 0; }
  .md-preview .footnote-backref {
    text-decoration: none;
    margin-left: 0.4rem;
    color: #999;
  }
  /* Nested blockquotes styling: stacked vertical bars with spacing */
  .md-preview blockquote {
    margin: 1.2em 0;
    padding: 0.6em 1em;
    border-left: 6px solid #e6ecef;
    color: #606060;
    background: transparent;
  }
  .md-preview blockquote blockquote {
    margin: 0.8em 0 0.8em 0.8em;
    padding-left: 0.8em;
    border-left: 6px solid #d7e0e3;
  }
  .md-preview blockquote blockquote blockquote {
    margin: 0.6em 0 0.6em 1.4em;
    padding-left: 0.8em;
    border-left: 6px solid #c6d2d6;
  }

  /* Code block frame + readable monospace font */
  .md-preview pre.hljs-container {
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    padding: 12px 14px;
    overflow: auto;
    -webkit-overflow-scrolling: touch;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
    line-height: 1.6;
  }
  .md-preview pre.hljs-container code {
    background: transparent;
    padding: 0;
    border: 0;
  }
  /* Try to wrap very long formulas gracefully */
  .md-preview.math-wrap .katex { 
    white-space: normal !important; 
    overflow-wrap: anywhere; 
    word-break: break-word; 
  }
  .md-preview.math-wrap .katex-display > .katex { 
    white-space: normal !important; 
  }
  /* Allow wrapping inside KaTeX HTML tree */
  .md-preview.math-wrap .katex .katex-html { 
    white-space: normal !important; 
  }
  .md-preview.math-wrap .katex .base { 
    display: inline !important; /* default is inline-block; inline enables wrapping */
  }
  /* Optional aggressive wrap: let tokens break line if needed. This may slightly change spacing,
     but prevents overflow on narrow screens. */
  .md-preview.math-wrap .katex .mord,
  .md-preview.math-wrap .katex .mop,
  .md-preview.math-wrap .katex .mbin,
  .md-preview.math-wrap .katex .mrel,
  .md-preview.math-wrap .katex .mopen,
  .md-preview.math-wrap .katex .mclose,
  .md-preview.math-wrap .katex .mpunct {
    display: inline; /* KaTeX uses inline-block; inline allows line wrapping */
  }

  ::v-deep(.md-preview) table { border-collapse: collapse; }
  ::v-deep(.md-preview) th,
  ::v-deep(.md-preview) td { border: 1px solid #e5e7eb; padding: 6px 10px; }
  ::v-deep(.md-preview) pre > code { display:block; border:1px solid #e5e7eb; border-radius:6px; }
  /* scoped equivalents */
  ::v-deep(.md-preview) blockquote {
    margin: 1.2em 0;
    padding: 0.6em 1em;
    border-left: 6px solid #e6ecef;
    color: #606060;
    background: transparent;
  }
  ::v-deep(.md-preview) blockquote blockquote {
    margin: 0.8em 0 0.8em 0.8em;
    padding-left: 0.8em;
    border-left: 6px solid #d7e0e3;
  }
  ::v-deep(.md-preview) blockquote blockquote blockquote {
    margin: 0.6em 0 0.6em 1.4em;
    padding-left: 0.8em;
    border-left: 6px solid #c6d2d6;
  }
  ::v-deep(.md-preview) pre.hljs-container {
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    padding: 12px 14px;
    overflow: auto;
    -webkit-overflow-scrolling: touch;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
    line-height: 1.6;
  }
  ::v-deep(.md-preview) pre.hljs-container code {
    background: transparent;
    padding: 0;
    border: 0;
  }
</style>
<style scoped>
.blog-detail-shell {
  width: 100%;
  padding-top: 132px;
}

.blog-detail-grid {
  width: min(1360px, calc(100vw - 24px));
  margin: 0 auto 48px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.blog-detail-main {
  min-width: 0;
}

.blog-detail-side {
  width: 320px;
}

  .el-card {
    width: 100%;
  }

  .el-popper /deep/ {
    box-shadow: 0 2px 4px 0 rgb(34 36 38 / 12%),
  }

  .first-card {
    border-radius: 30px;
    position: relative;
    padding: 0 0 10px;
    font: 300 1em/1.8 PingFang SC, Lantinghei SC, Microsoft Yahei, Hiragino Sans GB, Microsoft Sans Serif, WenQuanYi Micro Hei, sans-serif;
    overflow: hidden;
    background: #ffffff;
  }

  hr.style-one {
    width: 100%;
    background-image: linear-gradient(to right, rgba(64, 158, 255, 0), rgba(64, 158, 255, 0.75), rgba(64, 158, 255, 0));
  }

  .appreciate {
    text-align: center;
  }

  .story-hero {
    padding: 34px 36px 18px;
    border-bottom: 1px solid rgba(29, 36, 51, 0.08);
    background: #ffffff;
  }

  .story-kicker,
  .section-label {
    margin-bottom: 12px;
    color: rgba(29, 36, 51, 0.5);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.18em;
    text-transform: uppercase;
  }

  .story-body {
    padding: 30px 36px 8px;
    color: #273041;
  }

  .hero-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 14px;
    color: rgba(29, 36, 51, 0.58);
    font-size: 12px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .hero-meta span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  .hero-types {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .hero-types /deep/ .el-tag {
    border-radius: 999px;
  }

  .tags {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    margin: 8px 36px 24px;
  }

  .tag-item {
    display: inline-flex;
    align-items: center;
    margin-bottom: 8px;
    color: rgba(29, 36, 51, 0.72);
    font-size: 13px;
    line-height: 1;
  }

  .tag-item::before {
    content: "#";
    margin-right: 4px;
    color: rgba(29, 36, 51, 0.46);
  }

  .author {
    text-align: left;
    margin: 24px 36px;
    padding: 24px;
    border-radius: 24px;
    background-color: rgba(23, 32, 51, 0.04);
    color: #273041;
    font-size: small;
    font-family: PingFang SC, Lantinghei SC, Microsoft Yahei, Hiragino Sans GB, Microsoft Sans Serif, WenQuanYi Micro Hei, sans-serif;
  }

  .comments {
    margin: 24px 36px 30px;
    box-shadow: none;
    border: 1px solid rgba(29, 36, 51, 0.08);
    border-radius: 24px;
    background: #ffffff;
    text-align: left;
  }
  .blog-title {
    margin: 0 0 18px;
    text-align: left;
    font-size: clamp(2.4rem, 4vw, 4.4rem);
    line-height: 1.05;
    font-family: "Iowan Old Style", "Palatino Linotype", Georgia, serif;
    color: #1d2433;
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
    color: #111;
    font-weight: bold;
  }

  .attachment-section {
    margin: 0 36px 20px;
    padding: 4px 0 0;
  }

  .attachment-table /deep/ .el-table__body-wrapper,
  .attachment-table /deep/ .el-table__header-wrapper {
    border-radius: 20px;
  }

  .attachment-table /deep/ .el-input__inner,
  .attachment-table /deep/ .el-textarea__inner {
    background: #ffffff;
  }

  @media screen and (max-width: 768px) {
    .blog-detail-shell {
      padding-top: 112px;
    }

    .blog-detail-grid {
      width: calc(100vw - 12px);
      grid-template-columns: 1fr;
    }

    .blog-detail-side {
      width: 100%;
    }

    .tags {
      margin: 8px 20px 24px;
    }

    .story-hero,
    .story-body,
    .attachment-section,
    .author,
    .comments {
      margin-left: 20px;
      margin-right: 20px;
      padding-left: 20px;
      padding-right: 20px;
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
