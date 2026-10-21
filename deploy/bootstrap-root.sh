#!/usr/bin/env bash
# 一次性的 root 初始化（主人审过后手动执行一次；可重复执行）。
#
#   ssh vps "bash -s -- '$(cat ~/.ssh/zacblog_deploy.pub)'" < deploy/bootstrap-root.sh
#   bash deploy/bootstrap-root.sh --print      # 不需要 root：只打印将要安装的 unit、启动器和 sudoers
#
# 原则：root 运行或读取的任何东西，zacblog 都不能改（包括通过父目录改名替换）。
#
# 做的事：
#   1. 创建部署用户 zacblog（uid/gid 1001，锁定密码，家目录 /home/ZacBlog 已存在，不新建）
#   2. 权限：/home/ZacBlog 本身 root:root 755；backend/ 递归 root 所有、去掉组和其他人的写权限，
#      backend/config/application.yml 改成 600。zacblog 只拥有 .ssh/ server/ web/ scripts/ incoming/
#      backups/ zaaac/ zaaac/uploadPath/
#   3. 用第一个参数传入的公钥【替换】/home/ZacBlog/.ssh/authorized_keys（替换前备份）
#   4. 前端改成软链接发布：现有 frontend/ 移到 web/releases/legacy，
#      web/current → releases/legacy（归 zacblog），/home/ZacBlog/frontend → /home/ZacBlog/web/current（归 root）；
#      完成后 curl 确认 https://blog.zaaac.vip 仍是 200
#   5. 安装启动器 /usr/local/lib/zacblog/zacblog-server-run（root 所有，以 zacblog 身份读 server/zacblog.env、写 server/app.log）
#   6. 安装 zacblog-server.service，daemon-reload（不 enable、不 start）
#   7. 写 /etc/sudoers.d/zacblog-deploy（两个服务的 start/stop/enable/disable，新服务另有 restart/is-active），
#      visudo -cf 校验不通过就不安装
# 不碰 zacblog-backend.service 本身、nginx、上传目录里的文件。
set -euo pipefail

