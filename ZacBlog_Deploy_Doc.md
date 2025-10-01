---
title: 项目部署与运行文档
---

# 1. 项目目录

项目部署在服务器目录：  
`/home/ZacBlog/`

- 前端：纯静态页面（Vue/React build 输出）。
- 后端：Node.js / FastAPI / 其他服务，需要长期运行，建议通过 **screen** 维护进程。

---

# 2. 前端部署步骤

### 2.1 本地构建
```bash
cd frontend   # 本地前端源码目录
npm install   # 第一次需要安装依赖
npm run build:prod   # 构建生产环境版本
```
> 构建完成后会生成一个 `dist/` 文件夹。

---

### 2.2 上传到服务器
> 为避免出现旧文件和新文件混合，建议先清空部署目录，再上传。

```bash
# 清空旧文件（谨慎操作，确认路径！）
ssh root@198.98.53.225 "rm -rf /home/ZacBlog/frontend/*"

# 上传新的构建产物
scp -r dist/* root@198.98.53.225:/home/ZacBlog/frontend/
```

---

### 2.3 Nginx 配置
编辑 `/etc/nginx/sites-enabled/default`：

```nginx
server {
    listen 80;
    server_name zaaac.vip;

    root /home/ZacBlog/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

> ⚠️ 如果启用 HTTPS，请在 `listen 443 ssl;` 中加上证书路径：  
> `/etc/letsencrypt/live/zaaac.vip/fullchain.pem`  
> `/etc/letsencrypt/live/zaaac.vip/privkey.pem`

---

### 2.4 重启 Nginx
```bash
sudo nginx -t         # 检查配置是否正确
sudo systemctl reload nginx
```

---

### 2.5 回滚方案（推荐）
如果担心新版本有问题，可以采用版本目录策略：

```bash
# 上传到 releases 下
scp -r dist/* root@198.98.53.225:/home/ZacBlog/releases/20251001/

# 切换软链接
ssh root@198.98.53.225 "ln -sfn /home/ZacBlog/releases/20251001 /home/ZacBlog/frontend && sudo systemctl reload nginx"
```

回滚时只需切回之前的 releases 目录。

---

# 3. 后端部署步骤

### 3.1 进入后端目录
```bash
cd /home/ZacBlog/backend
```

### 3.2 安装依赖
```bash
npm install
# 或 pip install -r requirements.txt
```

### 3.3 启动后端（使用 screen 保持运行）
```bash
screen -S ZacBlog-Backend
npm start
# 或 python main.py
```

后端 API 应该监听在 `http://localhost:8000`。  
Nginx 将代理到 `https://api.zaaac.vip`。

### 3.4 退出 screen（保持进程运行）
按下 `[Ctrl] + A`，然后 `[Ctrl] + D`

---

# 4. screen 常用命令

- 新建 session：  
  ```bash
  screen -S <session名字>
  ```
- 查看 session：  
  ```bash
  screen -ls
  ```
- 进入 session：  
  ```bash
  screen -r <session ID.session名字>
  ```
  例：`screen -r 71180.ZacBlog-Backend`
- 删除 session：  
  ```bash
  screen -S <session ID.session名字> -X quit
  ```
- 滚动历史内容：  
  在 session 内按 `[ESC]` 进入滚动模式，再按 `[ESC]` 退出。

---

# 5. HTTPS 证书（已完成）

证书路径：
- `/etc/letsencrypt/live/zaaac.vip/fullchain.pem`
- `/etc/letsencrypt/live/zaaac.vip/privkey.pem`

- `zaaac.vip` → 前端站点  
- `api.zaaac.vip` → 后端 API  

> Certbot 已设置自动续期，无需手动操作。

---

# 6. 访问地址

✅ 部署完成后可访问：

- 前端： [https://zaaac.vip](https://zaaac.vip)  
- 后端 API： [https://api.zaaac.vip](https://api.zaaac.vip)

---
