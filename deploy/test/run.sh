#!/usr/bin/env bash
# deploy/scripts 的本地测试：在临时目录里模拟 /home/ZacBlog，不需要 Docker、root、MySQL。
#   - systemctl / sudo / mysqldump 换成 stubs/ 里的桩（sudo 桩只放行 sudoers 允许的命令）
#   - java 换成桩：启动 fake_server.py，按“jar”文件内容返回 UP 或 DOWN
# 用法：bash deploy/test/run.sh
set -euo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO=$(cd "$HERE/../.." && pwd)
SCRIPTS="$REPO/deploy/scripts"
STUBS="$HERE/stubs"
# 明显是假的测试值
PW='test-only-not-a-real-secret-db-password'

if command -v shellcheck >/dev/null 2>&1; then
  echo "== shellcheck"
  shellcheck -x -S warning "$SCRIPTS"/*.sh "$REPO/deploy/bootstrap-root.sh" "$HERE/run.sh" "$STUBS/java" "$STUBS/sudo" "$STUBS/systemctl" "$STUBS/mysqldump" "$STUBS/mysql"
else
  echo "== shellcheck 未安装，跳过"
fi
for f in "$SCRIPTS"/*.sh "$REPO/deploy/bootstrap-root.sh"; do bash -n "$f"; done

WORK=$(mktemp -d "${TMPDIR:-/tmp}/zacblog-deploy-test.XXXXXX")
WORK=$(cd "$WORK" && pwd -P)
export PATH="$STUBS:$PATH"

stop_all() {
  local f
  for f in "$WORK"/*/systemd/*.pid; do
    [ -e "$f" ] || continue
    kill "$(cat "$f")" 2>/dev/null || true
    rm -f "$f"
  done
  pkill -f "$WORK" 2>/dev/null || true
}
cleanup() {
  stop_all
  chmod -R u+w "$WORK" 2>/dev/null || true
  rm -rf "$WORK"
}
trap cleanup EXIT

FAILS=0
CASE=''
pass() { echo "  ok   $*"; }
fail() {
  echo "  FAIL $*"
  FAILS=$((FAILS + 1))
}
check() { # check <描述> <命令...>
  local desc=$1
  shift
  if "$@"; then pass "$desc"; else fail "$desc"; fi
}
eq() { [ "$1" = "$2" ] || { echo "       期望 '$2'，实际 '$1'" >&2; return 1; }; }

free_port() { python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1])'; }

jar_file() { # jar_file <路径> <UP|DOWN> <版本>
  printf 'status=%s\nversion=%s\n' "$2" "$3" > "$1"
}

# 建一套新的模拟服务器，旧后端 zacblog-backend 在运行
new_server() {
  CASE=$1
  echo "== $CASE"
  local root="$WORK/$CASE"
  H="$root/home"
  export ZACBLOG_HOME="$H"
  export SYSTEMD_STUB_DIR="$root/systemd" STUB_LOG_DIR="$root/stublogs"
  export STUB_PORT
  STUB_PORT=$(free_port)
  export HEALTH_URL="http://127.0.0.1:$STUB_PORT/actuator/health"
  export LEGACY_HEALTH_URL="http://127.0.0.1:$STUB_PORT/"
  export HEALTH_TIMEOUT=8 HEALTH_INTERVAL=1 BACKUP_KEEP=3 RELEASES_KEEP=2 WEB_RELEASES_KEEP=2
  export EXPECTED_DB_PASSWORD="$PW"
  unset MYSQLDUMP_STUB_FAIL MYSQL_STUB_ALTER_FAIL
  mkdir -p "$H"/{backend/config,server/releases,web/releases/legacy,backups,scripts,incoming,zaaac/uploadPath/2025} "$STUB_LOG_DIR"
  jar_file "$H/backend/zaaac-admin.jar" UP legacy
  echo 'legacy-config' > "$H/backend/config/application.yml"
  # bootstrap-root.sh 迁移后的样子：frontend（root）→ web/current → releases/legacy
  echo 'legacy-frontend' > "$H/web/releases/legacy/index.html"
  ln -s releases/legacy "$H/web/current"
  ln -s "$H/web/current" "$H/frontend"
  echo 'uploaded-image' > "$H/zaaac/uploadPath/2025/a.png"
  cat > "$H/server/zacblog.env" <<ENV
DB_URL=jdbc:mysql://127.0.0.1:3306/zaaac_blog?useUnicode=true&characterEncoding=utf8
DB_USERNAME=zacblog
DB_PASSWORD=$PW
JWT_SECRET=test-only-not-a-real-secret-jwt-0123456789abcdef0123456789abcdef
UPLOAD_PATH=$H/zaaac/uploadPath
ENV
  chmod 600 "$H/server/zacblog.env"
  # 5 份旧备份，用来验证只保留 BACKUP_KEEP 份
  for i in 1 2 3 4 5; do echo old | gzip > "$H/backups/zacblog-20200101-00000$i.sql.gz"; done
  cp "$SCRIPTS"/*.sh "$H/scripts/"
  systemctl start zacblog-backend
  wait_up legacy
  ENV_SUM=$(cksum < "$H/server/zacblog.env")
  # /home/ZacBlog 本身和 backend/ 归 root：zacblog 不能写，这里用只读模拟
  chmod 555 "$H" "$H/backend" "$H/backend/config"
  HOME_LIST=$(ls -A "$H")
}

version() { curl -s --max-time 2 "http://127.0.0.1:$STUB_PORT/version" || true; }
wait_up() {
  local i
  for i in $(seq 50); do
    [ "$(version)" = "$1" ] && return 0
    sleep 0.1
  done
  echo "  后端 $1 没起来" >&2
  return 1
}

# deploy <jar 名> <UP|DOWN> <版本> <前端内容>：上传产物并执行一次完整部署，退出码放在 RC
deploy() {
  local jar="$H/incoming/$1"
  jar_file "$jar" "$2" "$3"
  local dist
  dist=$(mktemp -d "$WORK/dist.XXXXXX")
  echo "$4" > "$dist/index.html"
  mkdir -p "$dist/assets" && echo 'js' > "$dist/assets/app.js"
  tar -czf "$H/incoming/web-dist.tgz" -C "$dist" .
  rm -rf "$dist"
  RC=0
  bash "$H/scripts/zacblog-deploy.sh" "$jar" "$H/incoming/web-dist.tgz" > "$WORK/$CASE/deploy-$3.out" 2>&1 || RC=$?
  sed 's/^/       | /' "$WORK/$CASE/deploy-$3.out"
}

active() { systemctl is-active --quiet "$1"; }
inactive() { ! systemctl is-active --quiet "$1"; }
frontend_is() { [ -L "$H/frontend" ] && [ "$(cat "$H/frontend/index.html")" = "$1" ]; }
web_current() { readlink "$H/web/current"; }
current_has() { [ -L "$H/server/current.jar" ] && grep -q "version=$1" "$H/server/current.jar"; }
backups() { find "$H/backups" -name 'zacblog-*.sql.gz' | wc -l | tr -d ' '; }

common_checks() {
  check "上传目录未被改动" eq "$(cat "$H/zaaac/uploadPath/2025/a.png")" uploaded-image
  check "环境变量文件未被改动" eq "$(cksum < "$H/server/zacblog.env")" "$ENV_SUM"
  check "环境变量文件权限仍是 600" eq "$(stat -c '%a' "$H/server/zacblog.env" 2>/dev/null || stat -f '%Lp' "$H/server/zacblog.env")" 600
  check "旧后端外置配置未被改动" eq "$(cat "$H/backend/config/application.yml")" legacy-config
  check "/home/ZacBlog 下没有新增或删除条目（frontend 仍是软链接）" eq "$(ls -A "$H")" "$HOME_LIST"
  check "sudo 只用了 sudoers 允许的命令" bash -c "! grep -q '^DENIED' '$STUB_LOG_DIR/sudo.log' 2>/dev/null"
  # 密码只能出现在 zacblog.env 里：命令行参数（java/mysqldump/sudo/systemctl 桩都记录了）、日志、部署输出都不能有
  check "数据库密码没有出现在任何参数和日志里" bash -c "! grep -rlF -- '$PW' '$WORK/$CASE' | grep -v '/home/server/zacblog.env\$'"
}

# ---------------------------------------------------------------- 场景 1
new_server switch-ok
deploy zacblog-server-v2.jar UP v2 v2-frontend
check "部署成功（退出码 0）" eq "$RC" 0
check "8080 上是新版本 v2" eq "$(version)" v2
check "zacblog-server 在运行" active zacblog-server
check "zacblog-backend 已停止" inactive zacblog-backend
check "current.jar 是指向 releases/ 的软链接，内容是 v2" current_has v2
check "前端已更新为 v2" frontend_is v2-frontend
check "web/current 指向新的 web release" bash -c "readlink '$H/web/current' | grep -q '/web/releases/[0-9-]*-zacblog-server-v2\$'"
check "前端回滚记录指向 releases/legacy" eq "$(cat "$H/web/.rollback-target")" "$H/web/releases/legacy"
check "新备份已生成，只保留 3 份" eq "$(backups)" 3
check "最新备份是这次的 mysqldump 输出" bash -c "ls '$H'/backups/zacblog-2*.sql.gz | sort | tail -1 | xargs gzip -dc | grep -q 'fake dump of zaaac_blog'"
check "mysqldump 通过 MYSQL_PWD 拿到了密码" grep -q 'mysql_pwd=ok' "$STUB_LOG_DIR/mysqldump-env.log"
check "mysqldump 参数里的库/主机/用户从 DB_URL 解析" grep -q -- '--host=127.0.0.1 --port=3306 --user=zacblog .* zaaac_blog$' "$STUB_LOG_DIR/mysqldump-args.log"
check "新后端拿到了 zacblog.env 里的 DB_PASSWORD" grep -q 'current.jar db_password_in_env=yes' "$STUB_LOG_DIR/java-env.log"
check "新后端在 server/ 下启动、日志写 server/app.log" grep -q 'fake backend v2' "$H/server/app.log"
check "回滚目标记录为旧后端" eq "$(cat "$H/server/.rollback-target")" legacy
check "改服务状态都通过 sudo -n" grep -q '^-n systemctl restart zacblog-server$' "$STUB_LOG_DIR/sudo.log"
common_checks

# ---------------------------------------------------------------- 场景 2（接着场景 1）
CASE=switch-ok
echo "== app-rollback（v2 运行中，发布健康检查失败的 v3）"
deploy zacblog-server-v3.jar DOWN v3 v3-frontend
check "部署失败（退出码非 0）" test "$RC" -ne 0
check "输出里有回滚完成" grep -q '回滚完成' "$WORK/$CASE/deploy-v3.out"
check "8080 上恢复为 v2" eq "$(version)" v2
check "current.jar 指回 v2" current_has v2
check "zacblog-server 在运行" active zacblog-server
check "zacblog-backend 仍然停止" inactive zacblog-backend
check "前端恢复为 v2" frontend_is v2-frontend
check "前端回滚记录已用掉" test ! -e "$H/web/.rollback-target"
common_checks

echo "== releases-cleanup（再发布 v4、v5，RELEASES_KEEP=2）"
deploy zacblog-server-v4.jar UP v4 v4-frontend
deploy zacblog-server-v5.jar UP v5 v5-frontend
check "v5 部署成功" eq "$RC" 0
check "8080 上是 v5" eq "$(version)" v5
check "releases/ 只保留 2 个版本（v5 和回滚目标 v4）" eq "$(ls "$H/server/releases" | sed 's/^[0-9-]*-//' | sort | tr '\n' ' ')" "zacblog-server-v4.jar zacblog-server-v5.jar "
check "web/releases 只保留 legacy 和 2 个版本（v5 和回滚目标 v4）" eq "$(ls "$H/web/releases" | sed 's/^[0-9-]*-//' | sort | tr '\n' ' ')" "legacy zacblog-server-v4 zacblog-server-v5 "
common_checks
stop_all

# ---------------------------------------------------------------- 场景 3
new_server switch-fail
deploy zacblog-server-v2.jar DOWN v2 v2-frontend
check "部署失败（退出码非 0）" test "$RC" -ne 0
check "备份成功、新版本确实启动过" grep -q '重启 zacblog-server' "$WORK/$CASE/deploy-v2.out"
check "输出里有回滚完成" grep -q '回滚完成' "$WORK/$CASE/deploy-v2.out"
check "8080 上恢复为旧后端" eq "$(version)" legacy
check "zacblog-backend 重新启动" active zacblog-backend
check "zacblog-server 已停止" inactive zacblog-server
check "current.jar 已删除" test ! -e "$H/server/current.jar" -a ! -L "$H/server/current.jar"
check "前端恢复为部署前的版本" frontend_is legacy-frontend
check "web/current 指回 releases/legacy" eq "$(web_current)" "$H/web/releases/legacy"
check "前端回滚记录已用掉" test ! -e "$H/web/.rollback-target"
check "旧后端没有拿到新后端的环境变量" bash -c "! grep -q 'zaaac-admin.jar db_password_in_env=yes' '$STUB_LOG_DIR/java-env.log'"
common_checks
stop_all

# ---------------------------------------------------------------- 场景 4
new_server backup-fail
LEGACY_PID=$(cat "$SYSTEMD_STUB_DIR/zacblog-backend.pid")
export MYSQLDUMP_STUB_FAIL=1
deploy zacblog-server-v2.jar UP v2 v2-frontend
check "部署失败（退出码非 0）" test "$RC" -ne 0
check "输出里说明备份失败中止" grep -q '备份失败，中止部署' "$WORK/$CASE/deploy-v2.out"
check "旧后端没有被重启" eq "$(cat "$SYSTEMD_STUB_DIR/zacblog-backend.pid")" "$LEGACY_PID"
check "8080 上仍是旧后端" eq "$(version)" legacy
check "没有执行任何 sudo 命令" test ! -s "$STUB_LOG_DIR/sudo.log"
check "前端没有改动" frontend_is legacy-frontend
check "web/current 仍指向 releases/legacy" eq "$(web_current)" releases/legacy
check "没有新增前端 release" eq "$(ls "$H/web/releases")" legacy
check "没有新增 release" test -z "$(ls "$H/server/releases")"
check "没有留下新的或半截的备份" eq "$(find "$H/backups" -type f | wc -l | tr -d ' ')" 5
common_checks
stop_all

# ---------------------------------------------------------------- 场景 5
new_server env-file-missing
mv "$H/server/zacblog.env" "$WORK/env.bak"
deploy zacblog-server-v2.jar UP v2 v2-frontend
check "环境变量文件不存在时报错退出" test "$RC" -ne 0
check "报错信息指出环境变量文件" grep -q '环境变量文件不存在' "$WORK/$CASE/deploy-v2.out"
check "前端没有改动" frontend_is legacy-frontend
mv "$WORK/env.bak" "$H/server/zacblog.env"
chmod 644 "$H/server/zacblog.env"
deploy zacblog-server-v2.jar UP v2 v2-frontend
check "环境变量文件权限不是 600 时报错退出" test "$RC" -ne 0
check "8080 上仍是旧后端" eq "$(version)" legacy
chmod 600 "$H/server/zacblog.env"
stop_all

# ---------------------------------------------------------------- 场景 6
new_server state-file-unwritable
LEGACY_PID=$(cat "$SYSTEMD_STUB_DIR/zacblog-backend.pid")
echo 'app /previous/release.jar' > "$H/server/.rollback-target"
# 让回滚记录的临时文件写不进去（server/ 只读），模拟权限或磁盘问题
chmod 555 "$H/server"
deploy zacblog-server-v2.jar UP v2 v2-frontend
chmod 755 "$H/server"
check "部署失败（退出码非 0）" test "$RC" -ne 0
check "输出说明后端前置检查失败、前端已恢复" grep -q '前端已恢复，后端没有改动' "$WORK/$CASE/deploy-v2.out"
check "没有误报「已回滚」" bash -c "! grep -q '已回滚到部署前的版本' '$WORK/$CASE/deploy-v2.out'"
check "前端恢复为部署前的版本" frontend_is legacy-frontend
check "前端回滚记录已用掉" test ! -e "$H/web/.rollback-target"
check "旧后端没有被重启" eq "$(cat "$SYSTEMD_STUB_DIR/zacblog-backend.pid")" "$LEGACY_PID"
check "没有执行任何 sudo 命令" test ! -s "$STUB_LOG_DIR/sudo.log"
check "原回滚记录没有被截断" eq "$(cat "$H/server/.rollback-target")" 'app /previous/release.jar'
common_checks
stop_all

# ---------------------------------------------------------------- zacblog-secrets.sh
PW_NEW='test-only-not-a-real-secret-db-password-new'
JWT_NEW='test-only-not-a-real-secret-jwt-new-0123456789abcdef0123456789abcdef'
# secrets <write-env|rotate> [当前密码]：模拟 server-secrets.yml，经 stdin 传密钥，退出码放在 RC
secrets() {
  local input="$WORK/secrets-input" # 放在 $WORK/$CASE 外面：泄漏检查只扫 $WORK/$CASE
  {
    echo 'DB_HOST=127.0.0.1'
    echo 'DB_PORT=3306'
    echo 'DB_NAME=zaaac_blog'
    echo 'DB_USERNAME=zacblog'
    echo "DB_PASSWORD_CURRENT=${2:-$PW}"
    echo "DB_PASSWORD_NEW=$PW_NEW"
    echo "JWT_SECRET_NEW=$JWT_NEW"
  } > "$input"
  RC=0
  bash "$H/scripts/zacblog-secrets.sh" "$1" < "$input" > "$WORK/$CASE/secrets-$1-$N.out" 2>&1 || RC=$?
  rm -f "$input"
  sed 's/^/       | /' "$WORK/$CASE/secrets-$1-$N.out"
  N=$((N + 1))
}
N=1
db_pw() { cat "$STUB_LOG_DIR/mysql-password"; }
env_pw() { grep '^DB_PASSWORD=' "$H/server/zacblog.env" | cut -d= -f2-; }
env_mode() { stat -c '%a' "$H/server/zacblog.env" 2>/dev/null || stat -f '%Lp' "$H/server/zacblog.env"; }
env_sum() { cksum < "$H/server/zacblog.env"; }
no_secret_leak() {
  local v
  for v in "$PW" "$PW_NEW" "$JWT_NEW"; do
    # 只能出现在环境变量文件和 mysql 桩的密码存储里
    if grep -rlF -- "$v" "$WORK/$CASE" | grep -vE '/home/server/zacblog\.env$|/stublogs/mysql-password$'; then
      return 1
    fi
  done
}
secret_checks() {
  check "环境变量文件权限是 600" eq "$(env_mode)" 600
  check "没有留下临时文件和备份" test -z "$(find "$H/server" -maxdepth 1 -name 'zacblog.env?*')"
  check "sudo 只用了 sudoers 允许的命令" bash -c "! grep -q '^DENIED' '$STUB_LOG_DIR/sudo.log' 2>/dev/null"
  check "密钥没有出现在参数、日志和输出里" no_secret_leak
}

new_server secrets
printf '%s' "$PW" > "$STUB_LOG_DIR/mysql-password"
rm -f "$H/server/zacblog.env"
secrets write-env
check "write-env 成功" eq "$RC" 0
check "生成的环境变量文件内容正确" eq "$(grep -v '^#' "$H/server/zacblog.env")" "SERVER_ADDRESS=127.0.0.1
DB_URL=jdbc:mysql://127.0.0.1:3306/zaaac_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=zacblog
DB_PASSWORD=$PW
JWT_SECRET=$JWT_NEW
UPLOAD_PATH=$H/zaaac/uploadPath"
check "mysql 的参数里没有密码，密码只走 MYSQL_PWD" grep -q -- '--host=127.0.0.1 --port=3306 --user=zacblog' "$STUB_LOG_DIR/mysql-args.log"
secret_checks
secrets counts
check "counts 成功" eq "$RC" 0
check "counts 输出 6 项数量" eq "$(grep -c '^COUNT ' "$WORK/$CASE/secrets-counts-$((N - 1)).out")" 6
check "counts 统计未删除的评论" grep -qE '^COUNT comments +7$' "$WORK/$CASE/secrets-counts-$((N - 1)).out"
check "counts 的 SQL 只有 SELECT（只读）" bash -c "! grep -q alter '$STUB_LOG_DIR/mysql-alter.log' 2>/dev/null"
SUM=$(env_sum)
secrets write-env
check "重跑 write-env 成功且文件不变" bash -c "[ '$RC' = 0 ] && [ \"\$(cksum < '$H/server/zacblog.env')\" = '$SUM' ]"

# 多行 secret：续行不能被回显，也不能改动任何状态（三种续行：无 =、未知键、和已有键重名）
CONT='test-only-secret-continuation'
MYSQL_CALLS=$(wc -l < "$STUB_LOG_DIR/mysql-args.log")
for cont in "$CONT" "$CONT=x" "DB_HOST=$CONT"; do
  for m in write-env rotate counts; do
    RC=0
    printf 'DB_HOST=127.0.0.1\nDB_PORT=3306\nDB_NAME=zaaac_blog\nDB_USERNAME=zacblog\nDB_PASSWORD_CURRENT=%s\n%s\nDB_PASSWORD_NEW=%s\nJWT_SECRET_NEW=%s\n' \
      "$PW" "$cont" "$PW_NEW" "$JWT_NEW" |
      bash "$H/scripts/zacblog-secrets.sh" "$m" > "$WORK/$CASE/multiline.out" 2>&1 || RC=$?
    check "多行 secret（${m}）被拒绝（退出码 3）" eq "$RC" 3
    check "多行 secret（${m}）的续行没有被回显" bash -c "! grep -qF -- '$CONT' '$WORK/$CASE/multiline.out'"
  done
done
check "多行 secret：没有连过 MySQL" eq "$(wc -l < "$STUB_LOG_DIR/mysql-args.log")" "$MYSQL_CALLS"
check "多行 secret：环境变量文件没变" eq "$(env_sum)" "$SUM"
check "多行 secret：数据库密码没变" eq "$(db_pw)" "$PW"
rm -f "$WORK/$CASE/multiline.out"

secrets rotate
check "切换前 rotate 被拒绝（退出码 3）" eq "$RC" 3
check "输出说明旧后端还在运行" grep -q '旧后端 zacblog-backend 还在运行' "$WORK/$CASE/secrets-rotate-$((N - 1)).out"
check "数据库密码没变" eq "$(db_pw)" "$PW"
check "环境变量文件没变" eq "$(env_sum)" "$SUM"

deploy zacblog-server-v2.jar UP v2 v2-frontend
check "用生成的环境变量文件完成切换" eq "$(version)" v2
SUM=$(env_sum)

export MYSQL_STUB_ALTER_FAIL=1
RESTARTS0=$(wc -l < "$STUB_LOG_DIR/systemctl.log")
secrets rotate
unset MYSQL_STUB_ALTER_FAIL
check "ALTER USER 被拒绝时失败并恢复（退出码 1）" eq "$RC" 1
check "数据库密码没变" eq "$(db_pw)" "$PW"
check "环境变量文件恢复原样" eq "$(env_sum)" "$SUM"
check "新后端仍在运行（v2）" eq "$(version)" v2
check "什么都没改时不重启服务" bash -c "! grep -q '^restart zacblog-server' <(sed -n '$((RESTARTS0 + 1)),\$p' '$STUB_LOG_DIR/systemctl.log')"

touch "$STUB_LOG_DIR/java-down-once"
secrets rotate
check "新密码重启后健康检查失败时恢复（退出码 1）" eq "$RC" 1
check "输出里有已恢复原状" grep -q '已恢复原状' "$WORK/$CASE/secrets-rotate-$((N - 1)).out"
check "数据库密码已改回原值" eq "$(db_pw)" "$PW"
check "环境变量文件恢复原样" eq "$(env_sum)" "$SUM"
check "恢复后新后端健康" bash -c "curl -s 'http://127.0.0.1:$STUB_PORT/actuator/health' | grep -q UP"
secret_checks

RESTARTS=$(grep -c '^restart zacblog-server' "$STUB_LOG_DIR/systemctl.log")
secrets rotate
check "rotate 成功" eq "$RC" 0
check "数据库密码已改为 DB_PASSWORD_NEW" eq "$(db_pw)" "$PW_NEW"
check "环境变量文件里是新密码" eq "$(env_pw)" "$PW_NEW"
check "JWT_SECRET 是 JWT_SECRET_NEW" grep -qxF "JWT_SECRET=$JWT_NEW" "$H/server/zacblog.env"
check "zacblog-server 重启过一次" eq "$(grep -c '^restart zacblog-server' "$STUB_LOG_DIR/systemctl.log")" "$((RESTARTS + 1))"
check "重启后健康（v2）" bash -c "curl -s 'http://127.0.0.1:$STUB_PORT/actuator/health' | grep -q UP"
check "输出确认旧密码已不能登录" grep -q '旧密码已不能登录 MySQL' "$WORK/$CASE/secrets-rotate-$((N - 1)).out"
check "旧密码登录 MySQL 失败" bash -c "! printf 'SELECT 1;' | MYSQL_PWD='$PW' mysql >/dev/null 2>&1"
check "改密码只通过 sudo -n systemctl restart" grep -q '^-n systemctl restart zacblog-server$' "$STUB_LOG_DIR/sudo.log"
secret_checks

SUM=$(env_sum)
ALTERS=$(wc -l < "$STUB_LOG_DIR/mysql-alter.log")
secrets rotate
check "重跑 rotate：识别为已轮换，成功" eq "$RC" 0
check "没有再次 ALTER" eq "$(wc -l < "$STUB_LOG_DIR/mysql-alter.log")" "$ALTERS"
check "环境变量文件不变" eq "$(env_sum)" "$SUM"
secrets write-env
check "轮换后重跑 write-env 自动使用新密码" bash -c "[ '$RC' = 0 ] && [ \"\$(cksum < '$H/server/zacblog.env')\" = '$SUM' ]"
secrets write-env 'test-only-wrong-password'
printf '%s' 'test-only-other' > "$STUB_LOG_DIR/mysql-password"
secrets write-env
check "两个密码都登录不了时 write-env 拒绝（退出码 3）" eq "$RC" 3
check "环境变量文件不变" eq "$(env_sum)" "$SUM"
printf '%s' "$PW_NEW" > "$STUB_LOG_DIR/mysql-password"
secret_checks
stop_all

# ---------------------------------------------------------------- bootstrap-root.sh
echo "== bootstrap-root.sh --print（不需要 root）"
BOOT_OUT="$WORK/bootstrap.out"
bash "$REPO/deploy/bootstrap-root.sh" --print > "$BOOT_OUT"
check "服务以 zacblog 运行" grep -q '^User=zacblog$' "$BOOT_OUT"
check "服务工作目录是 server/（不是 backend/）" grep -q '^WorkingDirectory=/home/ZacBlog/server$' "$BOOT_OUT"
check "服务通过 root 所有的启动器运行" grep -q '^ExecStart=/usr/local/lib/zacblog/zacblog-server-run$' "$BOOT_OUT"
check "unit 不让 systemd（root）打开 zacblog 可写的文件" bash -c "! grep -qE '^(EnvironmentFile|StandardOutput|StandardError)=' '$BOOT_OUT'"
check "启动器读取 server/zacblog.env" grep -qF 'done < "$server_dir/zacblog.env"' "$BOOT_OUT"
check "启动器启动 current.jar 并追加 server/app.log" grep -qF -- '-Xms256m -Xmx768m -jar "$server_dir/current.jar" >> "$server_dir/app.log"' "$BOOT_OUT"
for cmd in "start zacblog-server" "stop zacblog-server" "restart zacblog-server" "is-active zacblog-server" \
  "enable zacblog-server" "disable zacblog-server" \
  "start zacblog-backend" "stop zacblog-backend" "enable zacblog-backend" "disable zacblog-backend"; do
  check "sudoers 允许 systemctl $cmd" grep -q "systemctl $cmd\\(,\\|\$\\)" "$BOOT_OUT"
done
check "sudoers 没有放行别的命令" eq "$(grep -c 'systemctl' <(grep '^zacblog ' "$BOOT_OUT" | tr ',' '\n'))" 10

echo
if [ "$FAILS" -eq 0 ]; then
  echo "全部通过"
else
  echo "$FAILS 项失败"
  exit 1
fi
