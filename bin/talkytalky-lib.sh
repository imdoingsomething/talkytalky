#!/usr/bin/env bash
# Shared helpers for the talkytalky scripts. Sourced, not executed.
#
# Palette: dark & moody. Everything is dim except the one thing you need to see.

TT_CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/talkytalky"
TT_CONFIG_FILE="$TT_CONFIG_DIR/talkytalky.conf"
VOX_CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/voxtype"
VOX_CONFIG_FILE="$VOX_CONFIG_DIR/config.toml"
TT_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/tmp}/talkytalky"
TT_LASTWINDOW="$TT_RUNTIME_DIR/lastwindow"

# Defaults — overridable in ~/.config/talkytalky/talkytalky.conf
TALKYTALKY_VAULT="${TALKYTALKY_VAULT:-$HOME/ObsidianVaults/TalkyTalky Vault}"
TALKYTALKY_SESSIONS_DIR="${TALKYTALKY_SESSIONS_DIR:-Sessions}"
TALKYTALKY_CLEANUP_CMD="${TALKYTALKY_CLEANUP_CMD:-}"   # e.g. "ollama run llama3.2:1b 'Clean up this dictation...'"

# shellcheck disable=SC1090
[[ -r "$TT_CONFIG_FILE" ]] && source "$TT_CONFIG_FILE"

# ---- colours ---------------------------------------------------------------
if [[ -t 1 && -z "${NO_COLOR:-}" ]]; then
  C_RESET=$'\e[0m'
  C_DIM=$'\e[38;2;90;92;104m'      # ash
  C_FG=$'\e[38;2;200;196;189m'     # bone
  C_ACCENT=$'\e[38;2;124;111;159m' # dusty violet
  C_REC=$'\e[38;2;179;65;58m'      # dried blood
  C_WARN=$'\e[38;2;160;138;74m'    # old brass
  C_OK=$'\e[38;2;96;128;104m'      # moss
  C_BOLD=$'\e[1m'
else
  C_RESET='' C_DIM='' C_FG='' C_ACCENT='' C_REC='' C_WARN='' C_OK='' C_BOLD=''
fi

tt_say()  { printf '%s%s%s\n' "$C_FG" "$*" "$C_RESET"; }
tt_dim()  { printf '%s%s%s\n' "$C_DIM" "$*" "$C_RESET"; }
tt_ok()   { printf '%s  ✓ %s%s\n' "$C_OK" "$*" "$C_RESET"; }
tt_warn() { printf '%s  ! %s%s\n' "$C_WARN" "$*" "$C_RESET" >&2; }
tt_err()  { printf '%s  ✗ %s%s\n' "$C_REC" "$*" "$C_RESET" >&2; }
tt_kv()   { printf '  %s%-14s%s %s%s%s\n' "$C_DIM" "$1" "$C_RESET" "$C_FG" "$2" "$C_RESET"; }
tt_head() { printf '\n%s%s◆ %s%s\n' "$C_ACCENT" "$C_BOLD" "$*" "$C_RESET"; }

tt_have() { command -v "$1" >/dev/null 2>&1; }

# Current engine, read straight from voxtype's config.toml (cheap, no daemon call).
tt_engine_from_config() {
  [[ -r "$VOX_CONFIG_FILE" ]] || { echo unknown; return; }
  sed -nE 's/^[[:space:]]*engine[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$VOX_CONFIG_FILE" | head -1 \
    | grep . || echo whisper
}

# Model for the active engine: first `model = "..."` under the matching [engine] table.
tt_model_from_config() {
  local engine="${1:-$(tt_engine_from_config)}"
  [[ -r "$VOX_CONFIG_FILE" ]] || { echo unknown; return; }
  awk -v want="[$engine]" '
    /^\[/ { in_tbl = ($0 == want) }
    in_tbl && /^[[:space:]]*model[[:space:]]*=/ {
      sub(/^[^"]*"/, ""); sub(/".*$/, ""); print; exit
    }' "$VOX_CONFIG_FILE" | grep . || echo unknown
}
