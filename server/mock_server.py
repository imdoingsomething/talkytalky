#!/usr/bin/env python3
"""Contract-exact mock for Track B. `python mock_server.py` -> :8765. ?fail=1 style via header X-Mock: error|slow."""
import json, time
from http.server import BaseHTTPRequestHandler, HTTPServer


class H(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/transcribe":
            return self._send(404, {"error": "not found"})
        self.rfile.read(int(self.headers.get("Content-Length", 0)))
        mode = self.headers.get("X-Mock", "")
        if mode == "slow":
            time.sleep(25)
        if mode == "error":
            return self._send(500, {"error": "mock failure"})
        time.sleep(0.8)
        self._send(200, {"text": "This is a mocked transcript from the Alienware."})

    def _send(self, code, obj):
        b = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(b)))
        self.end_headers()
        self.wfile.write(b)


HTTPServer(("0.0.0.0", 8765), H).serve_forever()
