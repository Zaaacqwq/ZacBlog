import '@testing-library/jest-dom/vitest'
import { configure } from '@testing-library/react'
import type { JSDOM } from 'jsdom'

// Node 25+ 自带实验性的 Web Storage 全局变量，未配置存储文件时 localStorage 为 undefined，
// 并且会遮住 jsdom 提供的实现；这里换回 jsdom 的 localStorage/sessionStorage。
const dom = (globalThis as unknown as { jsdom?: JSDOM }).jsdom
if (dom) {
  for (const key of ['localStorage', 'sessionStorage'] as const) {
    if (!globalThis[key]) {
      Object.defineProperty(globalThis, key, { value: dom.window[key], configurable: true })
    }
  }
}

// antd 依赖 matchMedia，jsdom 没有实现
if (typeof window !== 'undefined' && !window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  })
}

// jsdom 没有实现滚动，调用会打印 “Not implemented”
window.scrollTo = () => {}
Element.prototype.scrollIntoView = () => {}

// 后台页面是懒加载的（antd 体积大），整套测试并行跑时首次 import 可能超过默认的 1 秒；
// 前台的旧 URL 跳转测试（/login、/index）也会落到后台路由上
configure({ asyncUtilTimeout: 5000 })
