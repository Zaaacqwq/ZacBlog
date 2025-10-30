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
      // hide custom preview controls by default since user requested no preview actions
      previewMode: 'desktop',
      showControls: false
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
      'outdent',
      'indent',
      'insert-after',
      'insert-before',
      '|',
      'undo',
      'redo',
      '|',
      'upload',
      'table',
      '|',
      'edit-mode',
      'fullscreen',
      'outline',
      'export'
    ]

      const options = {
        value: this.value || '',
        height: Number(this.height) || 400,
        mode: 'sv',
        cache: { enable: false },
        // vditor expects an array for toolbar; passing boolean caused runtime error
        toolbar: defaultToolbar,
        // disable all preview actions (desktop/tablet/mobile/mp-wechat/zhihu)
        preview: {
          actions: []
        },
        input: (value) => {
          // emit markdown value for v-model
          this.$emit('input', value)
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
      // update editor when v-model changes from outside
      if (this.vditor) {
        try { this.vditor.setValue(newVal || '') } catch (e) {}
      }
    }
  }
}
</script>

<style scoped>
.vditor-editor-component {
  width: 100%;
}
</style>
