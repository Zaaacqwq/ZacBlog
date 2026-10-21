# 部署（ZacBlog v2）

本文以服务器的实际情况为准；仓库根目录的 `ZacBlog_Deploy_Doc.md` 已过时，不要按它操作。

## 设计依据（ZB-7 执行中主人确认，取代任务原文的部分要求）

ZB-7 原始描述假设"zacblog 用户存在、sudo 要密码、后端用 screen + restart_backend.sh 启动、不能用 root/systemd"。
主人核实服务器后在任务中补充了实际情况（见下节）并定下新设计，以下几条原要求因此被替换：

| 原要求 | 现在 |
| --- | --- |
| 不能用 root、systemd；脚本里不能有 sudo、systemctl | 后端由 systemd 管理；sudoers 只放行 10 条 `sudo -n systemctl …`（见下），部署脚本只用其中的 start/stop/restart（测试的 sudo 桩会拒绝白名单外的命令） |
| 用 screen 启停，回滚时按 restart_backend.sh 启动旧 jar | 旧后端本来就是 `zacblog-backend.service`（root），没有 screen；回滚为 `systemctl start zacblog-backend` |
| 沿用现有 zacblog 用户 | 该用户已不存在，由 `deploy/bootstrap-root.sh` 重新创建（uid/gid 1001），由主人审过后以 root 手动执行一次 |
| `source` 环境变量文件 | 逐行解析（`DB_URL` 里的 `&` 不能被 bash source） |
| 环境变量文件 `/home/ZacBlog/zacblog.env` | 改到 `/home/ZacBlog/server/zacblog.env`（主人评审时定） |
| 前端 `frontend` / `frontend.prev` 目录互换 | 软链接发布：`frontend`（root）→ `web/current` → `web/releases/<版本>`（主人评审时定，zacblog 不再需要写 `/home/ZacBlog`） |

**权限原则**（主人评审时定）：root 运行或读取的任何东西，zacblog 都不能改，包括通过父目录改名来替换。
因此 `/home/ZacBlog` 本身和 `backend/` 都归 root；zacblog 只拥有 `.ssh/`、`server/`、`web/`、`scripts/`、`incoming/`、`backups/`、`zaaac/`、`zaaac/uploadPath/`。
按同一原则，`zacblog-server.service` **不用** `EnvironmentFile=` 和 `StandardOutput=append:`：这两个都由 systemd 以 root 身份打开文件，
而 `server/` 归 zacblog，它可以把这两个文件换成指向 root 文件的软链接：读 root 才能读的文件，或者以 root 身份往任意文件追加内容。
所以改由 root 所有的启动器 `/usr/local/lib/zacblog/zacblog-server-run` 在降权到 zacblog 之后，自己读 `server/zacblog.env`、打开 `server/app.log`。

其余要求不变：部署前备份、失败自动回滚（jar 和前端）、上传目录和环境变量文件不动、workflow 只有 `workflow_dispatch` 且不含明文密钥、不改 nginx、不安装软件。

## 服务器现状

| 项 | 情况 |
| --- | --- |
| 旧后端 | systemd 服务 `zacblog-backend.service`，root 运行，`/usr/bin/java -Xms512m -Xmx1024m -jar /home/ZacBlog/backend/zaaac-admin.jar --server.port=8080`，日志 `/home/ZacBlog/backend/app.log`，外置配置 `/home/ZacBlog/backend/config/application.yml` |
| 新后端 | systemd 服务 `zacblog-server.service`（由 `deploy/bootstrap-root.sh` 安装），用户 zacblog，工作目录 `/home/ZacBlog/server`，**不能**在 `backend/` 下启动（否则会加载旧后端的 `config/`） |
| 端口 | 新旧后端都用 8080，同一时间只能有一个在运行 |
| 前端 | nginx 根目录 `/home/ZacBlog/frontend`（`try_files` 回落 `index.html`）。bootstrap 后它是 root 所有的软链接 → `web/current` → `web/releases/<版本>`；nginx 默认跟随软链接，不用改配置 |
| 上传目录 | `/home/ZacBlog/zaaac/uploadPath/`（nginx 的 `/profile/` 直接 alias 到这里）。部署不碰它 |
| 环境变量文件 | `/home/ZacBlog/server/zacblog.env`，归 zacblog、权限 600，格式见下。部署不改它 |
| nginx | `blog.zaaac.vip` 的 `/api/` → `127.0.0.1:8080/`（去前缀），`api.zaaac.vip` 的 `/` → `127.0.0.1:8080/`。部署不改 nginx |
| JDK | 服务器是 JDK 21（`/usr/bin/java`），按 17 编译 |
| MySQL / Redis | 本机 |

