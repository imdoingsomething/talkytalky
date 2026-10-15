"""Env-driven settings. Everything has a default that matches the dev plan."""
import os
from pathlib import Path

WHISPER_MODEL = os.getenv("TT_WHISPER_MODEL", "large-v3-turbo")
WHISPER_DEVICE = os.getenv("TT_WHISPER_DEVICE", "cuda")
WHISPER_COMPUTE = os.getenv("TT_WHISPER_COMPUTE", "float16")
BEAM_SIZE = int(os.getenv("TT_BEAM_SIZE", "1"))

# GGUF path for the cleanup LLM. Qwen2.5-3B-Instruct Q4_K_M first (~2 GB);
# swap to Phi-3.5-mini or Llama-3.2-3B by changing this file only.
CLEANUP_MODEL_PATH = os.getenv("TT_CLEANUP_MODEL", str(Path.home() / "models/qwen2.5-3b-instruct-q4_k_m.gguf"))
CLEANUP_GPU_LAYERS = int(os.getenv("TT_CLEANUP_GPU_LAYERS", "-1"))  # -1 = all on GPU
CLEANUP_ENABLED = os.getenv("TT_CLEANUP", "1") != "0"
# "resident" keeps both models loaded; "sequential" frees the LLM after each request.
VRAM_STRATEGY = os.getenv("TT_VRAM_STRATEGY", "resident")

VAULT = Path(os.getenv("TALKYTALKY_VAULT", str(Path.home() / "ObsidianVaults/TalkyTalky Vault")))
SESSIONS_DIR = os.getenv("TALKYTALKY_SESSIONS_DIR", "Sessions")

MAX_UPLOAD_BYTES = int(os.getenv("TT_MAX_UPLOAD_BYTES", str(25 * 1024 * 1024)))
