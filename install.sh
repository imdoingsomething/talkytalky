#!/usr/bin/env bash
# install.sh — set up talkytalky on an Arch / Omarchy box.
#
#   1. voxtype + wtype + wl-clipboard (AUR / pacman)
#   2. scripts → ~/.local/bin
#   3. config template + Hyprland snippet + talkytalky.conf
#   4. voxtype's Quickshell OSD
#   5. talkytalky doctor
set -euo pipefail

ROOT="$(cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" && pwd)"
# shellcheck source=bin/talkytalky-lib.sh
source "$ROOT/bin/talkytalky-lib.sh"

BIN_TARGET="$HOME/.local/bin"
HYPR_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/hypr"
SKIP_PKGS=0
[[ "${1:-}" == "--skip-packages" ]] && SKIP_PKGS=1

tt_head "installing talkytalky"

# ---- 1. packages ------------------------------------------------------------
if (( ! SKIP_PKGS )); then
  if tt_have voxtype; then
    tt_ok "voxtype already installed ($(voxtype --version 2>/dev/null | head -1))"
  else
    helper=""
    for h in paru yay; do tt_have "$h" && { helper="$h"; break; }; done
    if [[ -n "$helper" ]]; then
      tt_dim "  $helper -S voxtype wtype wl-clipboard"
      "$helper" -S --needed --noconfirm voxtype wtype wl-clipboard
    else
      tt_warn "no AUR helper (paru/yay) found — install voxtype yourself:"
      tt_warn "  https://github.com/peteonrails/voxtype#installation"
    fi
  fi
  if tt_have pacman && ! tt_have wtype; then
    sudo pacman -S --needed --noconfirm wtype wl-clipboard
  fi
fi

# ---- 2. scripts -------------------------------------------------------------
mkdir -p "$BIN_TARGET"
for f in talkytalky talkytalky-doctor talkytalky-record-start talkytalky-vault-log.sh talkytalky-lib.sh; do
  ln -sfn "$ROOT/bin/$f" "$BIN_TARGET/$f"
done
chmod +x "$ROOT"/bin/talkytalky "$ROOT"/bin/talkytalky-doctor "$ROOT"/bin/talkytalky-record-start "$ROOT"/bin/talkytalky-vault-log.sh
tt_ok "scripts linked into $BIN_TARGET"
case ":$PATH:" in
  *":$BIN_TARGET:"*) ;;
  *) tt_warn "$BIN_TARGET is not on PATH — add it to your shell rc" ;;
esac

# ---- 3. config --------------------------------------------------------------
mkdir -p "$TT_CONFIG_DIR" "$VOX_CONFIG_DIR" "$HYPR_DIR"
ln -sfn "$ROOT/config/config.toml.tmpl" "$TT_CONFIG_DIR/config.toml.tmpl"
[[ -f "$TT_CONFIG_FILE" ]] || cp "$ROOT/config/talkytalky.conf.example" "$TT_CONFIG_FILE"
ln -sfn "$ROOT/config/hypr-talkytalky.conf" "$HYPR_DIR/talkytalky.conf"
tt_ok "config → $TT_CONFIG_DIR"

hypr_main="$HYPR_DIR/hyprland.conf"
if [[ -f "$hypr_main" ]] && ! grep -q 'talkytalky.conf' "$hypr_main"; then
  printf '\n# talkytalky push-to-talk\nsource = ~/.config/hypr/talkytalky.conf\n' >> "$hypr_main"
  tt_ok "sourced talkytalky.conf from hyprland.conf"
elif [[ ! -f "$hypr_main" ]]; then
  tt_warn "no $hypr_main — add:  source = ~/.config/hypr/talkytalky.conf"
fi

mkdir -p "$TALKYTALKY_VAULT/$TALKYTALKY_SESSIONS_DIR"
tt_ok "vault at $TALKYTALKY_VAULT"

# ---- 4. OSD -------------------------------------------------------------------
if tt_have voxtype && tt_have quickshell; then
  voxtype setup quickshell >/dev/null 2>&1 && tt_ok "quickshell OSD installed" || tt_warn "voxtype setup quickshell failed — the gtk4 OSD still works"
fi

# ---- 5. doctor -----------------------------------------------------------------
echo
"$ROOT/bin/talkytalky-doctor" --recheck --force

if tt_have systemctl && tt_have voxtype; then
  systemctl --user enable --now voxtype >/dev/null 2>&1 && tt_ok "voxtype daemon running" || tt_warn "start the daemon:  systemctl --user enable --now voxtype"
fi

echo
tt_say "  reload hyprland (hyprctl reload), hold F9, talk."
echo
