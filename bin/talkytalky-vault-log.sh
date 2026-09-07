#!/usr/bin/env bash
# talkytalky-vault-log.sh — voxtype [output.post_process] hook.
#
# Contract (from voxtype): transcript arrives on stdin; whatever we print to
# stdout is what gets typed. So: log it, then echo it back byte-for-byte.
# On *any* failure voxtype falls back to the raw transcript, and we also
# guard every side effect so a broken vault path never eats a dictation.
set -uo pipefail

# shellcheck source=talkytalky-lib.sh
source "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")/talkytalky-lib.sh"

TEXT="$(cat)"

# Optional Wispr-style LLM cleanup, chained *before* logging so the vault
# holds what was actually typed. Falls back to the raw text if it fails.
if [[ -n "$TALKYTALKY_CLEANUP_CMD" ]]; then
  CLEANED="$(printf '%s' "$TEXT" | bash -c "$TALKYTALKY_CLEANUP_CMD" 2>/dev/null)" && [[ -n "$CLEANED" ]] && TEXT="$CLEANED"
fi

# Always emit the text first — the vault write below must never block typing.
printf '%s' "$TEXT"

# Nothing said? Nothing to file.
[[ -n "${TEXT//[[:space:]]/}" ]] || exit 0

{
  SESSIONS="$TALKYTALKY_VAULT/$TALKYTALKY_SESSIONS_DIR"
  mkdir -p "$SESSIONS"

  NOW_DATE="$(date '+%Y-%m-%d')"
  NOW_TIME="$(date '+%H:%M:%S')"
  FILE="$SESSIONS/$NOW_DATE ${NOW_TIME//:/}.md"

  APP_CLASS=unknown APP_TITLE=""
  if [[ -r "$TT_LASTWINDOW" ]]; then
    APP_CLASS="$(sed -n '1p' "$TT_LASTWINDOW")"
    APP_TITLE="$(sed -n '2p' "$TT_LASTWINDOW")"
  fi

  ENGINE="$(tt_engine_from_config)"
  MODEL="$(tt_model_from_config "$ENGINE")"
  WORDS="$(wc -w <<< "$TEXT" | tr -d ' ')"

  # YAML-safe: wrap strings in double quotes, escape embedded quotes/backslashes.
  yq() { local s="${1//\\/\\\\}"; printf '"%s"' "${s//\"/\\\"}"; }

  cat > "$FILE" <<EOF
---
date: $NOW_DATE
time: $NOW_TIME
app: $(yq "$APP_CLASS")
title: $(yq "$APP_TITLE")
engine: $ENGINE
model: $(yq "$MODEL")
words: $WORDS
tags: [talkytalky, dictation]
---

$TEXT
EOF
} 2>/dev/null || true

exit 0
