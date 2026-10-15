"""Stage 1: faster-whisper, VAD-trimmed, beam_size=1."""
import config


class Transcriber:
    def __init__(self):
        from faster_whisper import WhisperModel
        self._model = WhisperModel(
            config.WHISPER_MODEL,
            device=config.WHISPER_DEVICE,
            compute_type=config.WHISPER_COMPUTE,
        )

    def transcribe(self, path: str) -> str:
        # faster-whisper decodes via PyAV, so .m4a/.wav/.ogg need no transcoding step.
        segments, _info = self._model.transcribe(
            path,
            beam_size=config.BEAM_SIZE,
            vad_filter=True,
            vad_parameters={"min_silence_duration_ms": 500},
            condition_on_previous_text=False,
        )
        return " ".join(s.text.strip() for s in segments).strip()
