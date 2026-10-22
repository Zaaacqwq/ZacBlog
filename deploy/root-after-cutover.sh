#!/usr/bin/env bash
# 切换完成后的一次性 root 收尾（主人审过后手动执行一次；可重复执行）。
#
#   ssh vps "bash -s" < deploy/root-after-cutover.sh
#   bash deploy/root-after-cutover.sh --nginx-edit <配置文件>   # 不需要 root：只把改后的配置打印到 stdout
#
# 做的事（互相独立，某一项失败不影响前面已完成的项；有失败时最后以非 0 退出）：
#   1. 开机自启：zacblog-server 正在运行且健康、zacblog-backend 已停时，
#      systemctl disable zacblog-backend + enable zacblog-server，并打印 is-enabled
#   2. 列出 /root/.ssh/authorized_keys 里每把 key 的类型、注释和指纹（不打印 key 本身，不删除任何 key）
#   3. nginx：只改 blog.zaaac.vip 那个 root 是 /home/ZacBlog/frontend 的 server 块，加一段
#      “# BEGIN zacblog-cache … # END zacblog-cache”（重跑时整体替换）：
#        index.html（包括 try_files 回落、目录 index 的内部跳转）→ Cache-Control: no-cache
#        /assets/（文件名带 hash）→ Cache-Control: public, max-age=31536000, immutable
#      改之前备份到 /root/zacblog-backups/；nginx -t 不过就恢复备份并退出；reload 后用 curl 核对
#      两个响应头，不对就恢复备份、再 reload 并退出。同一文件里的其他 server 块一个字都不动（脚本会校验）。
# 不碰 zacblog 的文件、MySQL、上传目录。
set -euo pipefail

BLOG_HOST=blog.zaaac.vip
WEB_ROOT=/home/ZacBlog/frontend
BLOG_URL=https://$BLOG_HOST
BACKUP_DIR=/root/zacblog-backups
AUTH_KEYS=/root/.ssh/authorized_keys
NEW_SERVICE=zacblog-server
OLD_SERVICE=zacblog-backend
HEALTH_URL=http://127.0.0.1:8080/actuator/health
CC_INDEX='no-cache'
CC_ASSETS='public, max-age=31536000, immutable'

log() { printf '[after-cutover] %s\n' "$*"; }
err() { printf '[after-cutover] ERROR: %s\n' "$*" >&2; }

# ---- nginx 配置编辑器：python3 <文件> <主机名> <root>，改后的完整内容写到 stdout。
# 退出码：0 成功；3 没找到目标 server 块；4 目标块里已有 Cache-Control/expires（不叠加，人工处理）；
#         5 解析失败或自检不通过
read -r -d '' NGINX_EDIT_PY <<'PY' || true
import sys

path, host, web_root, cc_index, cc_assets = sys.argv[1:6]
BEGIN, END = "# BEGIN zacblog-cache", "# END zacblog-cache"


def fail(code, msg):
    sys.stderr.write("nginx-edit: " + msg + "\n")
    sys.exit(code)


def strip_managed(text):
    out, inside = [], False
    for line in text.splitlines(keepends=True):
        s = line.strip()
        if s.startswith(BEGIN):
            if inside:
                fail(5, "BEGIN zacblog-cache 嵌套")
            inside = True
            continue
        if s.startswith(END):
            if not inside:
                fail(5, "多余的 END zacblog-cache")
            inside = False
            continue
        if not inside:
            out.append(line)
    if inside:
        fail(5, "BEGIN zacblog-cache 没有对应的 END")
    return "".join(out)


def tokenize(t):
    """(kind, value, start, end)：kind 是 word / { / } / ;。跳过注释，引号和 ${var} 算在词里。"""
    toks, i, n = [], 0, len(t)
    while i < n:
        c = t[i]
        if c.isspace():
            i += 1
        elif c == "#":
            while i < n and t[i] != "\n":
                i += 1
        elif c in "{};":
            toks.append((c, c, i, i + 1))
            i += 1
        else:
            start, word = i, []
            while i < n and not t[i].isspace() and t[i] not in "{};#":
                if t[i] in "\"'":
                    q, j = t[i], i + 1
                    while j < n and t[j] != q:
                        j += 2 if t[j] == "\\" else 1
                    if j >= n:
                        fail(5, "引号没有闭合（位置 %d）" % i)
                    word.append(t[i + 1:j])
                    i = j + 1
                elif t[i] == "\\" and i + 1 < n:
                    word.append(t[i + 1])
                    i += 2
                elif t[i] == "$" and i + 1 < n and t[i + 1] == "{":
                    j = t.find("}", i)
                    if j < 0:
                        fail(5, "${ 没有闭合")
                    word.append(t[i:j + 1])
                    i = j + 1
                else:
                    word.append(t[i])
                    i += 1
            toks.append(("word", "".join(word), start, i))
    return toks


