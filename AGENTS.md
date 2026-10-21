# AGENTS.md

ZacBlog v2：`server/`（Spring Boot 3 后端）+ `web/`（React 前端）。旧的若依代码 `frontend/`、`backend/` 仍在仓库里，**不要修改**，等迁移完成后统一删除。

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
docs/api.md                 接口契约
.github/workflows/ci.yml    CI
```

## server

数据访问用 **Spring Data JDBC**（`spring-boot-starter-data-jdbc`）：简单的增删改查用 Repository，复杂查询用 `JdbcClient` / `NamedParameterJdbcTemplate` 手写 SQL。不引入 JPA/Hibernate，也不引入 MyBatis。

路由不带 `/api` 前缀，前缀约定见 `docs/api.md`。安全配置在 `vip.zaaac.blog.config.SecurityConfig`，默认拒绝一切；只有 `/actuator/health` 对外开放。

环境变量：

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | |
| `DB_URL` | `jdbc:mysql://localhost:3306/zaaac_blog?...` | |
| `DB_USERNAME` / `DB_PASSWORD` | 空 | |
| `JWT_SECRET` | 空 | 必须设置 |
| `UPLOAD_PATH` | `/home/ZacBlog/zaaac/uploadPath` | 上传目录，`/profile/**` 从这里读 |

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
```