部署用户 `zacblog`（uid/gid 1001）只能免密执行：

```
sudo -n systemctl start|stop|restart|is-active|enable|disable zacblog-server
sudo -n systemctl start|stop|enable|disable zacblog-backend
```

## 开机自启（ZB-8 切换时用）

bootstrap 之后，旧服务 `zacblog-backend` 仍是 enable 状态，新服务 `zacblog-server` 没有 enable。部署脚本**不改**开机自启。所以只做切换、不改自启的话，机器一重启就会悄悄回到旧后端。

- 切换成功、确认稳定后：

  ```bash
  sudo -n systemctl disable zacblog-backend
  sudo -n systemctl enable zacblog-server
  ```

- 回滚到旧后端时反过来：

  ```bash
  sudo -n systemctl disable zacblog-server
  sudo -n systemctl enable zacblog-backend
  ```

## 目录布局

```
/home/ZacBlog/                root:root 755（zacblog 不能在这里建、删、改名）
  frontend -> /home/ZacBlog/web/current   root 所有的软链接，nginx 根目录
  backend/                    旧后端，递归归 root、去掉组和其他人的写权限；config/application.yml 600
  .ssh/                       zacblog 700
  scripts/zacblog-*.sh        zacblog；workflow 每次从 deploy/scripts/ 上传（旧的 restart_backend.sh 不动、不用）
  incoming/                   zacblog 700；workflow 上传的产物（部署成功后删除）
  server/                     zacblog
    zacblog.env               新后端的环境变量（600，不进仓库，由 workflow 生成）
    releases/<版本>.jar       每次部署一个，保留最近 5 个（当前版本和回滚目标永远保留）
    current.jar -> releases/… 新后端实际运行的版本
    app.log                   新后端日志（启动器以 zacblog 身份追加）
    .rollback-target          最近一次部署记下的后端回滚目标
  web/                        zacblog
    releases/legacy/          bootstrap 从原 frontend/ 迁过来的旧前端（永远保留）
    releases/<时间戳>-<jar 名>/ 每次部署一个，保留最近 5 个（当前版本和回滚目标永远保留）
    current -> releases/…     当前前端
    .rollback-target          最近一次部署记下的前端回滚目标
  backups/                    zacblog 700；zacblog-<时间戳>.sql.gz，每次部署前备份，保留最近 10 份
  zaaac/uploadPath/           zacblog；上传目录，部署不碰里面的文件
```

## 一次性初始化（root，主人执行）

`deploy/bootstrap-root.sh` 依次做这些事，可以重复执行；不碰 `zacblog-backend.service` 本身、nginx、上传目录里的文件：

1. 创建 zacblog 用户。
2. 按上面的布局修正属主和权限：`/home/ZacBlog` 和 `backend/` 归 root，`backend/config/application.yml` 改成 600。
3. 替换 authorized_keys。
4. 把 `frontend/` 迁到 `web/releases/legacy`，建好两层软链接，然后 `curl https://blog.zaaac.vip/` 确认还是 200；不是 200 就报错，并打印恢复命令。
5. 安装启动器和 `zacblog-server.service`（不 enable、不 start）。
6. 写 sudoers（`visudo -cf` 校验通过才安装）。

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

`/home/ZacBlog/server/zacblog.env` 给启动器和备份脚本用，每行一个 `KEY=VALUE`（与 systemd `EnvironmentFile` 格式兼容）：

```
DB_URL=jdbc:mysql://127.0.0.1:3306/zaaac_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
UPLOAD_PATH=/home/ZacBlog/zaaac/uploadPath
```

