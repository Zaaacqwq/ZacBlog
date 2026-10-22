# ZB-6 后台截图

1440×900，本地 server（H2 演示数据）+ `npm run dev`，用 macOS WebKit（WKWebView）截取；截图时关闭了 CSS 过渡动画（离屏渲染时过渡动画停在中途，页面会发灰）。

| 文件 | 内容 |
| --- | --- |
| `01-login.png` | 登录页 `/admin/login` |
| `02-dashboard.png` | 首页仪表盘 |
| `03-articles.png` | 文章列表，标签页栏有多个标签 |
| `04-article-edit.png` | 编辑文章：Markdown 编辑 + 预览 |
| `05-readers.png` | 读者管理：匿名读者、旧注册读者（老读者 / old@example.com）、已拉黑读者 |
| `06-readers-ip-blocks.png` | 读者管理 → IP 黑名单 Tab |
| `07-comments.png` | 评论管理 |
| `08-profile.png` | 个人中心（只有修改密码） |
| `09-sidebar-collapsed.png` | 侧栏折叠 |

## 与旧后台的对照

参照旧代码 `frontend/src/views/login.vue`、`frontend/src/layout/`（`Sidebar/Logo.vue`、`Navbar.vue`、`TagsView/index.vue`）和 `frontend/src/assets/styles/sidebar.scss`、`variables.scss`：

- 登录页：同一张背景图（压缩到 1920 宽）、400px 白色表单、标题 “Zac Admin System”、Username / Password / Captcha 占位、验证码图片在右侧（点击刷新）、Remember Me、整行 Login 按钮、底部版权行。
- 侧栏：左侧悬浮的深色圆角卡片（宽 286px、折叠 64px、圆角 28px），Logo 64px 反白 + 大写 “ZAC CONSOLE”，菜单项 44px 高、圆角 16px，选中项半透明白底。
- 顶栏：64px 高的米白半透明圆角条，左侧收起按钮 + 面包屑，右侧头像和用户名下拉（个人中心、退出登录）。
- 标签页栏：浅灰渐变圆角容器，标签为上圆角卡片，当前标签米白高亮，可关闭，右键有“关闭 / 关闭其他 / 关闭所有”。
- 页面背景：与旧版相同的米色到浅蓝渐变。

已知差异：表格、表单、按钮从 Element UI 换成了 Ant Design 组件，细节（字体粗细、控件圆角）与旧版略有不同；菜单按任务要求只保留 9 项。
