"""Append each session to the shared Obsidian vault (same format as bin/talkytalky-vault-log.sh)."""
import logging
from datetime import datetime
import config

log = logging.getLogger("talkytalky.vault")


def _q(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def log_session(text: str, raw: str, source: str = "android") -> None:
    if not text.strip():
        return
    try:
        d = config.VAULT / config.SESSIONS_DIR
        d.mkdir(parents=True, exist_ok=True)
        now = datetime.now()
        path = d / f"{now:%Y-%m-%d %H%M%S}.md"
        n = 1
        while path.exists():  # two clips in the same second
            path = d / f"{now:%Y-%m-%d %H%M%S}-{n}.md"
            n += 1
        path.write_text(
            "---\n"
            f"date: {now:%Y-%m-%d}\n"
            f"time: {now:%H:%M:%S}\n"
            f"app: {_q(source)}\n"
            'title: ""\n'
            "engine: whisper\n"
            f"model: {_q(config.WHISPER_MODEL)}\n"
            f"words: {len(text.split())}\n"
            "tags: [talkytalky, dictation, mobile]\n"
            "---\n\n"
            f"{text}\n"
            + (f"\n<!-- raw: {raw.replace('-->', '--&gt;')} -->\n" if raw != text else ""),
            encoding="utf-8",
        )
    except Exception:
        log.exception("vault write failed")  # never block the response