- 值不需要引号；`&` 之类可以直接写。脚本是逐行解析的，**不会**用 bash `source` 执行它。
- 值里不要写反斜杠、引号和 `$`；首尾成对的引号会被去掉。
- 备份脚本从 `DB_URL` 解析主机、端口、库名，密码通过 `MYSQL_PWD` 环境变量交给 mysqldump，不出现在命令行参数里。

## 部署流程

GitHub Actions `Deploy`（`.github/workflows/deploy.yml`，目前只能手动触发 `workflow_dispatch`）：

1. `build` job：`mvn -B package`（含测试）、`npm ci && npm run build`，打包成 `zacblog-server-<sha>.jar` 和 `web-dist.tgz`。
2. `deploy` job（`environment: production`，需要主人在 GitHub 上批准）：用 `vars.SSH_KNOWN_HOSTS` 写 `known_hosts`，`StrictHostKeyChecking yes`（变量为空就直接失败）；上传 `deploy/scripts/zacblog-*.sh` 到 `/home/ZacBlog/scripts/`、产物到 `/home/ZacBlog/incoming/`，然后执行：

```bash
bash /home/ZacBlog/scripts/zacblog-deploy.sh <jar> <web-dist.tgz>
```

`zacblog-deploy.sh` 依次执行：

| 步骤 | 脚本 | 失败时 |
| --- | --- | --- |
| 1. 备份数据库 | `zacblog-backup.sh` | 中止，前后端都不动 |
| 2. 发布前端：解压到 `web/releases/` 下的临时目录 → 检查 `index.html` → 改名为 `web/releases/<版本>` → 记下 `web/current` 原来的指向 → 原子替换 `web/current` | `zacblog-frontend.sh` | 中止（替换软链接之前失败不影响线上） |
| 3. 记下回滚目标 → jar 放进 `releases/` →（首次切换时）`stop zacblog-backend` → `current.jar` 指向新版本 → `restart zacblog-server` → 轮询 `http://127.0.0.1:8080/actuator/health` 最多 120 秒 | `zacblog-start.sh` | 调 `zacblog-rollback.sh`，workflow 标红并打印 `server/app.log` 末尾 |

回滚目标：

- `zacblog-server` 正在运行 → 回到 `current.jar` 原来指向的 release（`restart zacblog-server`）。
- 旧后端 `zacblog-backend` 正在运行（首次切换）→ `stop zacblog-server`、删掉 `current.jar`、`start zacblog-backend`，检查端口有响应（旧后端没有 actuator）。

回滚同时把 `web/current` 指回部署前的前端 release（回滚记录用一次就删除），最后再做一次健康检查。

如果 `zacblog-start.sh` 在改动后端之前就失败了（新 jar 缺失、环境变量文件不对、回滚记录写不进去等），它以退出码 3 退出，`zacblog-deploy.sh` 只恢复前端，并报告"后端没有改动"，不会误报已回滚。回滚记录先写临时文件再 rename，写失败不会截断上一次的记录。

## 手动回滚

```bash
bash /home/ZacBlog/scripts/zacblog-rollback.sh            # 后端回到上次部署前记下的目标，前端回到上次部署前的 release
bash /home/ZacBlog/scripts/zacblog-rollback.sh --frontend-only
```

## 本地测试

```bash
bash deploy/test/run.sh
```

在临时目录里模拟 `/home/ZacBlog`（`/home/ZacBlog` 本身和 `backend/` 设为只读，模拟归 root），用桩代替 `systemctl`、`sudo`（只放行上面 sudoers 允许的命令）、`mysqldump` 和 `java`（启动一个按“jar”内容返回 UP/DOWN 的本地 HTTP 服务）。`zacblog-server` 的桩会执行 bootstrap 生成的真实启动器。覆盖：首次切换成功、健康检查失败回滚到上一个 release、首次切换失败回滚到旧后端、备份失败中止、环境变量文件缺失或权限不对、回滚记录写不进去，以及前后端旧 release 的清理。不需要 Docker、root、MySQL；装了 shellcheck 会顺带检查脚本。

所有脚本的路径和命令都可以用环境变量覆盖（见 `deploy/scripts/zacblog-common.sh` 开头），例如 `HEALTH_TIMEOUT=180`。
