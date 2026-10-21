#!/usr/bin/env bash
# 一次完整部署：zacblog-deploy.sh <zacblog-server.jar> <web-dist.tgz>
#   备份数据库 → 发布前端 → 启动新后端 + 健康检查；健康检查失败时 jar 和前端都回滚。
#   备份失败时直接中止，前后端都不动。
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=zacblog-common.sh
. "$SCRIPT_DIR/zacblog-common.sh"

jar=${1:?用法：zacblog-deploy.sh <zacblog-server.jar> <web-dist.tgz>}
dist=${2:?用法：zacblog-deploy.sh <zacblog-server.jar> <web-dist.tgz>}
[ -f "$jar" ] || die "后端产物不存在：$jar"
[ -f "$dist" ] || die "前端产物不存在：$dist"
require_env_file

log "== 1/3 备份数据库"
bash "$SCRIPT_DIR/zacblog-backup.sh" || die "备份失败，中止部署（前后端都没有改动）"

log "== 2/3 发布前端"
bash "$SCRIPT_DIR/zacblog-frontend.sh" "$dist" || die "前端发布失败，中止部署（后端没有改动）"

log "== 3/3 启动新后端并做健康检查"
rc=0
bash "$SCRIPT_DIR/zacblog-start.sh" "$jar" || rc=$?
case "$rc" in
  0) log "部署完成" ;;
  3)
    log "后端前置检查失败，恢复前端"
    bash "$SCRIPT_DIR/zacblog-rollback.sh" --frontend-only || true
    exit 1
    ;;
  1) die "部署失败，已回滚到部署前的版本" ;;
  *) die "部署失败，回滚也失败（start 退出码 ${rc}），需要人工处理" ;;
esac
