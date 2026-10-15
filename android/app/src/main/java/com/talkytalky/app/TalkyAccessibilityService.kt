package com.talkytalky.app

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

class TalkyAccessibilityService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val sm = RecordingStateMachine()
    private lateinit var recorder: Recorder
    private lateinit var wm: WindowManager
    private lateinit var button: TextView
    private lateinit var lp: WindowManager.LayoutParams

    /** Field that had focus when recording started — text goes here even if focus moves. */
    private var target: AccessibilityNodeInfo? = null
    private var pending: File? = null
    private val timeout = Runnable { handle(sm.onTimeout()) }

    override fun onServiceConnected() {
        recorder = Recorder(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        button = TextView(this).apply {
            textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        }
        lp = WindowManager.LayoutParams(
            140, 140,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // NOT_FOCUSABLE: tapping the button must not steal focus from the text field.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 600 }
        installTouch()
        wm.addView(button, lp)
        render()
    }

    private fun installTouch() {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        button.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(e.rawX - downX) > 12 || abs(e.rawY - downY) > 12) moved = true
                    if (moved) { lp.x = startX + (e.rawX - downX).toInt(); lp.y = startY + (e.rawY - downY).toInt(); wm.updateViewLayout(button, lp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) handle(sm.onTap())
            }
            true
        }
    }

    private fun handle(a: RecordingStateMachine.Action) {
        when (a) {
            RecordingStateMachine.Action.START -> startRecording()
            RecordingStateMachine.Action.STOP_AND_SEND -> {
                main.removeCallbacks(timeout)
                pending = recorder.stop()
                if (pending == null) { sm.onRecorderError(); toast("Too short"); } else send()
            }
            RecordingStateMachine.Action.RETRY -> send()
            RecordingStateMachine.Action.NONE -> {}
        }
        render()
    }

    private fun startRecording() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            sm.onRecorderError(); toast("Open TalkyTalky app to grant mic permission"); return
        }
        target = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (!recorder.start()) { sm.onRecorderError(); toast("Mic unavailable"); return }
        main.postDelayed(timeout, RecordingStateMachine.MAX_RECORDING_MS)
    }

    private fun send() {
        val clip = pending ?: return
        val endpoint = Prefs.endpoint(this)
        io.execute {
            val r = TranscribeClient.transcribe(endpoint, clip)
            main.post {
                when (r) {
                    is TranscribeClient.Result.Ok -> {
                        sm.onSuccess()
                        val t = target
                        if (t == null || !TextInjector.inject(this, t, r.text)) {
                            copy(r.text); toast("No field to type into — copied")
                        }
                        clip.delete(); pending = null
                    }
                    is TranscribeClient.Result.Fail -> { sm.onFailure(); toast("${r.message} — tap to retry") }
                }
                render()
            }
        }
    }

    private fun copy(text: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("talkytalky", text))
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun render() {
        val (color, glyph) = when (sm.state) {
            RecordingStateMachine.State.IDLE -> 0xCC2B2B35.toInt() to "🎤"
            RecordingStateMachine.State.RECORDING -> 0xFFE5484D.toInt() to "⏹"
            RecordingStateMachine.State.SENDING -> 0xFF5B7CFA.toInt() to "…"
            RecordingStateMachine.State.FAILED -> 0xFFE5A100.toInt() to "↻"
        }
        button.text = glyph
        button.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        main.removeCallbacks(timeout)
        recorder.release()
        runCatching { wm.removeView(button) }
        super.onDestroy()
    }
}
