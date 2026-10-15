package com.talkytalky.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Setup screen: server address, mic permission (a service can't ask), and a shortcut to Accessibility settings. */
class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = 48
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        root.addView(TextView(this).apply {
            text = "TalkyTalky\n\n1. Enter your Alienware's Tailscale address.\n2. Grant mic access.\n3. Enable \"TalkyTalky dictation\" in Accessibility (Android will warn it can read the screen — that's how it types for you)."
        })
        val endpoint = EditText(this).apply {
            setText(Prefs.endpoint(this@MainActivity)); inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        root.addView(endpoint)
        root.addView(Button(this).apply {
            text = "Save address"; setOnClickListener { Prefs.setEndpoint(this@MainActivity, endpoint.text.toString()) }
        })
        root.addView(Button(this).apply {
            text = "Grant microphone"; setOnClickListener { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1) }
        })
        root.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(root)
    }
}