def parse(t):
    """返回所有块：{name, args, open, close, parent, stmts}；stmts 是直接子语句 (name, args)。"""
    blocks, stack, cur = [], [], []
    root = {"name": "", "args": [], "open": -1, "close": len(t), "parent": None, "stmts": [], "children": []}
    stack.append(root)
    for kind, val, s, e in tokenize(t):
        if kind == "word":
            cur.append(val)
        elif kind == ";":
            if cur:
                stack[-1]["stmts"].append((cur[0], cur[1:]))
            cur = []
        elif kind == "{":
            if not cur:
                fail(5, "{ 前面没有指令名（位置 %d）" % s)
            b = {"name": cur[0], "args": cur[1:], "open": s, "close": None, "parent": stack[-1],
                 "stmts": [], "children": []}
            stack[-1]["children"].append(b)
            blocks.append(b)
            stack.append(b)
            cur = []
        else:
            if cur or len(stack) == 1:
                fail(5, "} 不配对或前面缺分号（位置 %d）" % s)
            stack.pop()["close"] = s
    if len(stack) != 1 or cur:
        fail(5, "花括号不配对")
    return blocks


def descendants(b):
    yield b
    for c in b["children"]:
        yield from descendants(c)


original = open(path, encoding="utf-8").read()
base = strip_managed(original)
blocks = parse(base)

targets = []
for b in blocks:
    if b["name"] != "server":
        continue
    names = [a for n, args in b["stmts"] if n == "server_name" for a in args]
    if host not in names:
        continue
    roots = [args for d in descendants(b) for n, args in d["stmts"] if n == "root"]
    if [web_root] not in roots:
        continue
    for d in descendants(b):
        for n, args in d["stmts"]:
            if n == "expires" or (n == "add_header" and args and args[0].lower() == "cache-control"):
                line = base.count("\n", 0, b["open"]) + 1
                fail(4, "第 %d 行开始的 server 块里已经有 %s %s，不叠加，请人工处理" % (line, n, " ".join(args)))
    targets.append(b)

if not targets:
    fail(3, "没有找到 server_name 含 %s 且 root 是 %s 的 server 块" % (host, web_root))

out = base
for b in sorted(targets, key=lambda b: b["open"], reverse=True):
    eol = out.find("\n", b["open"])
    if eol < 0 or eol > b["close"]:
        fail(5, "server { 和 } 在同一行，无法安全插入")
    # 缩进取块里第一条非空行的缩进
    nxt = eol + 1
    indent = "    "
    while nxt < len(out):
        e2 = out.find("\n", nxt)
        e2 = len(out) if e2 < 0 else e2
        line = out[nxt:e2]
        if line.strip():
            if not line.strip().startswith("}"):
                indent = line[: len(line) - len(line.lstrip())]
            break
        nxt = e2 + 1
    snippet = [
        BEGIN + "（由 deploy/root-after-cutover.sh 维护，重跑会整体替换）",
        "# index.html 每次都向服务器确认；try_files 回落和目录 index 是内部跳转，跳转后会重新执行",
        "# server 级的 set/if，这时 $uri 已经是 /index.html。/assets/ 文件名带 hash，长期缓存。",
        "# 值为空时 nginx 不发这个头；不加 always，所以 404 等错误响应不会被长期缓存。",
        'set $zacblog_cache_control "";',
        'if ($uri = /index.html) { set $zacblog_cache_control "%s"; }' % cc_index,
        'if ($uri ~ ^/assets/) { set $zacblog_cache_control "%s"; }' % cc_assets,
        "add_header Cache-Control $zacblog_cache_control;",
        END,
    ]
    ins = "".join(indent + s + "\n" for s in snippet)
    out = out[: eol + 1] + ins + out[eol + 1:]

# 自检：去掉托管段后必须和原文件（去掉旧托管段）逐字相同，也就是除了这一段什么都没动
if strip_managed(out) != base:
    fail(5, "自检失败：除托管段外内容有变化")
