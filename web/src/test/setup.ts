import '@testing-library/jest-dom/vitest'
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
