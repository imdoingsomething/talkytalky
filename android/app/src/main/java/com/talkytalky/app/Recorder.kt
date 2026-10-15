package com.talkytalky.app

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/** AAC in MPEG-4 (.m4a), 16 kHz mono: hardware-encoded, small, and faster-whisper decodes it via PyAV with no transcode step. */
class Recorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    var file: File? = null
        private set

    fun start(): Boolean = try {
        val f = File(ctx.cacheDir, "clip.m4a").also { it.delete() }
        rec = MediaRecorder(ctx).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioChannels(1)
            setAudioEncodingBitRate(32_000)
            setOutputFile(f.absolutePath)
            prepare()
            start()
        }
        file = f
        true
    } catch (e: Exception) {
        release(); false
    }

    /** Returns the finished clip, or null if nothing usable was captured. */
    fun stop(): File? {
        val f = file
        val ok = try { rec?.stop(); true } catch (e: RuntimeException) { false } // stop() throws if too short
        release()
        return if (ok && f != null && f.length() > 0) f else null
    }

    fun release() { rec?.release(); rec = null }
}
