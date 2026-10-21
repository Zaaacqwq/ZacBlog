#!/usr/bin/env bash
# 发布前端：zacblog-frontend.sh <web-dist.tgz>
#   1. 解压到与 $FRONTEND_DIR 同一目录下的临时目录（保证 mv 不跨文件系统）
#   2. 当前 $FRONTEND_DIR 改名为 ${FRONTEND_PREV}（部署前备份，回滚用）
#   3. 临时目录改名为 $FRONTEND_DIR
# 上传目录（UPLOAD_PATH）不在前端目录里，这里不会碰到它。
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=zacblog-common.sh
. "$SCRIPT_DIR/zacblog-common.sh"

dist=${1:?用法：zacblog-frontend.sh <web-dist.tgz>}
[ -f "$dist" ] || die "前端产物不存在：$dist"

parent=$(dirname "$FRONTEND_DIR")
mkdir -p "$parent"
umask 022
tmp=$(mktemp -d "$parent/.frontend.new.XXXXXX")
trap 'rm -rf "$tmp"' EXIT

tar -xzf "$dist" -C "$tmp"
[ -f "$tmp/index.html" ] || die "前端产物里没有 index.html：$dist"
chmod 755 "$tmp"

rm -rf "${FRONTEND_PREV:?}"
if [ -e "$FRONTEND_DIR" ]; then
  mv "$FRONTEND_DIR" "$FRONTEND_PREV"
  log "已备份当前前端 → $FRONTEND_PREV"
fi
if ! mv "$tmp" "$FRONTEND_DIR"; then
  [ ! -e "$FRONTEND_PREV" ] || mv "$FRONTEND_PREV" "$FRONTEND_DIR"
  die "替换前端目录失败，已恢复原目录"
fi
trap - EXIT
log "前端已发布 → $FRONTEND_DIR"
