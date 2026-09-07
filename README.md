# talkytalky

Hold a key. Talk. Release. The words land at your cursor — and a copy lands in your Obsidian vault.

Local, offline dictation for Arch / Omarchy, built as a thin layer over [voxtype](https://github.com/peteonrails/voxtype). voxtype owns the hard part (audio, hotkeys, Wayland text injection, nine swappable ASR engines). talkytalky adds the two things it doesn't do:

- **Obsidian logging** — one dated `.md` per dictation, with frontmatter for the app you were in, the engine, the word count.
- **`talkytalky doctor`** — scans CPU / RAM / GPU and picks the engine and model that fit *this* machine, then writes the config.

Plus a quiet CLI and a dark, moody on-screen display.

## Install

```sh
git clone https://github.com/imdoingsomething/talkytalky ~/.local/src/talkytalky
~/.local/src/talkytalky/install.sh
```

That installs voxtype (via paru/yay), links the scripts into `~/.local/bin`, drops the Hyprland keybind into `~/.config/hypr/talkytalky.conf` (and sources it), sets up the Quickshell OSD, runs the doctor, and starts the daemon.

Reload Hyprland. Hold **SUPER+V**. Talk.

## Use

```
talkytalky doctor            scan hardware, pick engine, write config   (--dry-run / --json / --recheck)
talkytalky status            idle · recording · transcribing, engine, vault, count
talkytalky log [N]           last N dictations
talkytalky engine [e] [m]    show or swap engine/model by hand
talkytalky vault             open the vault in Obsidian
talkytalky config            edit voxtype's config.toml
talkytalky record start|stop|toggle
```

### What the doctor decides

| Machine | Engine |
|---|---|
| < 8 GB RAM, no dGPU | `moonshine` (~100 MB) |
| ≥ 8 GB, no dGPU, no AVX-512 | `whisper` `base.en` |
| ≥ 8 GB, no dGPU, AVX-512 | `whisper` `small.en` |
| ≥ 16 GB, no dGPU, AVX-512 | `parakeet` `parakeet-tdt-0.6b-v3` |
| dGPU < 4 GB VRAM | `whisper` `small.en` + GPU |
| dGPU ≥ 4 GB VRAM | `whisper` `large-v3-turbo` + GPU |

It only touches the block between `# >>> talkytalky:engine` / `# <<< talkytalky:engine` in `~/.config/voxtype/config.toml`; everything else is yours. A `.bak` is kept on every rewrite. Hardware changed? `talkytalky doctor --recheck`.

### The vault

Every dictation becomes `~/ObsidianVaults/TalkyTalky Vault/Sessions/2026-09-07 142233.md`:

```markdown
---
date: 2026-09-07
time: 14:22:33
app: "kitty"
title: "nvim ~ notes"
engine: whisper
model: "small.en"
words: 42
tags: [talkytalky, dictation]
---

the actual text
```

Change the path in `~/.config/talkytalky/talkytalky.conf`. The logger runs as voxtype's `[output.post_process]` hook and always echoes the text back first — a broken vault path never eats a dictation.

### Optional LLM cleanup

Wispr-style grammar/filler cleanup before the text is typed *and* logged:

```sh
# ~/.config/talkytalky/talkytalky.conf
TALKYTALKY_CLEANUP_CMD="ollama run llama3.2:1b 'Fix grammar and punctuation, remove filler words. Output only the cleaned text:'"
```

Bump `timeout_ms` under `[output.post_process]` to ~30000 if you do.

## Layout

```
install.sh                      packages, symlinks, keybind, doctor
bin/
  talkytalky                    the CLI
  talkytalky-doctor             hardware scan → config
  talkytalky-record-start       snapshots the focused window, then `voxtype record start`
  talkytalky-vault-log.sh       the post_process hook
  talkytalky-lib.sh             shared helpers + palette
config/
  config.toml.tmpl              voxtype config: hooks, OSD recipe, engine block the doctor fills in
  hypr-talkytalky.conf          SUPER+V push-to-talk
  talkytalky.conf.example       vault path, cleanup command
```

## Palette

Near-black `#0e0f13` · bone `#c8c4bd` · dusty violet `#7c6f9f` · dried blood `#b3413a` (recording) · old brass `#a08a4a` (transcribing) · moss `#608068`. The OSD recipe lives in `config.toml.tmpl` under `[osd]` — a low violet waveform, red bars only on peaks.

MIT.
