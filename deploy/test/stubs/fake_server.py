"""java 桩背后的 HTTP 服务：读取“jar”文件（其实是 key=value 文本），模拟后端。

  GET /actuator/health  status=UP 时 200 {"status":"UP"}，否则 503 {"status":"DOWN"}
                        （环境变量 FAKE_FORCE_DOWN=1 时总是 DOWN）
  GET /version          返回 version 的值
  其他路径              200，返回 version（模拟没有 actuator 的旧后端）
用法：python3 -I fake_server.py -jar <jar 文件>，端口取环境变量 STUB_PORT。
"""
import http.server
import os
import sys

jar = sys.argv[sys.argv.index("-jar") + 1]
conf = {}
with open(jar, encoding="utf-8") as f:
    for line in f:
        if "=" in line:
            k, v = line.strip().split("=", 1)
            conf[k] = v


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/actuator/health":
            up = conf.get("status") == "UP" and os.environ.get("FAKE_FORCE_DOWN") != "1"
            code, body = (200, '{"status":"UP"}') if up else (503, '{"status":"DOWN"}')
        else:
            code, body = 200, conf.get("version", "unknown")
        data = body.encode()
        self.send_response(code)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        pass


print(f"fake backend {conf.get('version')} listening", flush=True)
http.server.HTTPServer(("127.0.0.1", int(os.environ["STUB_PORT"])), Handler).serve_forever()
