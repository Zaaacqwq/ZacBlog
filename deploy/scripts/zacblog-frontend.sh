#!/usr/bin/env bash
# 发布前端：zacblog-frontend.sh <web-dist.tgz> [版本后缀]
#   1. 解压到 web/releases/ 下的临时目录，检查有 index.html，再改名为 web/releases/<时间戳>[-后缀]
#   2. 记下 web/current 当前指向的 release（$WEB_STATE_FILE，回滚用）
#   3. 原子替换 web/current 软链接（/home/ZacBlog/frontend → web/current 由 root 所有，nginx 跟随软链接）
#   4. 只保留最近 WEB_RELEASES_KEEP 个 release；当前版本、回滚目标和 bootstrap 迁过来的 legacy 永远保留
# 替换软链接之前的任何失败都不会影响线上前端。上传目录不在 web/ 里，这里不会碰到它。
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=zacblog-common.sh
. "$SCRIPT_DIR/zacblog-common.sh"

dist=${1:?用法：zacblog-frontend.sh <web-dist.tgz> [版本后缀]}
[ -f "$dist" ] || die "前端产物不存在：$dist"
version=$(date '+%Y%m%d-%H%M%S')${2:+-$2}
release="$WEB_RELEASES_DIR/$version"
[ ! -e "$release" ] || die "前端版本已存在：$release"

mkdir -p "$WEB_RELEASES_DIR"
umask 022
tmp=$(mktemp -d "$WEB_RELEASES_DIR/.new.XXXXXX")
trap 'rm -rf "$tmp"' EXIT

tar -xzf "$dist" -C "$tmp"
[ -f "$tmp/index.html" ] || die "前端产物里没有 index.html：$dist"
chmod 755 "$tmp"
mv "$tmp" "$release"
trap - EXIT

prev=''
if [ -L "$WEB_CURRENT" ]; then
  prev=$(readlink "$WEB_CURRENT")
  # 相对路径（bootstrap 建的是 releases/legacy）统一成绝对路径
  case "$prev" in /*) ;; *) prev="$WEB_DIR/$prev" ;; esac
fi
write_state "$WEB_STATE_FILE" "${prev:-none}" || die "无法写入前端回滚记录 $WEB_STATE_FILE"
log "前端回滚目标：${prev:-none}"

swap_link "$WEB_CURRENT" "$release"
log "前端已发布：$release"

find "$WEB_RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d ! -name legacy ! -name '.*' | sort -r |
  tail -n +"$((WEB_RELEASES_KEEP + 1))" | while IFS= read -r d; do
  if [ "$d" != "$release" ] && [ "$d" != "$prev" ]; then
    log "删除旧前端版本 $d"
    rm -rf -- "$d"
  fi
done || log "清理旧前端版本失败（不影响本次发布）"
