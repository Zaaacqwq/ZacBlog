#!/usr/bin/env bash
# 部署前备份数据库：mysqldump → $BACKUP_DIR/zacblog-<时间戳>.sql.gz，保留最近 BACKUP_KEEP 份。
# 任何一步失败都以非 0 退出，调用方（zacblog-deploy.sh）据此中止部署。
#
# 连接信息来自环境变量文件里的 DB_URL / DB_USERNAME / DB_PASSWORD；
# 主机、端口、库名也可以用 DB_HOST / DB_PORT / DB_NAME 单独覆盖。
# 密码只通过 MYSQL_PWD 环境变量交给 mysqldump，不出现在命令行参数里。
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=zacblog-common.sh
. "$SCRIPT_DIR/zacblog-common.sh"

require_env_file
DB_URL=$(env_file_get DB_URL) || die "环境变量文件里没有 DB_URL"
DB_USERNAME=$(env_file_get DB_USERNAME) || die "环境变量文件里没有 DB_USERNAME"
DB_PASSWORD=$(env_file_get DB_PASSWORD) || true

# jdbc:mysql://host[:port]/db?params
url=${DB_URL#jdbc:mysql://}
[ "$url" != "$DB_URL" ] || die "DB_URL 不是 jdbc:mysql:// 开头"
hostport=${url%%/*}
dbpart=${url#*/}
: "${DB_HOST:=${hostport%%:*}}"
if [ "$hostport" != "${hostport#*:}" ]; then
  : "${DB_PORT:=${hostport#*:}}"
fi
: "${DB_PORT:=3306}"
: "${DB_NAME:=${dbpart%%\?*}}"
[ -n "$DB_HOST" ] && [ -n "$DB_NAME" ] || die "无法从 DB_URL 解析出主机或库名"

mkdir -p "$BACKUP_DIR"
umask 077
stamp=$(date '+%Y%m%d-%H%M%S')
target="$BACKUP_DIR/zacblog-$stamp.sql.gz"
tmp="$target.partial"
trap 'rm -f "$tmp"' EXIT

log "备份数据库 $DB_NAME@$DB_HOST:$DB_PORT → $target"
MYSQL_PWD="$DB_PASSWORD" "$MYSQLDUMP_BIN" \
  --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" \
  --single-transaction --quick --routines --triggers --no-tablespaces \
  "$DB_NAME" | gzip -c > "$tmp"

gzip -t "$tmp" || die "备份文件损坏"
[ "$(gzip -dc "$tmp" | head -c 1 | wc -c)" -gt 0 ] || die "备份内容为空"
mv -f "$tmp" "$target"
trap - EXIT
log "备份完成：$(wc -c < "$target" | tr -d ' ') 字节"

# 只保留最近 BACKUP_KEEP 份（文件名里的时间戳按字典序就是时间顺序）
old=$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'zacblog-*.sql.gz' | sort -r | tail -n +"$((BACKUP_KEEP + 1))")
if [ -n "$old" ]; then
  printf '%s\n' "$old" | while IFS= read -r f; do
    log "删除旧备份 $f"
    rm -f -- "$f"
  done
fi
