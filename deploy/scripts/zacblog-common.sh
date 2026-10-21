# shellcheck shell=bash
# zacblog-*.sh 共用的默认值和函数，由各脚本 source，不单独执行。
#
# 以部署用户 zacblog 运行。它只能免密执行 deploy/bootstrap-root.sh 写进 sudoers 的几条命令：
#   systemctl start|stop|restart|is-active zacblog-server
#   systemctl start|stop zacblog-backend
#   systemctl enable|disable zacblog-server|zacblog-backend（切换时用，部署脚本本身不调用）
# zacblog 不能写 /home/ZacBlog 本身和 backend/（root 所有），只能写 server/、web/、scripts/、
# incoming/、backups/、zaaac/。所以发布前后端都是“放进 releases/ + 原子替换软链接”。
# 所有路径和命令都可以用环境变量覆盖（deploy/test/run.sh 就是这样在临时目录里模拟服务器的）。

: "${ZACBLOG_HOME:=/home/ZacBlog}"

# 新后端（zacblog-server.service）：WorkingDirectory 是 server/，绝不能是 backend/
#（backend/config/application.yml 是旧后端的外置配置，Spring Boot 会自动加载工作目录下的 config/）
: "${SERVER_DIR:=$ZACBLOG_HOME/server}"
: "${ENV_FILE:=$SERVER_DIR/zacblog.env}"
: "${RELEASES_DIR:=$SERVER_DIR/releases}"
: "${CURRENT_JAR:=$SERVER_DIR/current.jar}"
: "${APP_LOG:=$SERVER_DIR/app.log}"
: "${STATE_FILE:=$SERVER_DIR/.rollback-target}"
: "${RELEASES_KEEP:=5}"
: "${APP_SERVICE:=zacblog-server}"
: "${HEALTH_URL:=http://127.0.0.1:8080/actuator/health}"

# 旧后端（zacblog-backend.service，若依 zaaac-admin.jar，以 root 运行）。部署只会 start/stop 它。
: "${LEGACY_SERVICE:=zacblog-backend}"
: "${LEGACY_JAR:=$ZACBLOG_HOME/backend/zaaac-admin.jar}"
# 旧后端没有 actuator：端口上有任意 HTTP 响应就算起来了
: "${LEGACY_HEALTH_URL:=http://127.0.0.1:8080/}"

# 前端：/home/ZacBlog/frontend 是 root 所有的软链接 → web/current → web/releases/<版本>
: "${WEB_DIR:=$ZACBLOG_HOME/web}"
: "${WEB_RELEASES_DIR:=$WEB_DIR/releases}"
: "${WEB_CURRENT:=$WEB_DIR/current}"
: "${WEB_STATE_FILE:=$WEB_DIR/.rollback-target}"
: "${WEB_RELEASES_KEEP:=5}"
: "${BACKUP_DIR:=$ZACBLOG_HOME/backups}"
: "${BACKUP_KEEP:=10}"

: "${HEALTH_TIMEOUT:=120}"
: "${HEALTH_INTERVAL:=2}"

# 改变服务状态要提权（-n：需要密码时直接失败，不会卡住等输入）；只读查询不需要
: "${SYSTEMCTL:=sudo -n systemctl}"
: "${SYSTEMCTL_QUERY:=systemctl}"
: "${CURL_BIN:=curl}"
: "${MYSQLDUMP_BIN:=mysqldump}"

log() { printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
die() { log "ERROR: $*" >&2; exit 1; }

# 用 root 跑会留下 root 所有的文件，之后 zacblog 就删不掉了
if [ "$(id -u)" = 0 ] && [ "${ALLOW_ROOT:-0}" != 1 ]; then
  die "请以部署用户（zacblog）运行，不要用 root"
fi

# SYSTEMCTL 故意不加引号，允许写成多个词
svc() {
  # shellcheck disable=SC2086
  $SYSTEMCTL "$@"
}
svc_active() {
  # shellcheck disable=SC2086
  $SYSTEMCTL_QUERY is-active --quiet "$1"
}

# 文件权限的八进制表示（GNU stat 和 BSD stat 参数不同）
file_mode() { stat -c '%a' "$1" 2>/dev/null || stat -f '%Lp' "$1"; }

# 环境变量文件必须存在，并且只有属主可读写（600/400）
require_env_file() {
  [ -f "$ENV_FILE" ] || die "环境变量文件不存在：$ENV_FILE"
  local mode
  mode=$(file_mode "$ENV_FILE")
  case "$mode" in
    600 | 400) ;;
    *) die "环境变量文件权限是 ${mode}，必须是 600：$ENV_FILE" ;;
  esac
}

# env_file_get <KEY>：从环境变量文件里取一个值。
# 文件是 systemd EnvironmentFile 格式（KEY=VALUE，每行一个，值不需要引号），
# 里面的 DB_URL 常带 &，不能直接用 bash source，所以逐行解析，不执行文件内容。
# 与 systemd 一致：忽略空行和 # ; 注释，去掉成对的引号，同名取最后一个。
env_file_get() {
  local line key val found=1 out=''
  while IFS= read -r line || [ -n "$line" ]; do
    line=${line%$'\r'}
    case "$line" in '' | '#'* | ';'*) continue ;; esac
    key=${line%%=*}
    [ "$key" = "$1" ] || continue
    val=${line#*=}
    case "$val" in
      \"*\") val=${val#\"} val=${val%\"} ;;
      \'*\') val=${val#\'} val=${val%\'} ;;
    esac
    out=$val
    found=0
  done < "$ENV_FILE"
  printf '%s' "$out"
  return "$found"
}

# swap_link <软链接> <目标>：原子地让软链接指向新目标（先建临时软链接再 rename 覆盖）。
# 软链接指向目录时普通 mv 会把临时链接挪进目录里，所以 GNU 用 -T，BSD/macOS 用 -h。
swap_link() {
  local link=$1 target=$2 tmp="$1.tmp.$$"
  ln -sfn "$target" "$tmp"
  if mv --version >/dev/null 2>&1; then
    mv -fT "$tmp" "$link"
  else
    mv -fh "$tmp" "$link"
  fi
  log "${link##*/} → $target"
}
point_current_to() { swap_link "$CURRENT_JAR" "$1"; }

# write_state <文件> <内容>：先写临时文件再 rename，写失败不会截断原记录
write_state() {
  local tmp="$1.tmp.$$"
  if printf '%s\n' "$2" > "$tmp" && mv -f "$tmp" "$1"; then
    return 0
  fi
  rm -f "$tmp" 2>/dev/null || true
  return 1
}

# wait_health <url> <strict|any>
#   strict：HTTP 200 且响应里有 "UP"（Spring actuator）
#   any：   有任意 HTTP 响应即可（旧后端）
wait_health() {
  local url=$1 mode=$2 code='' body start=$SECONDS
  body=$(mktemp)
  log "健康检查 ${url}（最多 ${HEALTH_TIMEOUT}s）"
  while [ $((SECONDS - start)) -lt "$HEALTH_TIMEOUT" ]; do
    code=$("$CURL_BIN" -s -o "$body" -w '%{http_code}' --max-time 5 "$url" 2>/dev/null || true)
    if { [ "$mode" = strict ] && [ "$code" = 200 ] && grep -q '"UP"' "$body"; } ||
      { [ "$mode" = any ] && [ -n "$code" ] && [ "$code" != 000 ]; }; then
      rm -f "$body"
      log "健康检查通过（HTTP ${code}）"
      return 0
    fi
    sleep "$HEALTH_INTERVAL"
  done
  rm -f "$body"
  log "健康检查失败：${HEALTH_TIMEOUT}s 内没有通过（最后一次 HTTP ${code:-000}）"
  return 1
}
