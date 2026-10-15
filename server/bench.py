#!/usr/bin/env python3
"""Time real clips end-to-end: python bench.py http://<tailscale-ip>:8765 clip1.m4a clip2.m4a ..."""
import sys, time, httpx

base, clips = sys.argv[1].rstrip("/"), sys.argv[2:]
for c in clips:
    t = time.perf_counter()
    r = httpx.post(f"{base}/transcribe", files={"audio": (c, open(c, "rb"))}, timeout=20)
    print(f"{c}: {r.status_code} {time.perf_counter()-t:.2f}s  {r.text[:100]}")
