# ZacBlog v2 接口契约

本文是 `server/`（T2、T3 实现）和 `web/`（T4 前台、T5 后台对接）之间的约定。改接口先改这里。

## 1. 路由约定：server 只用根路径，`/api` 只存在于浏览器一侧

server 的所有路由都挂在根路径，**不带 `/api` 前缀**：

| 前缀 | 用途 | 鉴权 |
| --- | --- | --- |
| `/public/**` | 前台匿名读取；匿名发表评论、留言 | 匿名。写接口有限流（429）和黑名单（邮箱 / IP / 关键字，403） |
| `/auth/**` | 验证码、登录 | 匿名，登录有限流 |
| `/admin/**` | 后台管理 | 需要 JWT：`Authorization: Bearer <token>` |
| `/profile/**` | 上传文件的静态访问，兼容旧地址 `/profile/upload/...` | 匿名，只读 |
| `/actuator/health` | 健康检查 | 匿名。其他 actuator 端点一律不可访问 |

除上表外的路径一律拒绝（401/403/404）。

浏览器里的 web 统一请求 `/api/...`，由前面一层去掉 `/api` 前缀后转给 server：

| 环境 | 入口 | 转发 |
| --- | --- | --- |
| 线上前台/后台 | `https://blog.zaaac.vip/api/xxx` | nginx `location /api/ { proxy_pass http://127.0.0.1:8080/; }`，去掉 `/api` 后转给 `127.0.0.1:8080/xxx` |
| 线上 API 域名 | `https://api.zaaac.vip/xxx` | nginx 直接反代到 `127.0.0.1:8080/xxx`，不改路径 |
| 本地开发 | `http://localhost:5173/api/xxx` | `web/vite.config.ts` 的 dev 代理去掉 `/api`，转给 `http://localhost:8080/xxx` |

例：`GET /api/actuator/health` → server 的 `GET /actuator/health`。

**`/profile/**` 必须一直留在根路径。** 旧文章正文、封面里存的是 `https://api.zaaac.vip/profile/upload/...` 这类地址，只有 server 在根路径提供 `/profile/**` 这些地址才不会失效。server 不得给它加前缀或改名。

## 2. 统一响应格式

除 `/profile/**`（直接返回文件）和 `/actuator/health`（Spring Boot 原生格式）外，所有接口都返回 JSON：

```json
{ "code": 200, "msg": "操作成功", "data": { } }
```

- `code`：与 HTTP 状态码相同（见第 4 节）。成功一律是 200，web 端以 `code === 200` 判断成功。
- `msg`：给人看的中文说明，失败时可以直接展示。
- `data`：业务数据；没有数据时为 `null`。

时间字段用 ISO-8601 字符串（`2026-10-21T18:30:00+08:00`），id 用数字。

## 3. 分页

列表接口的查询参数：

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `pageNum` | 1 | 从 1 开始 |
| `pageSize` | 10 | 最大 100，超过按 100 处理 |

分页结果放在 `data` 里：

```json
{ "code": 200, "msg": "操作成功", "data": { "rows": [ ], "total": 42 } }
```

## 4. 错误码

| HTTP / `code` | 含义 | web 端处理 |
| --- | --- | --- |
| 400 | 参数校验失败、验证码错误；`msg` 说明哪个字段 | 提示 `msg` |
| 401 | 未登录、token 无效或过期、用户名或密码错误 | 后台清掉 token，跳到登录页 |
| 403 | 无权访问；前台发表评论/留言时**邮箱、IP 被拉黑或内容命中屏蔽关键字**（`msg` 是通用提示，不说明原因） | 提示 `msg`，不要重试 |
| 404 | 资源不存在（文章未发布也按 404 处理） | 展示 404 页 |
| 413 | 上传文件过大（单个文件上限 10MB） | 提示 `msg` |
| 429 | 请求太频繁（限流），响应头带 `Retry-After`（秒） | 提示稍后再试 |
| 500 | 服务器内部错误；`msg` 不暴露堆栈 | 通用错误提示 |

限流的默认规则（进程内计数，重启清零；阈值可通过环境变量调整）：

- 发表评论/留言（两者合计）：同一 IP 每 30 秒 1 条（`RATE_LIMIT_POST_INTERVAL`）、每 24 小时 20 条（`RATE_LIMIT_POSTS_PER_DAY`）。
- `/auth/login`：同一 IP、同一账号（按数据库解析出的 `user_id` 计，`admin` / `ADMIN` 等等价写法共享额度）各自每分钟最多 5 次（`RATE_LIMIT_LOGIN_PER_MINUTE`），不论成功失败都计数。

## 5. 数据模型（响应字段）

