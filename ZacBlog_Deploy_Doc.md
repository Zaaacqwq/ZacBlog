# 🚀 ZacBlog 全自动部署文档

------

## 1️⃣ 服务器环境准备

### 🖥 系统环境

- Ubuntu 20.04+ / Debian 系列均可

- 已安装：

  ```
  sudo apt update
  sudo apt install -y openjdk-17-jdk nodejs npm git nginx rsync
  ```

- **Java 版本**：

  ```
  java -version  # 应为 17.x
  ```

### 🧑 用户与目录

项目统一运行在普通用户 `zacblog` 下：

```
sudo adduser --home /home/ZacBlog --shell /bin/bash zacblog
sudo usermod -aG sudo zacblog
```

创建部署目录：

```
sudo mkdir -p /home/ZacBlog/{frontend,backend,scripts}
sudo chown -R zacblog:zacblog /home/ZacBlog
```

------

## 2️⃣ SSH 无密码登录配置（GitHub Actions）

### 🪪 在本地生成密钥

```
ssh-keygen -t ed25519 -C "github-actions" -f github_actions_deploy_key
```

得到：

- 私钥：`github_actions_deploy_key`
- 公钥：`github_actions_deploy_key.pub`

### 📋 把公钥加到服务器

以 root 或 sudo 身份执行：

```
sudo mkdir -p /home/ZacBlog/.ssh
sudo cat github_actions_deploy_key.pub | sudo tee /home/ZacBlog/.ssh/authorized_keys
sudo chown -R zacblog:zacblog /home/ZacBlog/.ssh
sudo chmod 700 /home/ZacBlog/.ssh
sudo chmod 600 /home/ZacBlog/.ssh/authorized_keys
```

### 🧩 测试密钥登录

```
ssh -i github_actions_deploy_key zacblog@198.98.53.225 'echo OK'
# 返回 OK 即成功
```

### 🔒 把私钥转为 Base64（Mac）

```
base64 -b 0 < github_actions_deploy_key > github_actions_deploy_key.b64
```

### 🔑 添加到 GitHub Secrets

在仓库中进入：

> Settings → Secrets and variables → Actions → New repository secret

逐个添加：

| 名称               | 示例值                           |
| ------------------ | -------------------------------- |
| `SSH_HOST`         | `198.98.53.225`                  |
| `SSH_PORT`         | `22`                             |
| `SSH_USER`         | `zacblog`                        |
| `SSH_KEY_B64`      | *(粘贴 `.b64` 文件的一整行内容)* |
| `BACKEND_DIR`      | `/home/ZacBlog/backend`          |
| `FRONTEND_DIR`     | `/home/ZacBlog/frontend`         |
| `BACKEND_SCREEN`   | `ZacBlog-Backend`                |
| `BACKEND_JAR_NAME` | `zaaac-admin.jar`                |
| `JAVA_BIN`         | `/usr/bin/java`                  |

------

## 3️⃣ Nginx 配置

编辑：

```
sudo nano /etc/nginx/sites-available/zacblog.conf
```

内容：

```
server {
    listen 80;
    server_name zaaac.vip;

    root /home/ZacBlog/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

启用并检查：

```
sudo ln -sf /etc/nginx/sites-available/zacblog.conf /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

------

## 4️⃣ 后端运行脚本

文件：`/home/ZacBlog/scripts/restart_backend.sh`

```
#!/usr/bin/env bash
set -e

BACKEND_DIR="${1:-/home/ZacBlog/backend}"
SCREEN_NAME="${2:-ZacBlog-Backend}"
JAVA_BIN="${3:-java}"
JAR_NAME="${4:-zaaac-admin.jar}"
JAVA_OPTS="-Xms256m -Xmx1024m -Dfile.encoding=UTF-8"

cd "$BACKEND_DIR"

if screen -list | grep -q "$SCREEN_NAME"; then
  screen -S "$SCREEN_NAME" -X quit || true
  sleep 1
fi

pids=$(pgrep -f "$JAR_NAME" || true)
if [ -n "$pids" ]; then
  kill $pids || true
  sleep 1
fi

screen -dmS "$SCREEN_NAME" bash -lc "$JAVA_BIN $JAVA_OPTS -jar \"$BACKEND_DIR/$JAR_NAME\" >> \"$BACKEND_DIR/app.log\" 2>&1"
echo "✅ Started $JAR_NAME in screen: $SCREEN_NAME"
```

执行权限：

```
chmod +x /home/ZacBlog/scripts/restart_backend.sh
```

------

## 5️⃣ GitHub Actions 自动部署

### 📁 文件位置

放在仓库：

```
.github/workflows/
├── deploy-frontend.yml
└── deploy-backend.yml
```

------

### 🟩 deploy-frontend.yml

