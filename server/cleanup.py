"""Stage 2: strip fillers / false starts, keep meaning and tone."""
import logging
import config

log = logging.getLogger("talkytalky.cleanup")

SYSTEM = (
    "You clean up dictated speech. Remove filler words (um, uh, like, you know), "
    "false starts and stutters, and fix obvious punctuation and capitalization. "
    "Do NOT add, summarize, answer, or reword beyond that. Preserve meaning, tone, "
    "and the speaker's vocabulary. Output only the cleaned text, nothing else."
)


class Cleaner:
    def __init__(self):
        self._llm = None

    def _load(self):
        if self._llm is None:
            from llama_cpp import Llama
            self._llm = Llama(
                model_path=config.CLEANUP_MODEL_PATH,
                n_gpu_layers=config.CLEANUP_GPU_LAYERS,
                n_ctx=4096,
                verbose=False,
            )
        return self._llm

    def clean(self, text: str) -> str:
        if not config.CLEANUP_ENABLED or not text.strip():
            return text
        try:
            llm = self._load()
            out = llm.create_chat_completion(
                messages=[{"role": "system", "content": SYSTEM},
                          {"role": "user", "content": text}],
                temperature=0.0,
                max_tokens=max(64, int(len(text.split()) * 2.5)),
            )["choices"][0]["message"]["content"].strip()
            # Guard against the LLM going off-script: fall back to the raw transcript.
            if not out or len(out) > len(text) * 1.5 + 20:
                return text
            return out
        except Exception:
            log.exception("cleanup failed; returning raw transcript")
            return text
        finally:
            if config.VRAM_STRATEGY == "sequential":
                self._llm = None