```ts
Article {
  id: number
  title: string
  summary: string            // 摘要
  coverUrl: string | null    // 封面，可能是 /profile/upload/... 或外链
  contentMarkdown: string    // 只有详情接口返回
  contentHtml: string        // 只有详情接口返回；旧文章可能只有 HTML
  categoryId: number | null
  categoryName: string | null
  tags: { id: number; name: string }[]
  top: boolean               // 置顶
  essay: boolean             // 随笔（旧的 cmsEssayList）
  status: 'published' | 'draft'
  views: number
  commentCount: number
  createdAt: string
  updatedAt: string
}
Category { id: number; name: string; articleCount: number }
Tag      { id: number; name: string; articleCount: number }
Comment {                    // 评论和留言共用
  id: number
  parentId: number | null    // 回复的那条；顶层为 null
  nickname: string
  website: string | null
  avatarUrl: string | null
  content: string
  likeCount: number
  fromAdmin: boolean         // 博主回复
  replyToNickname: string | null // 前台：被回复那条的昵称；顶层为 null
  createdAt: string
  children: Comment[]        // 只有前台树形列表返回：这一楼（顶层评论）下的全部回复，按时间正序，不再嵌套
  // 以下字段只在 /admin/** 返回
  email?: string | null
  ip?: string
  articleId?: number | null
  articleTitle?: string | null
}
Notice   { id: number; title: string; content: string; createdAt: string }   // content 是 HTML
FileInfo { id: number; name: string; url: string; size: number; createdAt: string }
```

图片和附件的 `url` 统一存成根路径形式 `/profile/upload/yyyy/MM/dd/<文件名>`。web 显示时在前面加 `/api`（`/api/profile/upload/...`）；以 `http` 开头的外链和旧的 `https://api.zaaac.vip/profile/...` 原样使用。

## 6. 接口清单

### 6.1 前台 `/public/**`（匿名）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/public/articles` | 已发布文章分页，置顶在前、再按创建时间倒序。可选参数：`categoryId`、`tagId`、`keyword`（标题包含）、`essay`（`true` 只看随笔，`false` 只看普通文章，不传则都返回）。列表项不含正文 |
| GET | `/public/articles/recommend` | 推荐文章（最多 `limit` 篇，默认 5），`data` 为 `Article[]` |
| GET | `/public/articles/{id}` | 文章详情（含正文、标签、附件 `files: FileInfo[]`）。未发布或不存在返回 404 |
| GET | `/public/articles/{id}/files` | 文章附件 `FileInfo[]`（与详情里的 `files` 相同）。未发布或不存在返回 404 |
| POST | `/public/articles/{id}/views` | 浏览量 +1（同一 IP 对同一篇 10 分钟内只计一次），`data` 为最新浏览量 |
| GET | `/public/archives` | 归档：`data` 为 `{ year: number; month: number; articles: { id, title, createdAt }[] }[]` |
| GET | `/public/categories` | 全部分类 `Category[]` |
| GET | `/public/tags` | 全部标签 `Tag[]` |
| GET | `/public/articles/{id}/comments` | 文章评论，分页的是顶层评论，每条带 `children` |
| POST | `/public/articles/{id}/comments` | 发表评论，见下方请求体。限流 429，拉黑 403 |
| GET | `/public/messages` | 留言板，分页规则同评论 |
| POST | `/public/messages` | 发表留言，请求体同评论。限流 429，拉黑 403 |
| POST / DELETE | `/public/comments/{id}/like` | 评论点赞 / 取消点赞（同一 IP 只计一次），`data` 为最新 `likeCount` |
| POST / DELETE | `/public/messages/{id}/like` | 留言点赞 / 取消点赞，规则同上（评论和留言是两张表，id 会重复，所以分开） |
| GET | `/public/notices` | 公告分页 `Notice`，最新在前（只含状态正常的） |
| GET | `/public/notices/latest` | 最新一条公告 `Notice`，没有时 `data` 为 `null` |
| GET | `/public/site` | 侧栏统计：`{ articleCount, categoryCount, tagCount, commentCount, messageCount, totalViews }` |
| GET | `/public/about` | About 页：`{ content: string }`，Markdown。沿用旧版的来源——旧前端打包的 `aboutMe.md`，现在放在 server 的 `classpath:about/about.md` |

评论/留言请求体：

```json
{ "parentId": null, "nickname": "访客", "email": "a@example.com", "website": null, "content": "写得好", "homepage": "" }
```

校验：`nickname` 1–20 字，`content` 1–255 字（旧表 `content` 列是 varchar(255)），`email` **必填**且必须合法（只存库，任何前台接口都不返回），`website` 可空但必须是 http(s) 地址，`parentId` 必须是同一篇文章（或留言板）下未删除的那条，否则 400。成功返回新建的 `Comment`。

