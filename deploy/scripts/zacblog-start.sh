#!/usr/bin/env bash
# 发布新后端：zacblog-start.sh <jar> [版本名]
#   1. 记下当前运行的版本作为回滚目标（写到 ${STATE_FILE}）：
#        app <jar>  zacblog-server 正在运行：current.jar 当前指向的 release
#        legacy     旧的 zacblog-backend 正在运行（首次切换）
#        none       都没在运行，也没有可回滚的版本
#   2. jar 复制为 releases/<版本名>.jar；首次切换时先停 zacblog-backend（两者共用 8080）；
#      current.jar 指向新 release，restart zacblog-server（服务自己读 zacblog.env，日志写 server/app.log）
#   3. 轮询 ${HEALTH_URL}，最多 HEALTH_TIMEOUT 秒
#   4. 失败时调用 zacblog-rollback.sh（jar 和前端都恢复）
# 版本名默认是 <时间戳>-<jar 文件名去掉 .jar>，保证每次部署都不同。
#
# 退出码：0 成功；1 失败且已回滚；2 失败且回滚也失败；3 前置检查失败，什么都没改
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=zacblog-common.sh
. "$SCRIPT_DIR/zacblog-common.sh"

new_jar=${1:-}
if [ -z "$new_jar" ] || [ ! -f "$new_jar" ]; then
  log "ERROR: 新 jar 不存在：${new_jar:-（未指定）}" >&2
  exit 3
fi
base=${new_jar##*/}
version=${2:-$(date '+%Y%m%d-%H%M%S')-${base%.jar}}
if ! (require_env_file) || ! mkdir -p "$RELEASES_DIR"; then
  exit 3
fi
release="$RELEASES_DIR/$version.jar"

if svc_active "$APP_SERVICE" && [ -L "$CURRENT_JAR" ]; then
  target="app $(readlink "$CURRENT_JAR")"
elif svc_active "$LEGACY_SERVICE"; then
  target=legacy
elif [ -L "$CURRENT_JAR" ] && [ -f "$CURRENT_JAR" ]; then
  target="app $(readlink "$CURRENT_JAR")"
elif [ -f "$LEGACY_JAR" ]; then
  target=legacy
else
  target=none
fi
printf '%s\n' "$target" > "$STATE_FILE"
log "回滚目标：$target"

# 从这里开始会改动后端，任何失败都要回滚
rollback_on_failure() {
  local rc=$?
  trap - EXIT
  [ "$rc" -ne 0 ] || exit 0
  log "部署失败，开始回滚"
  if bash "$SCRIPT_DIR/zacblog-rollback.sh"; then
    exit 1
  fi
  log "ERROR: 回滚失败，需要人工处理" >&2
  exit 2
}
trap rollback_on_failure EXIT

cp "$new_jar" "$release.tmp"
mv -f "$release.tmp" "$release"
log "新版本：$release"

if [ "$target" = legacy ] && svc_active "$LEGACY_SERVICE"; then
  log "停止旧后端 $LEGACY_SERVICE"
  svc stop "$LEGACY_SERVICE"
fi
point_current_to "$release"
log "重启 $APP_SERVICE"
svc restart "$APP_SERVICE"
wait_health "$HEALTH_URL" strict

trap - EXIT
log "部署成功：$version"

# 只保留最近 RELEASES_KEEP 个 release；当前版本和回滚目标永远保留
keep_prev=${target#app }
find "$RELEASES_DIR" -maxdepth 1 -type f -name '*.jar' | sort -r | tail -n +"$((RELEASES_KEEP + 1))" |
  while IFS= read -r f; do
    if [ "$f" != "$release" ] && [ "$f" != "$keep_prev" ]; then
      log "删除旧版本 $f"
      rm -f -- "$f"
    fi
  done
