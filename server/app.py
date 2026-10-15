"""POST /transcribe — audio in, cleaned text out. See TalkyTalky Android dev plan, API contract."""
import logging
import os
import tempfile
import time
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse

import config
import vault

log = logging.getLogger("talkytalky")


def create_app(transcriber=None, cleaner=None) -> FastAPI:
    app = FastAPI(title="talkytalky")

    @app.on_event("startup")
    def _load():
        nonlocal transcriber, cleaner
        if transcriber is None:
            from transcribe import Transcriber
            transcriber = Transcriber()
        if cleaner is None:
            from cleanup import Cleaner
            cleaner = Cleaner()

    @app.post("/transcribe")
    async def transcribe(audio: UploadFile = File(...)):
        t0 = time.perf_counter()
        data = await audio.read()
        if not data:
            return JSONResponse({"error": "empty audio"}, status_code=400)
        if len(data) > config.MAX_UPLOAD_BYTES:
            return JSONResponse({"error": "audio too large"}, status_code=413)
        suffix = os.path.splitext(audio.filename or "")[1] or ".m4a"
        try:
            with tempfile.NamedTemporaryFile(suffix=suffix) as f:
                f.write(data)
                f.flush()
                raw = transcriber.transcribe(f.name)
            t1 = time.perf_counter()
            text = cleaner.clean(raw)
            t2 = time.perf_counter()
        except Exception as e:
            log.exception("transcription failed")
            return JSONResponse({"error": f"transcription failed: {e}"}, status_code=500)
        vault.log_session(text, raw)
        log.info("whisper %.2fs cleanup %.2fs total %.2fs (%d bytes)",
                 t1 - t0, t2 - t1, time.perf_counter() - t0, len(data))
        return {"text": text}

    return app


app = create_app() if os.getenv("TT_NO_AUTOLOAD") != "1" else None
