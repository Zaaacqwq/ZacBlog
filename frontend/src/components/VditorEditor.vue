<template>
  <div class="vditor-editor-component">
    <div ref="vditor" />
  </div>
</template>

<script>
import Vditor from 'vditor'
import 'vditor/dist/index.css'

export default {
  name: 'VditorEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    height: {
      type: [String, Number],
      default: 400
    }
  },
  data() {
    return {
      vditor: null,
      // prevent cursor jump by distinguishing internal edits from external value changes
      _internalEdit: false
    }
  },
  mounted() {
    this.initVditor()
  },
  beforeDestroy() {
    try {
      if (this.vditor && this.vditor.destroy) this.vditor.destroy()
    } catch (e) {}
  },
  methods: {
    initVditor() {
    const defaultToolbar = [
      'emoji',
      '|',
      'headings',
      'bold',
      'italic',
      'strike',
      '|',
      'link',
      'list',
      'ordered-list',
      'check',
      '|',
      'quote',
      'line',
      'code',
      'inline-code',
      '|',
      'undo',
      'redo',
      '|',
      'edit-mode',
      '|',
      'upload',
      'table',
      '|',
      'fullscreen'
    ]

      const options = {
        value: this.value || '',
        height: Number(this.height) || 400,
        // IR mode is the closest Vditor offers to Typora-style Markdown editing.
        mode: 'ir',
        lang: 'en_US',
        cache: { enable: false },
        toolbar: defaultToolbar,
        placeholder: 'Write in Markdown...',
        toolbarConfig: {
          pin: true
        },
        preview: {
          actions: []
        },
        input: (value) => {
          // mark this change as internal to avoid resetting content in the watcher
          this._internalEdit = true
          // emit markdown value for v-model
          this.$emit('input', value)
          // clear the flag on next microtask
          this.$nextTick(() => { this._internalEdit = false })
        }
      }

      try {
        this.vditor = new Vditor(this.$refs.vditor, options)
      } catch (err) {
        // if initialization fails, log error and provide graceful fallback
        // do not throw — parent component can still use markdown text
        // eslint-disable-next-line no-console
        console.error('Vditor init error:', err)
        this.vditor = null
      }
    },
    // return the current markdown content
    getMarkdown() {
      try {
        return this.vditor.getValue()
      } catch (e) {
        return this.value || ''
      }
    },
    // try to return HTML; fall back to markdown if HTML API unavailable
    getHtml() {
      try {
        // Vditor provides preview/render helpers; instance may have getHTML
        if (this.vditor.getHTML) {
          return this.vditor.getHTML()
        }
        return this.getMarkdown()
      } catch (e) {
        return this.getMarkdown()
      }
    },
    // allow setting editor's value from parent
    setValue(val) {
      try {
        if (this.vditor && typeof this.vditor.setValue === 'function') {
          this.vditor.setValue(val)
        }
      } catch (e) {}
    }
  },
  watch: {
    value(newVal) {
      // Only update when value actually changed from the outside
      if (!this.vditor) return
      try {
        if (this._internalEdit) return
        const current = this.vditor.getValue ? this.vditor.getValue() : ''
        const nextVal = newVal || ''
        if (current === nextVal) return
        this.vditor.setValue(nextVal)
      } catch (e) {}
    }
  }
}
</script>

<style scoped>
.vditor-editor-component {
  width: 100%;
}
</style>

<style lang="scss">
.vditor-editor-component {
  .vditor {
    border: 1px solid rgba(23, 32, 51, 0.08);
    border-radius: 24px;
    overflow: visible;
    background: rgba(255, 253, 249, 0.96);
    box-shadow: 0 24px 60px rgba(18, 27, 43, 0.08);
  }

  .vditor-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    padding: 10px 12px;
    background: linear-gradient(180deg, rgba(247, 244, 237, 0.98) 0%, rgba(239, 235, 228, 0.96) 100%);
    border-bottom: 1px solid rgba(23, 32, 51, 0.08);
  }

  .vditor-ir,
  .vditor-sv,
  .vditor-wysiwyg {
    overflow: hidden;
    border-radius: 0 0 24px 24px;
  }

  .vditor-toolbar__item {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    margin: 0 3px;
    border-radius: 10px;
    line-height: 1;
    color: #667085;
    transition: background .18s ease, color .18s ease, transform .18s ease;
  }

  .vditor-toolbar__item:hover,
  .vditor-toolbar__item--current {
    background: rgba(174, 123, 50, 0.12);
    color: #8f6327;
    transform: translateY(-1px);
  }

  .vditor-toolbar__divider {
    align-self: center;
    width: 1px;
    height: 20px;
    background: rgba(23, 32, 51, 0.08);
    margin: 0 6px;
  }

  .vditor-ir,
  .vditor-sv,
  .vditor-wysiwyg {
    background: transparent;
  }

  .vditor-reset {
    padding: 28px 32px 36px;
    color: #1d2433;
    font-size: 15px;
    line-height: 1.8;
  }

  .vditor-ir pre.vditor-reset,
  .vditor-sv__editor,
  .vditor-wysiwyg {
    padding: 28px 32px 36px !important;
  }

  .vditor-ir__node,
  .vditor-wysiwyg__block {
    padding-left: 0;
    padding-right: 0;
  }

  .vditor-reset h1,
  .vditor-reset h2,
  .vditor-reset h3,
  .vditor-reset h4 {
    color: #162033;
    letter-spacing: -0.02em;
  }

  .vditor-reset blockquote {
    margin: 1.2em 0;
    padding: 12px 16px;
    border-left: 4px solid rgba(174, 123, 50, 0.4);
    background: rgba(174, 123, 50, 0.08);
    border-radius: 0 14px 14px 0;
    color: rgba(29, 36, 51, 0.78);
  }

  .vditor-reset code:not(.hljs),
  .vditor-reset .vditor-ir__marker--code {
    background: rgba(23, 32, 51, 0.06);
    border-radius: 8px;
    padding: 2px 6px;
  }

  .vditor-reset pre {
    border-radius: 16px;
    background: #1d2433;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  }

  .vditor-reset table th,
  .vditor-reset table td {
    border-color: rgba(23, 32, 51, 0.08);
  }

  .vditor-counter {
    padding: 8px 16px 14px;
    color: rgba(29, 36, 51, 0.45);
    background: rgba(255, 253, 249, 0.96);
  }

  .vditor-resize {
    background: transparent;
  }
}
</style>
