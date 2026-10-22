# AGENTS.md

ZacBlog v2：`server/`（Spring Boot 3 后端）+ `web/`（React 前端），部署脚本在 `deploy/`。旧的若依代码已在 ZB-8 切换上线后删除（在 git 历史里）。

## 硬性规定

- **任何文件都不能写真实的密码、密钥、token 或服务器地址凭据**（包括 `application*.yml`、测试、文档、CI）。配置一律从环境变量读取；测试里只能用明显是假的值（如 `test-only-not-a-real-secret-...`）。
- `.env` / `*.env` 已在 `.gitignore` 中，本地配置放这里，不要提交。
- `.github/workflows/**`、`deploy/**`、`.swarm/**` 是受保护路径，改动需要主人单独审阅。
- 接口以 [`docs/api.md`](docs/api.md) 为准；改接口先改文档。

## 目录布局

```
server/                     Spring Boot 3.5、Java 17、Maven 单模块，包名 vip.zaaac.blog
  src/main/resources/
    application.yml         只有环境变量占位符
    db/migration/           Flyway 脚本 V<n>__<desc>.sql（需同时兼容 MySQL 8 和 H2 MySQL 模式）
  src/test/resources/
    application-test.yml    test profile：H2 内存库
web/                        Vite + React 18 + TypeScript + React Router
  src/public/routes.tsx     前台路由（T4）
  src/admin/routes.tsx      后台路由（T5）
  src/shared/               共享代码：api client（api.ts）、token 存储（token.ts）
  src/routes.tsx            只负责拼装上面两组路由
deploy/                     部署：scripts/（服务器上以 zacblog 运行）、bootstrap-root.sh 和
                            root-after-cutover.sh（一次性 root 脚本，主人执行）、test/run.sh（本地桩测试）
docs/api.md                 接口契约
docs/deploy.md              服务器布局、部署/回滚/密码轮换流程（只写路径和流程，不写值）
.github/workflows/ci.yml    CI
.github/workflows/deploy.yml          部署：push 到 main 且 server/**、web/** 有改动时触发，也可手动
.github/workflows/server-secrets.yml  生成服务器环境变量文件 / 轮换数据库密码（手动）
.github/workflows/db-counts.yml       只读统计内容数量（手动）
```

## server

数据访问用 **Spring Data JDBC**（`spring-boot-starter-data-jdbc`）：简单的增删改查用 Repository，复杂查询用 `JdbcClient` / `NamedParameterJdbcTemplate` 手写 SQL。不引入 JPA/Hibernate，也不引入 MyBatis。

路由不带 `/api` 前缀，前缀约定见 `docs/api.md`。安全配置在 `vip.zaaac.blog.config.SecurityConfig`：`/public/**`、`/auth/**`、`/profile/**`、`/actuator/health` 匿名，`/admin/**` 需要 JWT（`hasRole("ADMIN")`），其余拒绝。

Flyway 版本号：T2 用 V1–V9，T3（后台）从 V10 起；V2+ 只能新增，规则见 `server/src/main/resources/db/migration/README.md`。

环境变量：

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | |
| `DB_URL` | `jdbc:mysql://localhost:3306/zaaac_blog?...` | |
| `DB_USERNAME` / `DB_PASSWORD` | 空 | |
| `JWT_SECRET` | 空 | 必须设置 |
| `UPLOAD_PATH` | `/home/ZacBlog/zaaac/uploadPath` | 上传目录，`/profile/**` 从这里读 |
| `JWT_TTL` | `7d` | token 有效期 |
| `ADMIN_USER_IDS` | `1` | 能登录后台的 `sys_user.user_id`，逗号分隔 |
| `BLOG_TIME_ZONE` | `Asia/Shanghai` | 旧库 datetime 列的时区 |
| `RATE_LIMIT_LOGIN_PER_MINUTE` | `5` | 登录限流（按 IP、按用户名各算） |
| `RATE_LIMIT_POST_INTERVAL` / `RATE_LIMIT_POSTS_PER_DAY` | `30s` / `20` | 匿名评论+留言按 IP 限流 |

```bash
cd server
mvn -B verify                 # 单元测试 + H2 集成测试；不需要 Docker、MySQL
mvn -B verify -Pmysql-it      # 只跑 @Tag("mysql") 的 Testcontainers 测试，需要 Docker（CI 里跑）
DB_URL=... DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=... mvn spring-boot:run
curl localhost:8080/actuator/health
```

测试约定：普通测试用 `@ActiveProfiles("test")`（H2）。需要真实 MySQL 的测试类加 `@Tag("mysql")`，默认的 `mvn verify` 会排除它们。测试类名以 `Test` / `Tests` 结尾（surefire 只扫描这些）。

没有 MySQL 时本地启动：H2 是 test 依赖，可以用
`DB_URL='jdbc:h2:mem:zacblog;MODE=MySQL;DATABASE_TO_LOWER=TRUE' DB_USERNAME=sa mvn spring-boot:run -Dspring-boot.run.useTestClasspath=true`。

## web

```bash
cd web
npm ci
npm run dev                   # http://localhost:5173，/api/** 去掉前缀代理到 http://localhost:8080
npm run lint
npm test -- --run             # Vitest + Testing Library
npm run build                 # tsc + vite build，产物在 web/dist
```

所有请求都通过 `src/shared/api.ts` 发到 `/api/...`，不要在代码里写死后端域名。dev 代理目标可用环境变量 `VITE_API_TARGET` 覆盖。

## 验证

提交前跑和 CI 一致的命令（`.swarm/project.yaml` 的 verify 也是这些）：

```bash
cd server && mvn -B verify
cd web && npm ci && npm run lint && npm test -- --run && npm run build
bash deploy/test/run.sh       # 部署脚本；不需要 Docker、root、MySQL，装了 shellcheck 会顺带检查
```

macOS 自带的 bash 3.2 会把紧跟在 `$VAR` 后面的全角字符（`，`、`（`）当成变量名的一部分，脚本里的中文日志一律写 `${VAR}`。
