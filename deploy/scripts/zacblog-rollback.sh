#!/usr/bin/env bash
# 回滚：zacblog-rollback.sh [--frontend-only]
#   - 前端：web/current 指回 zacblog-frontend.sh 记下的上一个 release（$WEB_STATE_FILE）
#   - 后端：按 zacblog-start.sh 记下的回滚目标恢复
#       app <jar>  current.jar 指回 <jar>，restart zacblog-server
#       legacy     停 zacblog-server、删掉 current.jar，启动旧的 zacblog-backend（首次切换失败时）
#       none       停 zacblog-server、删掉 current.jar
#   - 最后再做一次健康检查；不通过就以非 0 退出
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=zacblog-common.sh
. "$SCRIPT_DIR/zacblog-common.sh"

restore_frontend() {
  local prev
  prev=$(cat "$WEB_STATE_FILE" 2>/dev/null || echo none)
  if [ "$prev" = none ] || [ ! -d "$prev" ]; then
    log "没有可用的前端回滚记录，前端保持不变"
    return 0
  fi
  swap_link "$WEB_CURRENT" "$prev"
  # 记录只用一次，避免重复回滚把前端来回切
  rm -f "$WEB_STATE_FILE"
  log "前端已恢复为部署前的版本"
}

restore_frontend
if [ "${1:-}" = --frontend-only ]; then
  exit 0
fi

target=$(cat "$STATE_FILE" 2>/dev/null || echo none)
log "回滚后端，目标：$target"

case "$target" in
  app\ *)
    prev=${target#app }
    [ -f "$prev" ] || die "找不到上一个版本 $prev"
    point_current_to "$prev"
    svc restart "$APP_SERVICE"
    wait_health "$HEALTH_URL" strict
    ;;
  legacy)
    svc stop "$APP_SERVICE" || true
    # 失败的版本不再作为 current，下次部署时才会正确地把旧后端当作回滚目标
    rm -f "$CURRENT_JAR"
    svc start "$LEGACY_SERVICE"
    wait_health "$LEGACY_HEALTH_URL" any
    ;;
  *)
    svc stop "$APP_SERVICE" || true
    rm -f "$CURRENT_JAR"
    die "没有可回滚的后端版本"
    ;;
esac
log "回滚完成"