```
name: Deploy Frontend
on:
  push:
    branches: [ "main" ]
    paths:
      - "frontend/**"
      - ".github/workflows/deploy-frontend.yml"
  workflow_dispatch: {}

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Build frontend
        working-directory: frontend
        run: |
          npm ci
          npm run build

      - name: Decode SSH key
        run: |
          echo "${{ secrets.SSH_KEY_B64 }}" | base64 -d > /tmp/gha_id
          chmod 600 /tmp/gha_id

      - name: SSH check
        run: |
          ssh -i /tmp/gha_id -o StrictHostKeyChecking=no -p ${{ secrets.SSH_PORT }} \
            ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }} \
            'echo ✅ SSH Connected && whoami'

      - name: Tar dist
        run: tar -czf dist.tgz -C frontend dist

      - name: Upload & deploy
        run: |
          scp -i /tmp/gha_id -o StrictHostKeyChecking=no -P ${{ secrets.SSH_PORT }} dist.tgz \
            ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }}:/home/ZacBlog/
          ssh -i /tmp/gha_id -o StrictHostKeyChecking=no -p ${{ secrets.SSH_PORT }} \
            ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }} '
              set -e
              mkdir -p /home/ZacBlog/frontend
              rm -rf /home/ZacBlog/frontend/*
              tar -xzf /home/ZacBlog/dist.tgz -C /home/ZacBlog
              mv /home/ZacBlog/dist/* /home/ZacBlog/frontend/
              rm -rf /home/ZacBlog/dist /home/ZacBlog/dist.tgz
              echo ✅ Frontend Deployed
            '
```

------

### 🟧 deploy-backend.yml

```
name: Deploy Backend
on:
  push:
    branches: [ "main" ]
    paths:
      - "backend/**"
      - ".github/workflows/deploy-backend.yml"
  workflow_dispatch: {}

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Build backend (multi-module)
        working-directory: backend
        run: mvn -B -DskipTests clean install

      - name: Copy final jar
        run: cp backend/zaaac-admin/target/zaaac-admin.jar ./zaaac-admin.jar

      - name: Decode SSH key
        run: |
          echo "${{ secrets.SSH_KEY_B64 }}" | base64 -d > /tmp/gha_id
          chmod 600 /tmp/gha_id

      - name: SSH check
        run: |
          ssh -i /tmp/gha_id -o StrictHostKeyChecking=no -p ${{ secrets.SSH_PORT }} \
            ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }} 'echo ✅ SSH Connected && whoami'

      - name: Upload jar & restart backend
        run: |
          scp -i /tmp/gha_id -o StrictHostKeyChecking=no -P ${{ secrets.SSH_PORT }} zaaac-admin.jar \
            ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }}:${{ secrets.BACKEND_DIR }}/
          ssh -i /tmp/gha_id -o StrictHostKeyChecking=no -p ${{ secrets.SSH_PORT }} \
            ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }} "
              bash /home/ZacBlog/scripts/restart_backend.sh \
                '${{ secrets.BACKEND_DIR }}' \
                '${{ secrets.BACKEND_SCREEN }}' \
                '${{ secrets.JAVA_BIN }}' \
                '${{ secrets.BACKEND_JAR_NAME }}'
              tail -n 30 '${{ secrets.BACKEND_DIR }}/app.log' || true
              echo ✅ Backend Deployed
            "
```

------

## 6️⃣ 测试部署

1️⃣ 推送代码到 `main` 分支
 2️⃣ 打开 GitHub → **Actions**
 3️⃣ 选择 `Deploy Backend` 或 `Deploy Frontend` → **Run workflow**
 4️⃣ 等待运行成功（绿色✅）
 5️⃣ 浏览器访问：

- 前端页面：https://zaaac.vip
- API 接口：https://zaaac.vip/api/***

------

## 7️⃣ 故障排查快速表

| 问题                            | 可能原因                     | 解决方案                                       |
| ------------------------------- | ---------------------------- | ---------------------------------------------- |
| `Permission denied (publickey)` | SSH 私钥粘贴错误             | 重新生成并用 Base64 添加到 `SSH_KEY_B64`       |
| `Could not find artifact ...`   | 后端多模块未 install         | workflow 改为 `mvn clean install`              |
| 前端没更新                      | Nginx 缓存或浏览器缓存       | `sudo nginx -t && sudo systemctl reload nginx` |
| 后端未启动                      | screen 名称不匹配            | 确保 Secrets 里的 `BACKEND_SCREEN` 与脚本一致  |
| 自动化日志卡住                  | `StrictHostKeyChecking` 问题 | 我们的 workflow 已禁用 host key 检查           |

------

## ✅ 最终目录结构

服务器端：

```
/home/ZacBlog/
├── backend/
│   ├── zaaac-admin.jar
│   └── app.log
├── frontend/
│   ├── index.html
│   └── assets/
├── scripts/
│   └── restart_backend.sh
└── .ssh/
    └── authorized_keys
```

------

完成以上所有步骤后，你的 ZacBlog 系统即实现：

> **Push 到 main → 自动构建 → 自动上传 → 自动部署 → 即刻上线** 🎯