`homepage` 是**蜜罐字段**：web 端要渲染成对人不可见的输入框并保持为空。非空时 server 不入库，但照常返回 200（`data` 为 `null`），不让机器人察觉。

处理顺序：蜜罐 → 文章存在且已发布（否则 404）→ 黑名单（403）→ 限流（429）→ 入库。

### 6.2 认证 `/auth/**`（匿名）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/auth/captcha` | `data`: `{ captchaId: string; image: string }`，`image` 是 `data:image/png;base64,...`，内容是算术题（如 `3 + 4 = ?`，答案是非负整数）。5 分钟有效、只能用一次（答错也作废，需重新获取） |
| POST | `/auth/login` | 请求体 `{ username, password, captchaId, captchaCode }`。成功 `data`: `{ token: string; expiresAt: string }`。验证码错 400，账号密码错 401，太频繁 429 |

能登录的是 `sys_user` 里 `user_id` 在 `ADMIN_USER_IDS`（默认 `1`，即若依的超级管理员）中、状态正常的账号，密码按旧版 BCrypt 哈希校验；旧注册读者不能登录。

token 是 HS256 JWT，密钥来自环境变量 `JWT_SECRET`（至少 32 字节，未设置时 server 启动失败），有效期默认 7 天（`JWT_TTL`）。登出由 web 端删除本地 token 完成，server 不维护会话。

### 6.3 后台 `/admin/**`（需要 JWT）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/me` | 当前用户 `{ username, nickname, avatarUrl }` |
| PUT | `/admin/me/password` | `{ oldPassword, newPassword }` |
| GET | `/admin/dashboard` | `{ articleCount, commentCount, messageCount, totalViews, viewsLast7Days: { date, views }[], articlesByCategory: { name, count }[], articlesByTag: { name, count }[] }` |
| GET | `/admin/articles` | 分页，含草稿。可选参数：`keyword`、`categoryId`、`tagId`、`status` |
| GET | `/admin/articles/{id}` | 详情（含正文、附件） |
| POST | `/admin/articles` | 新建，请求体 `{ title, summary, coverUrl, contentMarkdown, contentHtml, categoryId, tagIds: number[], top, essay, status, fileIds: number[] }`，返回 `Article` |
| PUT | `/admin/articles/{id}` | 修改，请求体同新建 |
| DELETE | `/admin/articles/{id}` | 删除 |
| GET / POST | `/admin/categories` | 列表（不分页） / 新建 `{ name }` |
| PUT / DELETE | `/admin/categories/{id}` | 改名 / 删除（仍有文章时 400） |
| GET / POST | `/admin/tags` | 列表（不分页） / 新建 `{ name }` |
| PUT / DELETE | `/admin/tags/{id}` | 改名 / 删除（同时解除与文章的关联） |
| GET | `/admin/comments` | 评论分页（平铺，不是树），可选参数 `articleId`、`keyword` |
| POST | `/admin/comments/{id}/reply` | 以博主身份回复 `{ content }` |
| DELETE | `/admin/comments/{id}` | 删除（连同子回复） |
| GET | `/admin/messages` | 留言分页，可选参数 `keyword` |
| POST | `/admin/messages/{id}/reply` | 以博主身份回复 `{ content }` |
| DELETE | `/admin/messages/{id}` | 删除（连同子回复） |
| GET / POST | `/admin/notices` | 公告分页 / 新建 `{ title, content }` |
| PUT / DELETE | `/admin/notices/{id}` | 修改 / 删除 |
| POST | `/admin/files` | 上传，`multipart/form-data`，字段名 `file`，返回 `FileInfo`（`url` 为 `/profile/upload/...`） |
| GET | `/admin/files` | 文件分页 |
| DELETE | `/admin/files/{id}` | 删除记录和磁盘文件 |
| GET | `/admin/blacklist` | IP 黑名单分页：`{ id, ip, reason, createdAt }` |
| POST | `/admin/blacklist` | 拉黑 `{ ip, reason }` |
| DELETE | `/admin/blacklist/{id}` | 解除拉黑 |

### 6.4 静态文件 `/profile/**`

`GET /profile/upload/yyyy/MM/dd/<文件名>` 直接返回 `UPLOAD_PATH/upload/yyyy/MM/dd/<文件名>`（`UPLOAD_PATH` 默认 `/home/ZacBlog/zaaac/uploadPath`）。不允许列目录，不允许 `..` 越界。

### 6.5 健康检查

`GET /actuator/health` → `{"status":"UP"}`，不返回组件详情。`/actuator/env`、`/actuator/beans` 等其他端点不暴露。
