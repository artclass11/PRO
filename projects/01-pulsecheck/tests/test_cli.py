from http.server import BaseHTTPRequestHandler, HTTPServer
from threading import Thread
from pulsecheck.__main__ import check

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"ok")
    def log_message(self, format, *args):
        pass

def test_check_local_http_server():
    server = HTTPServer(("127.0.0.1", 0), Handler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        result = check(f"http://127.0.0.1:{server.server_port}/", timeout=2)
        assert result.ok is True
        assert result.status == 200
        assert result.latency_ms is not None
    finally:
        server.shutdown()
        server.server_close()
