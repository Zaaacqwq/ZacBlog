#!/usr/bin/env bash
# 一次性的 root 初始化（主人审过后手动执行一次；可重复执行）。
#
#   ssh vps "bash -s -- '$(cat ~/.ssh/zacblog_deploy.pub)'" < deploy/bootstrap-root.sh
#   bash deploy/bootstrap-root.sh --print      # 不需要 root：只打印将要安装的 unit 和 sudoers
#
# 做的事：
#   1. 创建部署用户 zacblog（uid/gid 1001，锁定密码，家目录 /home/ZacBlog 已存在，不新建）
#   2. 用第一个参数传入的公钥【替换】/home/ZacBlog/.ssh/authorized_keys
#   3. 建 /home/ZacBlog/server、server/releases、/home/ZacBlog/backups（归 zacblog）
#   4. 安装 zacblog-server.service 并 daemon-reload（不 enable、不 start）
#   5. 写 /etc/sudoers.d/zacblog-deploy，visudo -cf 校验不通过就不安装
# 不碰 zacblog-backend.service、nginx、/home/ZacBlog/backend、上传目录和 zacblog.env。
set -euo pipefail

DEPLOY_USER=zacblog
DEPLOY_UID=1001
HOME_DIR=/home/ZacBlog
SERVER_DIR=$HOME_DIR/server
JAVA=/usr/bin/java
UNIT_PATH=/etc/systemd/system/zacblog-server.service
SUDOERS_PATH=/etc/sudoers.d/zacblog-deploy
SYSTEMCTL_PATH=$(command -v systemctl 2>/dev/null || echo /usr/bin/systemctl)

log() { printf '[bootstrap] %s\n' "$*"; }
die() {
  printf '[bootstrap] ERROR: %s\n' "$*" >&2
  exit 1
}

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
EnvironmentFile=$HOME_DIR/zacblog.env
ExecStart=$JAVA -Xms256m -Xmx768m -jar $SERVER_DIR/current.jar
StandardOutput=append:$SERVER_DIR/app.log
StandardError=append:$SERVER_DIR/app.log
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
}

sudoers_file() {
  local s=$SYSTEMCTL_PATH
  cat <<SUDOERS
# 由 ZacBlog deploy/bootstrap-root.sh 生成。部署用户只能管理这两个服务的这几个动作。
$DEPLOY_USER ALL=(root) NOPASSWD: $s start zacblog-server, $s stop zacblog-server, $s restart zacblog-server, $s is-active zacblog-server, $s start zacblog-backend, $s stop zacblog-backend
SUDOERS
}

if [ "${1:-}" = --print ]; then
  echo "# ---- $UNIT_PATH"
  unit_file
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

# 家目录本身归 zacblog（发布前端要在这里 mv frontend/frontend.prev），不递归；
# sshd 的 StrictModes 要求家目录不能被组/其他用户写
chown "$DEPLOY_USER:$DEPLOY_USER" "$HOME_DIR"
chmod go-w "$HOME_DIR"

# ---- 2. authorized_keys（替换）
install -d -m 700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$HOME_DIR/.ssh"
tmp_keys=$(mktemp "$HOME_DIR/.ssh/.authorized_keys.XXXXXX")
printf '%s\n' "$pubkey" > "$tmp_keys"
chmod 600 "$tmp_keys"
chown "$DEPLOY_USER:$DEPLOY_USER" "$tmp_keys"
mv -f "$tmp_keys" "$HOME_DIR/.ssh/authorized_keys"
log "已替换 $HOME_DIR/.ssh/authorized_keys"

# ---- 3. 目录
install -d -m 755 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$SERVER_DIR" "$SERVER_DIR/releases" "$HOME_DIR/scripts"
install -d -m 700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$HOME_DIR/backups"
# 预先建好日志文件，避免 systemd 以 root 身份创建后 zacblog 读不到
[ -e "$SERVER_DIR/app.log" ] || install -m 640 -o "$DEPLOY_USER" -g "$DEPLOY_USER" /dev/null "$SERVER_DIR/app.log"
log "目录已就绪：${SERVER_DIR}、$SERVER_DIR/releases、$HOME_DIR/backups"

# ---- 4. systemd unit（不 enable、不 start）
tmp_unit=$(mktemp)
unit_file > "$tmp_unit"
install -m 644 -o root -g root "$tmp_unit" "$UNIT_PATH"
rm -f "$tmp_unit"
"$SYSTEMCTL_PATH" daemon-reload
log "已安装 $UNIT_PATH 并 daemon-reload（未 enable、未 start）"

# ---- 5. sudoers：临时文件名带“.”，sudo 不会加载它；校验通过才改名生效
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
