# 部署（ZacBlog v2）

本文以服务器的实际情况为准；仓库根目录的 `ZacBlog_Deploy_Doc.md` 已过时，不要按它操作。

## 设计依据（ZB-7 执行中主人确认，取代任务原文的部分要求）

ZB-7 原始描述假设"zacblog 用户存在、sudo 要密码、后端用 screen + restart_backend.sh 启动、不能用 root/systemd"。
主人核实服务器后在任务中补充了实际情况（见下节）并定下新设计，以下几条原要求因此被替换：

| 原要求 | 现在 |
| --- | --- |
| 不能用 root、systemd；脚本里不能有 sudo、systemctl | 后端由 systemd 管理；部署脚本只用 sudoers 白名单里的 6 条 `sudo -n systemctl …`（测试的 sudo 桩会拒绝其他命令） |
| 用 screen 启停，回滚时按 restart_backend.sh 启动旧 jar | 旧后端本来就是 `zacblog-backend.service`（root），没有 screen；回滚为 `systemctl start zacblog-backend` |
| 沿用现有 zacblog 用户 | 该用户已不存在，由 `deploy/bootstrap-root.sh` 重新创建（uid/gid 1001），由主人审过后以 root 手动执行一次 |
| `source` 环境变量文件 | 逐行解析（文件同时是 systemd `EnvironmentFile`，`DB_URL` 里的 `&` 不能被 bash source） |

其余要求不变：部署前备份、失败自动回滚（jar 和前端）、上传目录和环境变量文件不动、workflow 只有 `workflow_dispatch` 且不含明文密钥、不改 nginx、不安装软件。

## 服务器现状

| 项 | 情况 |
| --- | --- |
| 旧后端 | systemd 服务 `zacblog-backend.service`，root 运行，`/usr/bin/java -Xms512m -Xmx1024m -jar /home/ZacBlog/backend/zaaac-admin.jar --server.port=8080`，日志 `/home/ZacBlog/backend/app.log`，外置配置 `/home/ZacBlog/backend/config/application.yml` |
| 新后端 | systemd 服务 `zacblog-server.service`（由 `deploy/bootstrap-root.sh` 安装），用户 zacblog，工作目录 `/home/ZacBlog/server`，**不能**在 `backend/` 下启动（否则会加载旧后端的 `config/`） |
| 端口 | 新旧后端都用 8080，同一时间只能有一个在运行 |
| 前端 | `/home/ZacBlog/frontend`（nginx 根目录，`try_files` 回落 `index.html`） |
| 上传目录 | `/home/ZacBlog/zaaac/uploadPath/`（nginx 的 `/profile/` 直接 alias 到这里）。部署不碰它 |
| 环境变量文件 | `/home/ZacBlog/zacblog.env`，权限 600，systemd `EnvironmentFile` 格式（见下）。部署不改它 |
| nginx | `blog.zaaac.vip` 的 `/api/` → `127.0.0.1:8080/`（去前缀），`api.zaaac.vip` 的 `/` → `127.0.0.1:8080/`。部署不改 nginx |
| JDK | 服务器是 JDK 21（`/usr/bin/java`），按 17 编译 |
| MySQL / Redis | 本机 |

部署用户 `zacblog`（uid/gid 1001）只能免密执行：

```
sudo -n systemctl start|stop|restart|is-active zacblog-server
sudo -n systemctl start|stop zacblog-backend
```

## 目录布局

```
/home/ZacBlog/
  zacblog.env                 新后端的环境变量（600，不进仓库，由 workflow 生成）
  scripts/zacblog-*.sh        部署脚本（workflow 每次从 deploy/scripts/ 上传；旧的 restart_backend.sh 不动、不用）
  incoming/                   workflow 上传的产物（部署成功后删除）
  server/
    releases/<版本>.jar       每次部署一个，保留最近 5 个（当前版本和回滚目标永远保留）
    current.jar -> releases/… 新后端实际运行的版本（软链接）
    app.log                   新后端日志
    .rollback-target          最近一次部署记下的回滚目标
  frontend/                   当前前端
  frontend.prev/              上一次部署前的前端（回滚用）
  backups/zacblog-<时间戳>.sql.gz   每次部署前的数据库备份，保留最近 10 份
  backend/                    旧后端（部署只 start/stop 它的服务，不改任何文件）
  zaaac/uploadPath/           上传目录
```

## 一次性初始化（root，主人执行）

`deploy/bootstrap-root.sh` 创建 zacblog 用户、替换 authorized_keys、建目录、安装 `zacblog-server.service`（不 enable、不 start）、写 sudoers（`visudo -cf` 校验通过才安装）。可重复执行；不碰 `zacblog-backend`、nginx、`backend/`、上传目录、`zacblog.env`。

