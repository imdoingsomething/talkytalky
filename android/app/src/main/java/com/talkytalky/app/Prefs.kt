package com.talkytalky.app

import android.content.Context

object Prefs {
    private const val FILE = "talkytalky"
    const val DEFAULT_ENDPOINT = "http://100.x.y.z:8765"

    fun endpoint(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString("endpoint", DEFAULT_ENDPOINT)!!

    fun setEndpoint(c: Context, v: String) =
        c.getSharedPreferences(FILE, 0).edit().putString("endpoint", v.trim().trimEnd('/')).apply()
}