parse(out)
for b in targets:
    sys.stderr.write("nginx-edit: 已处理第 %d 行开始的 server 块\n" % (base.count("\n", 0, b["open"]) + 1))
sys.stdout.write(out)
PY

nginx_edit() { python3 -I -c "$NGINX_EDIT_PY" "$1" "$BLOG_HOST" "$WEB_ROOT" "$CC_INDEX" "$CC_ASSETS"; }

if [ "${1:-}" = --nginx-edit ]; then
  [ -f "${2:-}" ] || { err "用法：root-after-cutover.sh --nginx-edit <配置文件>"; exit 2; }
  nginx_edit "$2"
  exit
fi

[ "$(id -u)" = 0 ] || { err "需要 root 执行"; exit 1; }
for c in python3 nginx curl ssh-keygen systemctl; do
  command -v "$c" >/dev/null 2>&1 || { err "找不到 $c"; exit 1; }
done
FAILED=0

# ================================================================ 1. 开机自启
log "== 1/3 开机自启"
if ! systemctl is-active --quiet "$NEW_SERVICE"; then
  err "$NEW_SERVICE 没在运行，不改开机自启"
  FAILED=1
elif systemctl is-active --quiet "$OLD_SERVICE"; then
  err "$OLD_SERVICE 还在运行，不改开机自启"
  FAILED=1
elif ! curl -s --max-time 5 "$HEALTH_URL" | grep -q '"UP"'; then
  err "$NEW_SERVICE 健康检查不通过，不改开机自启"
  FAILED=1
else
  systemctl disable "$OLD_SERVICE" 2>&1 | sed 's/^/[after-cutover]   /' || FAILED=1
  systemctl enable "$NEW_SERVICE" 2>&1 | sed 's/^/[after-cutover]   /' || FAILED=1
  old_state=$(systemctl is-enabled "$OLD_SERVICE" 2>/dev/null || true)
  new_state=$(systemctl is-enabled "$NEW_SERVICE" 2>/dev/null || true)
  log "is-enabled $OLD_SERVICE：$old_state"
  log "is-enabled $NEW_SERVICE：$new_state"
  if [ "$old_state" = disabled ] && [ "$new_state" = enabled ]; then
    log "开机自启已切到 $NEW_SERVICE"
  else
    err "is-enabled 结果不符合预期（期望 ${OLD_SERVICE}=disabled、${NEW_SERVICE}=enabled）"
    FAILED=1
  fi
fi

# ================================================================ 2. authorized_keys
log "== 2/3 $AUTH_KEYS 里的 key（只列类型、注释和指纹，不删除）"
if [ ! -f "$AUTH_KEYS" ]; then
  log "$AUTH_KEYS 不存在"
else
  n=0
  while IFS= read -r line || [ -n "$line" ]; do
    n=$((n + 1))
    case "$line" in '' | '#'*) continue ;; esac
    # ssh-keygen -l 认 authorized_keys 格式（含 options）；输出：位数 指纹 注释 (类型)
    if fp=$(printf '%s\n' "$line" | ssh-keygen -lf /dev/stdin 2>/dev/null); then
      type=${fp##*(}
      type=${type%)}
      rest=${fp#* }
      fingerprint=${rest%% *}
      comment=${rest#* }
      comment=${comment% (*}
      [ "$comment" != "$rest" ] || comment='（无注释）'
      case "$line" in ssh-* | ecdsa-* | sk-*) opts='' ;; *) opts='，带 options' ;; esac
      log "  第 ${n} 行：${type}  ${comment}  ${fingerprint}${opts}"
    else
      log "  第 ${n} 行：无法解析"
    fi
  done < "$AUTH_KEYS"
  log "请认出旧的部署 key 后自己删除（脚本不删）"
fi

# ================================================================ 3. nginx
log "== 3/3 nginx：$BLOG_HOST 的缓存头"
# 收集 nginx 实际加载的配置文件，找出含 blog server 块的那一个
mapfile -t conf_files < <(nginx -T 2>/dev/null | sed -n 's/^# configuration file \(.*\):$/\1/p' | sort -u)
[ "${#conf_files[@]}" -gt 0 ] || { err "nginx -T 没有列出任何配置文件"; exit 1; }
target=''
for f in "${conf_files[@]}"; do
  [ -f "$f" ] || continue
  grep -q "$BLOG_HOST" "$f" || continue
  rc=0
  nginx_edit "$f" > /dev/null 2>&1 || rc=$?
  case "$rc" in
    0)
      [ -z "$target" ] || { err "不止一个文件里有 $BLOG_HOST 的 server 块：$target、$f，请人工处理"; exit 1; }
      target=$f
      ;;
    3) ;;
    *) nginx_edit "$f" > /dev/null || true; err "无法处理 $f（见上面的 nginx-edit 输出），nginx 没有改动"; exit 1 ;;
  esac
