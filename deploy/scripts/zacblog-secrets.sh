#!/usr/bin/env bash
# 生成环境变量文件 / 轮换数据库密码 / 统计内容数量：zacblog-secrets.sh write-env|rotate|counts
# 由 .github/workflows/server-secrets.yml（write-env、rotate）和 db-counts.yml（counts）调用。密钥只从 stdin 读（每行一个 KEY=VALUE），
# 不出现在命令行参数里，也不打印；只有 mysql 客户端进程通过 MYSQL_PWD 环境变量拿到密码。
#
# stdin 需要的键：DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD_CURRENT DB_PASSWORD_NEW JWT_SECRET_NEW
#
# write-env：
#   用 DB_PASSWORD_CURRENT 登录 MySQL 确认能连上，再写 $ENV_FILE（600）。
#   如果 CURRENT 已经登录不了、NEW 可以（说明已经轮换过），就写 NEW，这样轮换后重跑也不会把站点弄坏。
#   两个都登录不了就不写。不重启服务（文件内容有变化且服务在运行时会提示）。
# rotate（只能在切换完成后、zacblog-server 正在运行时执行）：
#   1. 用 CURRENT 登录，ALTER USER CURRENT_USER() IDENTIFIED BY NEW
#   2. 确认 NEW 能登录，把 NEW 写进 $ENV_FILE（原文件先备份）
#   3. restart zacblog-server + 健康检查；确认 CURRENT 已经登录不了
#   4. 1–3 任何一步失败：密码 ALTER 回 CURRENT、恢复环境变量文件、再重启并做健康检查
#   CURRENT 登录不了而 NEW 可以时视为已经轮换过：只保证环境变量文件用的是 NEW，不再 ALTER。
# counts（只读）：用 CURRENT（登录不了就用 NEW）统计文章、评论、留言、公告的数量，切换前后各跑一次对比。
#
# 退出码：0 成功；1 失败且已恢复原状；2 失败且恢复也失败（需要人工处理）；
#         3 前置检查失败，什么都没改；4 轮换成功但旧密码仍能登录（可能有多条同名账号）
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=zacblog-common.sh
. "$SCRIPT_DIR/zacblog-common.sh"

: "${MYSQL_BIN:=mysql}"
: "${UPLOAD_PATH_VALUE:=$ZACBLOG_HOME/zaaac/uploadPath}"
# 与 application.yml 的默认 DB_URL 参数一致
: "${DB_URL_PARAMS:=useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}"

pre() { # 前置检查失败：什么都没改
  log "ERROR: $*" >&2
  exit 3
}

mode=${1:-}
case "$mode" in
  write-env | rotate | counts) ;;
  *) pre "用法：zacblog-secrets.sh write-env|rotate|counts（密钥从 stdin 读）" ;;
esac

# ---- 读 stdin
DB_HOST='' DB_PORT='' DB_NAME='' DB_USERNAME='' DB_PASSWORD_CURRENT='' DB_PASSWORD_NEW='' JWT_SECRET_NEW=''
while IFS= read -r line || [ -n "$line" ]; do
  line=${line%$'\r'}
  [ -n "$line" ] || continue
  key=${line%%=*}
  val=${line#*=}
  case "$key" in
    DB_HOST | DB_PORT | DB_NAME | DB_USERNAME | DB_PASSWORD_CURRENT | DB_PASSWORD_NEW | JWT_SECRET_NEW)
      printf -v "$key" '%s' "$val"
      ;;
    *) pre "stdin 里有未知的键：$key" ;;
  esac
done

for key in DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD_CURRENT; do
  [ -n "${!key}" ] || pre "缺少 $key"
