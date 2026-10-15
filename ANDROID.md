# TalkyTalky Android

Self-hosted Wispr Flow replacement. Tap the floating mic in any app, talk, tap again; audio goes to the Alienware over Tailscale, cleaned text is typed into the field that had focus.

- `server/` — Track A. FastAPI + faster-whisper (`large-v3-turbo`, fp16, beam 1, VAD) → local GGUF cleanup LLM (Qwen2.5-3B-Instruct default) → text. Logs each session to the shared Obsidian vault.
- `android/` — Track B. AccessibilityService + `TYPE_ACCESSIBILITY_OVERLAY` button, `.m4a` (AAC, 16 kHz mono) upload, `ACTION_SET_TEXT` injection.

## API contract
`POST /transcribe` (multipart, field `audio`) → `200 {"text"}` or non-200 `{"error"}`. No auth; the server binds only to the Tailscale IP. Client timeout 20 s; failure turns the button amber and a tap retries.

## Server
```sh
cd server
pip install -r requirements.txt
# download a GGUF, e.g. qwen2.5-3b-instruct-q4_k_m.gguf, then:
TT_CLEANUP_MODEL=~/models/qwen2.5-3b-instruct-q4_k_m.gguf ./run.sh   # binds `tailscale ip -4`, port 8765
python bench.py http://<tailscale-ip>:8765 clip.m4a                    # end-to-end latency
python -m pytest tests                                                # API tests (stubbed models)
```
VRAM: `TT_VRAM_STRATEGY=resident` (default) keeps both models loaded (~1.6 GB Whisper + ~2.3 GB LLM fits in 8 GB); set `sequential` if headroom is short. `TT_CLEANUP=0` disables cleanup. Cleanup falls back to the raw transcript on any LLM error or runaway output.

## Android
Needs Android SDK 35 (open `android/` in Android Studio, or `gradle :app:assembleDebug`). Develop UI against `python server/mock_server.py` (`X-Mock: error|slow` simulates failures). First run: set the server address, grant mic, enable the accessibility service.

## Status
Built from the dev plan without a GPU or Android SDK in the sandbox: server API/vault paths are tested with stubs; the Whisper/LLM stages, the Android app, injection across real apps, and latency are **untested** and need a pass on the Alienware and a phone.