done
[ -n "$target" ] || { err "没有找到 server_name 含 $BLOG_HOST 且 root 是 $WEB_ROOT 的配置，nginx 没有改动"; exit 1; }
real=$(readlink -f "$target")
log "配置文件：$target${real:+（实际文件 $real）}"

install -d -m 700 "$BACKUP_DIR"
stamp=$(date '+%Y%m%d-%H%M%S')
backup="$BACKUP_DIR/nginx-$(basename "$real").$stamp"
# 临时文件和备份都放在 /root 下，不放进 sites-enabled（那里的文件可能被 include 进去）
new_conf="$BACKUP_DIR/.nginx-new.$stamp"
trap 'rm -f "$new_conf"' EXIT
nginx_edit "$real" > "$new_conf"

restore() {
  cat "$backup" > "$real"
  if nginx -t >/dev/null 2>&1; then
    systemctl reload nginx || true
    err "已恢复 $real（备份 $backup）并 reload"
  else
    err "已恢复 $real，但 nginx -t 仍不通过，需要人工处理（备份 $backup）"
  fi
}

changed=0
if cmp -s "$new_conf" "$real"; then
  log "配置已经是最新的，不需要修改"
else
  cp -p "$real" "$backup"
  log "已备份到 $backup"
  # cat > 保留原文件的属主、权限和 inode
  cat "$new_conf" > "$real"
  changed=1
  log "改动（只在 $BLOG_HOST 的 server 块里加了托管段）："
  diff -u "$backup" "$real" | sed 's/^/[after-cutover]   /' || true
  if ! test_out=$(nginx -t 2>&1); then
    printf '%s\n' "$test_out" | sed 's/^/[after-cutover]   /' >&2
    err "nginx -t 不通过"
    cat "$backup" > "$real"
    err "已恢复 $real（备份 $backup），没有 reload"
    exit 1
  fi
  systemctl reload nginx
  log "nginx 已 reload"
  sleep 1
fi

# curl 核对：-D - 只看响应头；同名头必须正好一个且值相同
header_is() { # header_is <url> <期望值>
  local got
  got=$({ curl -sS --max-time 15 -D - -o /dev/null "$1" || true; } | tr -d '\r' | { grep -i '^cache-control:' || true; } | sed 's/^[^:]*: *//')
  if [ "$got" = "$2" ]; then
    log "  $1 → Cache-Control: $got"
    return 0
  fi
  err "  $1 → Cache-Control: '${got//$'\n'/' | '}'（期望 '$2'）"
  return 1
}
ok=1
header_is "$BLOG_URL/" "$CC_INDEX" || ok=0
header_is "$BLOG_URL/index.html" "$CC_INDEX" || ok=0
header_is "$BLOG_URL/zacblog-cache-check-$stamp" "$CC_INDEX" || ok=0 # try_files 回落到 index.html
asset=$(curl -sS --max-time 15 "$BLOG_URL/" | grep -o '/assets/[^"'"'"' >]*\.js' | head -n 1 || true)
if [ -n "$asset" ]; then
  header_is "$BLOG_URL$asset" "$CC_ASSETS" || ok=0
else
  err "  index.html 里没找到 /assets/*.js"
  ok=0
fi
code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$BLOG_URL/api/public/site" || true)
log "  $BLOG_URL/api/public/site → HTTP $code"
[ "$code" = 200 ] || ok=0

if [ "$ok" != 1 ]; then
  if [ "$changed" = 1 ]; then
    restore
  fi
  err "缓存头核对不通过"
  exit 1
fi
log "缓存头核对通过"
rm -f "$new_conf"
trap - EXIT

if [ "$FAILED" != 0 ]; then
  err "有步骤失败，见上面的 ERROR"
  exit 1
fi
log "全部完成"