先看一眼会装什么（不需要 root）：

```bash
bash deploy/bootstrap-root.sh --print
```

执行（`vps` 是能以 root 登录的 ssh 别名）：

```bash
ssh vps "bash -s -- '$(cat ~/.ssh/zacblog_deploy.pub)'" < deploy/bootstrap-root.sh
```

注意：公钥会**替换** `/home/ZacBlog/.ssh/authorized_keys` 的全部内容（替换前会备份为同目录下的 `authorized_keys.bak.<时间戳>`）；对应私钥的 base64 放到 GitHub Environment `production` 的 `SSH_KEY_B64`，`SSH_USER` 填 `zacblog`。

## 环境变量文件格式

`/home/ZacBlog/zacblog.env` 同时给 systemd（`EnvironmentFile=`）和备份脚本用，每行一个 `KEY=VALUE`：

```
DB_URL=jdbc:mysql://127.0.0.1:3306/zaaac_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
UPLOAD_PATH=/home/ZacBlog/zaaac/uploadPath
```

- 值不需要引号；`&` 之类可以直接写。脚本是逐行解析的，**不会**用 bash `source` 执行它。
- 值里不要写反斜杠、引号和 `$`（systemd 和脚本对它们的处理不完全一样）。
- 备份脚本从 `DB_URL` 解析主机、端口、库名，密码通过 `MYSQL_PWD` 环境变量交给 mysqldump，不出现在命令行参数里。

## 部署流程

GitHub Actions `Deploy`（`.github/workflows/deploy.yml`，目前只能手动触发 `workflow_dispatch`）：

1. `build` job：`mvn -B package`（含测试）、`npm ci && npm run build`，打包成 `zacblog-server-<sha>.jar` 和 `web-dist.tgz`。
2. `deploy` job（`environment: production`，需要主人在 GitHub 上批准）：上传 `deploy/scripts/zacblog-*.sh` 到 `/home/ZacBlog/scripts/`、产物到 `/home/ZacBlog/incoming/`，然后执行：

```bash
bash /home/ZacBlog/scripts/zacblog-deploy.sh <jar> <web-dist.tgz>
```

`zacblog-deploy.sh` 依次执行：

| 步骤 | 脚本 | 失败时 |
| --- | --- | --- |
| 1. 备份数据库 | `zacblog-backup.sh` | 中止，前后端都不动 |
| 2. 发布前端：解压到临时目录 → `frontend` 改名为 `frontend.prev` → 临时目录改名为 `frontend` | `zacblog-frontend.sh` | 中止（中途失败会自己恢复原目录） |
| 3. 记下回滚目标 → jar 放进 `releases/` →（首次切换时）`stop zacblog-backend` → `current.jar` 指向新版本 → `restart zacblog-server` → 轮询 `http://127.0.0.1:8080/actuator/health` 最多 120 秒 | `zacblog-start.sh` | 调 `zacblog-rollback.sh`，workflow 标红并打印 `server/app.log` 末尾 |

回滚目标：

- `zacblog-server` 正在运行 → 回到 `current.jar` 原来指向的 release（`restart zacblog-server`）。
- 旧后端 `zacblog-backend` 正在运行（首次切换）→ `stop zacblog-server`、删掉 `current.jar`、`start zacblog-backend`，检查端口有响应（旧后端没有 actuator）。

回滚同时把 `frontend.prev` 恢复成 `frontend`，最后再做一次健康检查。

## 手动回滚

```bash
bash /home/ZacBlog/scripts/zacblog-rollback.sh            # 后端回到上次部署前记下的目标，前端回到 frontend.prev
bash /home/ZacBlog/scripts/zacblog-rollback.sh --frontend-only
```

## 本地测试

```bash
bash deploy/test/run.sh
```

在临时目录里模拟 `/home/ZacBlog`，用桩代替 `systemctl`、`sudo`（只放行上面 sudoers 允许的命令）、`mysqldump` 和 `java`（启动一个按“jar”内容返回 UP/DOWN 的本地 HTTP 服务）。覆盖：首次切换成功、健康检查失败回滚到上一个 release、首次切换失败回滚到旧后端、备份失败中止、环境变量文件缺失或权限不对。不需要 Docker、root、MySQL；装了 shellcheck 会顺带检查脚本。

所有脚本的路径和命令都可以用环境变量覆盖（见 `deploy/scripts/zacblog-common.sh` 开头），例如 `HEALTH_TIMEOUT=180`。
