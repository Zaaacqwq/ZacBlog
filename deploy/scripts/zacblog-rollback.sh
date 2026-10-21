#!/usr/bin/env bash
# 回滚：zacblog-rollback.sh [--frontend-only]
#   - 前端：$FRONTEND_PREV 存在时恢复成 $FRONTEND_DIR
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
  if [ ! -d "$FRONTEND_PREV" ]; then
    log "没有 ${FRONTEND_PREV}，前端保持不变"
    return 0
  fi
  local failed
  failed=$(mktemp -d "$(dirname "$FRONTEND_DIR")/.frontend.failed.XXXXXX")
  if [ -e "$FRONTEND_DIR" ]; then
    mv "$FRONTEND_DIR" "$failed/frontend"
  fi
  mv "$FRONTEND_PREV" "$FRONTEND_DIR"
  rm -rf "$failed"
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