done
[ "$mode" = counts ] || [ -n "$JWT_SECRET_NEW" ] || pre "缺少 JWT_SECRET_NEW"
[ "$mode" = write-env ] || [ -n "$DB_PASSWORD_NEW" ] || pre "缺少 DB_PASSWORD_NEW"
[[ "$DB_HOST" =~ ^[A-Za-z0-9._-]+$ ]] || pre "DB_HOST 格式不对（只允许字母、数字和 . _ -）"
[[ "$DB_PORT" =~ ^[0-9]{1,5}$ ]] || pre "DB_PORT 必须是数字"
[[ "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || pre "DB_NAME 格式不对（只允许字母、数字和 _）"
[[ "$DB_USERNAME" =~ ^[A-Za-z0-9_.-]+$ ]] || pre "DB_USERNAME 格式不对"

# 写进环境变量文件和 SQL 字符串的值：不能有换行和反斜杠（SQL 里反斜杠的含义取决于 sql_mode），
# 也不能首尾是成对的引号（启动器会去掉它们）。报错只说是哪个键，不打印值。
check_value() {
  local key=$1 v=${!1}
  case "$v" in
    *$'\n'* | *$'\r'* | *\\*) pre "$key 里不能有换行或反斜杠" ;;
    \"*\" | \'*\') pre "$key 不能首尾都是引号" ;;
  esac
}
for key in DB_PASSWORD_CURRENT DB_PASSWORD_NEW JWT_SECRET_NEW; do check_value "$key"; done
[ "$mode" = counts ] || [ "${#JWT_SECRET_NEW}" -ge 32 ] || pre "JWT_SECRET_NEW 太短（至少 32 个字符，建议 openssl rand -hex 32）"
if [ "$mode" = rotate ] && [ "$DB_PASSWORD_NEW" = "$DB_PASSWORD_CURRENT" ]; then
  pre "DB_PASSWORD_NEW 和 DB_PASSWORD_CURRENT 相同"
fi

# ---- MySQL：密码只通过 MYSQL_PWD 交给客户端，SQL 走 stdin
# stderr 丢掉：mysql 的语法错误会回显 SQL 片段（其中有新密码）
mysql_as() { # mysql_as <密码>，SQL 从 stdin 读
  MYSQL_PWD="$1" "$MYSQL_BIN" --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" --protocol=TCP \
    --batch --skip-column-names --connect-timeout=10 2>/dev/null
}
can_login() { printf 'SELECT 1;\n' | mysql_as "$1" >/dev/null; }
sql_quote() { # 单引号字符串字面量：' 写成 ''（与 sql_mode 无关）
  printf "'%s'" "${1//\'/\'\'}"
}
# set_password <用来登录的密码> <新密码>
set_password() {
  printf 'ALTER USER CURRENT_USER() IDENTIFIED BY %s;\n' "$(sql_quote "$2")" | mysql_as "$1" >/dev/null
}

# ---- 环境变量文件
env_content() { # env_content <数据库密码>
  cat <<ENV
# 由 .github/workflows/server-secrets.yml 生成，不要手动编辑（下次运行会被覆盖）
SERVER_ADDRESS=127.0.0.1
DB_URL=jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?$DB_URL_PARAMS
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$1
JWT_SECRET=$JWT_SECRET_NEW
UPLOAD_PATH=$UPLOAD_PATH_VALUE
ENV
}
# write_env <数据库密码>：先在同目录写临时文件（600）再 rename；内容有变化时 ENV_CHANGED=1
ENV_CHANGED=0
write_env() {
  local tmp="$ENV_FILE.tmp.$$"
  ENV_CHANGED=0
  mkdir -p "$(dirname "$ENV_FILE")" || die "无法创建 $(dirname "$ENV_FILE")"
  if ! (umask 077 && env_content "$1" > "$tmp") || ! chmod 600 "$tmp"; then
    rm -f "$tmp"
    die "无法写入 $tmp"
  fi
  if [ -f "$ENV_FILE" ] && cmp -s "$tmp" "$ENV_FILE"; then
    rm -f "$tmp"
    chmod 600 "$ENV_FILE" || die "无法修改 $ENV_FILE 的权限"
    log "环境变量文件内容没有变化：${ENV_FILE}（权限 $(file_mode "$ENV_FILE")）"
    return 0
  fi
  mv -f "$tmp" "$ENV_FILE" || die "无法替换 $ENV_FILE"
  ENV_CHANGED=1
  log "已写入 ${ENV_FILE}（权限 $(file_mode "$ENV_FILE")，键：$(grep -v '^#' "$ENV_FILE" | cut -d= -f1 | tr '\n' ' ')）"
}

restart_app() {
  log "重启 $APP_SERVICE"
  svc restart "$APP_SERVICE" && wait_health "$HEALTH_URL" strict
}

log "数据库 ${DB_NAME}@${DB_HOST}:${DB_PORT}，用户 ${DB_USERNAME}"

# ================================================================ counts
if [ "$mode" = counts ]; then
  if can_login "$DB_PASSWORD_CURRENT"; then
    pw=$DB_PASSWORD_CURRENT
  elif [ -n "$DB_PASSWORD_NEW" ] && can_login "$DB_PASSWORD_NEW"; then
    pw=$DB_PASSWORD_NEW
  else
    pre "DB_PASSWORD_CURRENT 和 DB_PASSWORD_NEW 都不能登录 MySQL"
  fi
  # cms_blog、sys_notice 没有 del_flag（旧版是物理删除）；评论、留言按 del_flag 统计未删除的
  db="\`$DB_NAME\`"
  out=$(mysql_as "$pw" <<SQL
SELECT 'articles_total', COUNT(*) FROM $db.cms_blog;
SELECT 'articles_published', COUNT(*) FROM $db.cms_blog WHERE status = '1';
SELECT 'comments', COUNT(*) FROM $db.cms_comment WHERE del_flag = '0';
SELECT 'messages', COUNT(*) FROM $db.cms_message WHERE del_flag = '0';
SELECT 'notices_total', COUNT(*) FROM $db.sys_notice;
SELECT 'notices_normal', COUNT(*) FROM $db.sys_notice WHERE status = '0';
SQL
  ) || pre "统计 SQL 执行失败"
  log "内容数量："
  printf '%s\n' "$out" | while IFS=$'\t' read -r name n; do printf 'COUNT %-20s %s\n' "$name" "$n"; done
  exit 0
fi

# ================================================================ write-env
if [ "$mode" = write-env ]; then
  if can_login "$DB_PASSWORD_CURRENT"; then
    log "DB_PASSWORD_CURRENT 可以登录 MySQL"
    pw=$DB_PASSWORD_CURRENT
  elif [ -n "$DB_PASSWORD_NEW" ] && can_login "$DB_PASSWORD_NEW"; then
    log "DB_PASSWORD_CURRENT 登录失败、DB_PASSWORD_NEW 可以：数据库密码已经轮换过，环境变量文件使用 DB_PASSWORD_NEW"
    pw=$DB_PASSWORD_NEW
  else
    pre "DB_PASSWORD_CURRENT 和 DB_PASSWORD_NEW 都不能登录 MySQL，没有写环境变量文件"
  fi
  write_env "$pw"
  if [ "$ENV_CHANGED" = 1 ] && svc_active "$APP_SERVICE"; then
    log "注意：$APP_SERVICE 正在运行，新的环境变量要重启后才生效（下次部署或 rotate 会重启）"
  fi
  log "完成"
  exit 0
fi

# ================================================================ rotate
require_env_file_or_pre() { (require_env_file) || pre "环境变量文件不可用，先运行 write-env"; }

if ! can_login "$DB_PASSWORD_CURRENT"; then
  if can_login "$DB_PASSWORD_NEW"; then
    log "DB_PASSWORD_CURRENT 登录失败、DB_PASSWORD_NEW 可以：数据库密码已经轮换过，不再 ALTER"
    require_env_file_or_pre
    write_env "$DB_PASSWORD_NEW"
    if [ "$ENV_CHANGED" = 1 ]; then
      restart_app || die "环境变量文件已改为新密码，但重启后健康检查失败，需要人工处理"
    fi
    log "完成（已轮换过）"
    exit 0
  fi
  pre "DB_PASSWORD_CURRENT 和 DB_PASSWORD_NEW 都不能登录 MySQL，什么都没改"
fi

# 旧后端用的是自己 config/ 里的旧密码：切换完成前轮换会把它弄挂
if svc_active "$LEGACY_SERVICE"; then
  pre "旧后端 $LEGACY_SERVICE 还在运行：切换完成后才能轮换数据库密码"
fi
svc_active "$APP_SERVICE" || pre "$APP_SERVICE 没有运行：切换完成后才能轮换数据库密码"
require_env_file_or_pre
wait_health "$HEALTH_URL" strict || pre "轮换前健康检查就不通过，什么都没改"

backup="$ENV_FILE.before-rotate"
cp -p "$ENV_FILE" "$backup"
chmod 600 "$backup"

recover() {
  local rc=$?
  trap - EXIT
  [ "$rc" -ne 0 ] || exit 0
  log "轮换失败，开始恢复"
  # ALTER 没成功、密码仍是原值：环境变量文件和服务都还没动，不用重启
  if [ "$altered" = 0 ] && can_login "$DB_PASSWORD_CURRENT"; then
    rm -f "$backup"
    log "数据库密码、环境变量文件和服务都没有改动"
    exit 1
  fi
  local ok=1
  if can_login "$DB_PASSWORD_CURRENT"; then
    log "数据库密码仍是原值"
  elif can_login "$DB_PASSWORD_NEW" && set_password "$DB_PASSWORD_NEW" "$DB_PASSWORD_CURRENT" &&
    can_login "$DB_PASSWORD_CURRENT"; then
    log "数据库密码已改回原值"
  else
    log "ERROR: 数据库密码没能改回原值" >&2
    ok=0
  fi
  if mv -f "$backup" "$ENV_FILE"; then
    log "环境变量文件已恢复"
  else
    log "ERROR: 环境变量文件恢复失败（备份在 ${backup}）" >&2
    ok=0
  fi
  if restart_app; then
    log "已用原密码重启，健康检查通过"
  else
    log "ERROR: 恢复后健康检查失败" >&2
    ok=0
  fi
  if [ "$ok" = 1 ]; then
    log "已恢复原状"
    exit 1
  fi
  log "ERROR: 恢复不完整，需要人工处理" >&2
  exit 2
}
altered=0
trap recover EXIT

log "== 1/3 ALTER USER CURRENT_USER()（新密码来自 DB_PASSWORD_NEW）"
set_password "$DB_PASSWORD_CURRENT" "$DB_PASSWORD_NEW" || die "ALTER USER 失败（可能是密码策略不允许）"
altered=1
can_login "$DB_PASSWORD_NEW" || die "改密码后用 DB_PASSWORD_NEW 登录失败"
log "DB_PASSWORD_NEW 可以登录"

log "== 2/3 把新密码写进环境变量文件"
write_env "$DB_PASSWORD_NEW"

log "== 3/3 重启并做健康检查"
restart_app || die "用新密码重启后健康检查失败"

trap - EXIT
rm -f "$backup"
if can_login "$DB_PASSWORD_CURRENT"; then
  log "ERROR: 轮换已生效，但旧密码仍能登录 MySQL（可能存在多条 $DB_USERNAME@<host> 账号），需要人工检查" >&2
  exit 4
fi
log "旧密码已不能登录 MySQL"
log "轮换完成。之后再运行 write-env 也会自动使用 DB_PASSWORD_NEW"