DEPLOY_USER=zacblog
DEPLOY_UID=1001
HOME_DIR=/home/ZacBlog
SERVER_DIR=$HOME_DIR/server
WEB_DIR=$HOME_DIR/web
JAVA=/usr/bin/java
LAUNCHER_DIR=/usr/local/lib/zacblog
LAUNCHER=$LAUNCHER_DIR/zacblog-server-run
UNIT_PATH=/etc/systemd/system/zacblog-server.service
BLOG_URL=${BLOG_URL:-https://blog.zaaac.vip/}
SUDOERS_PATH=/etc/sudoers.d/zacblog-deploy
SYSTEMCTL_PATH=$(command -v systemctl 2>/dev/null || echo /usr/bin/systemctl)

log() { printf '[bootstrap] %s\n' "$*"; }
die() {
  printf '[bootstrap] ERROR: %s\n' "$*" >&2
  exit 1
}

# 不用 EnvironmentFile= 和 StandardOutput=append:：这两个都是 systemd（root）去打开文件，
# 而 server/ 归 zacblog，它可以把 zacblog.env / app.log 换成指向 root 文件的软链接
#（读 root 才能读的文件、以 root 身份往任意文件追加内容）。
# 改由 root 所有的启动器在 User=zacblog 之后读环境变量文件、打开日志。
unit_file() {
  cat <<UNIT
[Unit]
Description=ZacBlog server (v2, Spring Boot)
After=network-online.target mysql.service
Wants=network-online.target

[Service]
Type=simple
User=$DEPLOY_USER
Group=$DEPLOY_USER
WorkingDirectory=$SERVER_DIR
ExecStart=$LAUNCHER
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
}

# 以 zacblog 身份运行：读 server/zacblog.env（systemd EnvironmentFile 格式），日志追加到 server/app.log。
# ZACBLOG_SERVER_DIR / ZACBLOG_JAVA 只给 deploy/test/run.sh 用，systemd 下不会设置。
launcher_file() {
  cat <<'LAUNCHER'
#!/bin/bash
# 由 ZacBlog deploy/bootstrap-root.sh 安装（root 所有）。zacblog-server.service 以 User=zacblog 执行它。
set -euo pipefail
server_dir=${ZACBLOG_SERVER_DIR:-/home/ZacBlog/server}
java=${ZACBLOG_JAVA:-/usr/bin/java}
[ "$(id -u)" != 0 ] || { echo "zacblog-server-run: 不能以 root 运行" >&2; exit 1; }
while IFS= read -r line || [ -n "$line" ]; do
  line=${line%$'\r'}
  case "$line" in '' | '#'* | ';'*) continue ;; esac
  key=${line%%=*}
  [[ "$line" == *=* && "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
  val=${line#*=}
  case "$val" in
    \"*\") val=${val#\"} val=${val%\"} ;;
    \'*\') val=${val#\'} val=${val%\'} ;;
  esac
  export "$key=$val"
done < "$server_dir/zacblog.env"
cd "$server_dir"
exec "$java" -Xms256m -Xmx768m -jar "$server_dir/current.jar" >> "$server_dir/app.log" 2>&1
LAUNCHER
}

sudoers_file() {
  local s=$SYSTEMCTL_PATH
  cat <<SUDOERS
# 由 ZacBlog deploy/bootstrap-root.sh 生成。部署用户只能管理这两个服务的这几个动作。
$DEPLOY_USER ALL=(root) NOPASSWD: $s start zacblog-server, $s stop zacblog-server, $s restart zacblog-server, $s is-active zacblog-server, $s enable zacblog-server, $s disable zacblog-server, $s start zacblog-backend, $s stop zacblog-backend, $s enable zacblog-backend, $s disable zacblog-backend
SUDOERS
}

if [ "${1:-}" = --print-launcher ]; then
  launcher_file
  exit 0
fi
if [ "${1:-}" = --print ]; then
  echo "# ---- $UNIT_PATH"
  unit_file
  echo "# ---- $LAUNCHER"
  launcher_file
  echo "# ---- $SUDOERS_PATH"
  sudoers_file
  exit 0
fi

[ "$(id -u)" = 0 ] || die "需要 root 执行"
pubkey=${1:-}
[ -n "$pubkey" ] || die "用法：bootstrap-root.sh '<公钥>'"
case "$pubkey" in
  *$'\n'*) die "公钥只能是一行" ;;
esac
printf '%s\n' "$pubkey" | grep -qE '^(ssh-ed25519|ssh-rsa|ecdsa-sha2-nistp(256|384|521)|sk-ssh-ed25519@openssh\.com|sk-ecdsa-sha2-nistp256@openssh\.com) [A-Za-z0-9+/]+=* ?.*$' ||
  die "看起来不是 OpenSSH 公钥"
[ -d "$HOME_DIR" ] || die "$HOME_DIR 不存在"
command -v visudo >/dev/null 2>&1 || die "找不到 visudo"
command -v curl >/dev/null 2>&1 || die "找不到 curl"

# ---- 1. 用户和组（uid/gid 1001 已被别的名字占用时停下，不去改它）
if getent group "$DEPLOY_USER" >/dev/null; then
  [ "$(getent group "$DEPLOY_USER" | cut -d: -f3)" = "$DEPLOY_UID" ] || die "组 $DEPLOY_USER 已存在但 gid 不是 $DEPLOY_UID"
elif getent group "$DEPLOY_UID" >/dev/null; then
  die "gid $DEPLOY_UID 已被组 $(getent group "$DEPLOY_UID" | cut -d: -f1) 占用"
else
  groupadd -g "$DEPLOY_UID" "$DEPLOY_USER"
  log "已创建组 ${DEPLOY_USER}（gid ${DEPLOY_UID}）"
fi
if id -u "$DEPLOY_USER" >/dev/null 2>&1; then
  [ "$(id -u "$DEPLOY_USER")" = "$DEPLOY_UID" ] || die "用户 $DEPLOY_USER 已存在但 uid 不是 $DEPLOY_UID"
  log "用户 $DEPLOY_USER 已存在"
elif getent passwd "$DEPLOY_UID" >/dev/null; then
  die "uid $DEPLOY_UID 已被用户 $(getent passwd "$DEPLOY_UID" | cut -d: -f1) 占用"
else
  useradd -u "$DEPLOY_UID" -g "$DEPLOY_UID" -d "$HOME_DIR" -M -s /bin/bash -c "ZacBlog deploy" "$DEPLOY_USER"
  log "已创建用户 ${DEPLOY_USER}（uid ${DEPLOY_UID}，家目录 ${HOME_DIR}）"
fi
passwd -l "$DEPLOY_USER" >/dev/null
log "已锁定 $DEPLOY_USER 的密码（只能用 SSH 公钥登录）"

# ---- 2. 权限：root 用到的东西 zacblog 都不能改
chown root:root "$HOME_DIR"
chmod 755 "$HOME_DIR"
if [ -d "$HOME_DIR/backend" ]; then
  chown -R root:root "$HOME_DIR/backend"
  chmod -R go-w "$HOME_DIR/backend"
  [ ! -f "$HOME_DIR/backend/config/application.yml" ] || chmod 600 "$HOME_DIR/backend/config/application.yml"
  log "$HOME_DIR 和 backend/ 已改为 root 所有；backend/config/application.yml 改为 600"
fi
own_dir() { # own_dir <模式> <目录>：建目录（或修正已有目录）并归 zacblog，不递归
  install -d -m "$1" -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$2"
}
own_dir 700 "$HOME_DIR/.ssh"
own_dir 755 "$SERVER_DIR"
own_dir 755 "$SERVER_DIR/releases"
own_dir 755 "$WEB_DIR"
own_dir 755 "$WEB_DIR/releases"
own_dir 755 "$HOME_DIR/scripts"
own_dir 700 "$HOME_DIR/incoming"
own_dir 700 "$HOME_DIR/backups"
# 上传目录：只改属主，不改权限，不碰里面的文件（本来就归 1001）
for d in "$HOME_DIR/zaaac" "$HOME_DIR/zaaac/uploadPath"; do
  [ -d "$d" ] || install -d -m 755 "$d"
  chown "$DEPLOY_USER:$DEPLOY_USER" "$d"
done
log "zacblog 拥有：.ssh/ server/ web/ scripts/ incoming/ backups/ zaaac/ zaaac/uploadPath/"

# ---- 3. authorized_keys（替换）
tmp_keys=$(mktemp "$HOME_DIR/.ssh/.authorized_keys.XXXXXX")
printf '%s\n' "$pubkey" > "$tmp_keys"
chmod 600 "$tmp_keys"
chown "$DEPLOY_USER:$DEPLOY_USER" "$tmp_keys"
# 替换前留一份带时间戳的备份，误操作时可以从服务器控制台恢复原来的登录入口
if [ -f "$HOME_DIR/.ssh/authorized_keys" ]; then
  keys_bak="$HOME_DIR/.ssh/authorized_keys.bak.$(date '+%Y%m%d-%H%M%S')"
  cp -p "$HOME_DIR/.ssh/authorized_keys" "$keys_bak"
  log "原 authorized_keys 已备份为 $keys_bak"
fi
mv -f "$tmp_keys" "$HOME_DIR/.ssh/authorized_keys"
log "已替换 $HOME_DIR/.ssh/authorized_keys"

# ---- 4. 前端改为软链接：frontend（root）→ web/current（zacblog）→ releases/legacy
if [ -L "$HOME_DIR/frontend" ]; then
  [ "$(readlink "$HOME_DIR/frontend")" = "$WEB_DIR/current" ] ||
    die "$HOME_DIR/frontend 已是软链接但指向 $(readlink "$HOME_DIR/frontend")，请人工检查"
  [ -e "$WEB_DIR/current" ] || die "$WEB_DIR/current 不存在或指向无效目录"
  chown -h root:root "$HOME_DIR/frontend"
  log "前端已经是软链接发布，跳过迁移"
elif [ -d "$HOME_DIR/frontend" ]; then
  [ ! -e "$WEB_DIR/releases/legacy" ] || die "$WEB_DIR/releases/legacy 已存在，请人工检查"
  [ ! -e "$WEB_DIR/current" ] && [ ! -L "$WEB_DIR/current" ] || die "$WEB_DIR/current 已存在，请人工检查"
  ln -s releases/legacy "$WEB_DIR/current"
  chown -h "$DEPLOY_USER:$DEPLOY_USER" "$WEB_DIR/current"
  # 下面两步之间 nginx 会有毫秒级找不到根目录
  mv "$HOME_DIR/frontend" "$WEB_DIR/releases/legacy"
  ln -s "$WEB_DIR/current" "$HOME_DIR/frontend"
  chown -R "$DEPLOY_USER:$DEPLOY_USER" "$WEB_DIR/releases/legacy"
  log "前端已迁移：frontend → web/current → releases/legacy"
else
  die "$HOME_DIR/frontend 不存在"
fi
code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$BLOG_URL" || true)
if [ "$code" != 200 ]; then
  die "$BLOG_URL 返回 HTTP ${code}（期望 200）。恢复方法：rm $HOME_DIR/frontend $WEB_DIR/current && mv $WEB_DIR/releases/legacy $HOME_DIR/frontend"
fi
log "$BLOG_URL 返回 200"

# 预先建好日志文件（启动器以 zacblog 身份追加）
[ -e "$SERVER_DIR/app.log" ] || install -m 640 -o "$DEPLOY_USER" -g "$DEPLOY_USER" /dev/null "$SERVER_DIR/app.log"

# ---- 5. 启动器（root 所有）
install -d -m 755 -o root -g root "$LAUNCHER_DIR"
tmp_launcher=$(mktemp)
launcher_file > "$tmp_launcher"
install -m 755 -o root -g root "$tmp_launcher" "$LAUNCHER"
rm -f "$tmp_launcher"
log "已安装 $LAUNCHER"

# ---- 6. systemd unit（不 enable、不 start）
tmp_unit=$(mktemp)
unit_file > "$tmp_unit"
install -m 644 -o root -g root "$tmp_unit" "$UNIT_PATH"
rm -f "$tmp_unit"
"$SYSTEMCTL_PATH" daemon-reload
log "已安装 $UNIT_PATH 并 daemon-reload（未 enable、未 start）"

# ---- 7. sudoers：临时文件名带“.”，sudo 不会加载它；校验通过才改名生效
tmp_sudoers=$(mktemp "/etc/sudoers.d/.zacblog-deploy.XXXXXX")
trap 'rm -f "$tmp_sudoers"' EXIT
sudoers_file > "$tmp_sudoers"
chmod 440 "$tmp_sudoers"
chown root:root "$tmp_sudoers"
visudo -cf "$tmp_sudoers" >/dev/null || die "sudoers 校验失败，未安装"
mv -f "$tmp_sudoers" "$SUDOERS_PATH"
trap - EXIT
if ! visudo -c >/dev/null; then
  rm -f "$SUDOERS_PATH"
  die "整体 sudoers 校验失败，已移除 $SUDOERS_PATH"
fi
log "已安装 $SUDOERS_PATH"

log "完成。现在可以用部署私钥以 $DEPLOY_USER 登录，并只能免密执行："
sudoers_file | tail -n 1 | sed 's/^[^:]*: //; s/, /\n/g' | sed 's/^/[bootstrap]   sudo -n /'
