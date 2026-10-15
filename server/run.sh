#!/usr/bin/env bash
# Bind ONLY to the Tailscale interface — no auth, tailnet membership is the access control.
set -euo pipefail
cd "$(dirname "$0")"
TS_IP="${TT_BIND:-$(tailscale ip -4 | head -n1)}"
[[ -n "$TS_IP" ]] || { echo "no tailscale IPv4 found" >&2; exit 1; }
exec uvicorn app:app --host "$TS_IP" --port "${TT_PORT:-8765}"
