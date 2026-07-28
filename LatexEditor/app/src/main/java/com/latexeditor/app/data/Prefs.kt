package com.latexeditor.app.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("latex_editor_prefs", Context.MODE_PRIVATE)

    var compileEndpoint: String
        get() = sp.getString("compile_endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
        set(value) = sp.edit().putString("compile_endpoint", value).apply()

    var theme: String // "light" | "dark" | "high_contrast"
        get() = sp.getString("theme", "light") ?: "light"
        set(value) = sp.edit().putString("theme", value).apply()

    var wordWrap: Boolean
        get() = sp.getBoolean("word_wrap", true)
        set(value) = sp.edit().putBoolean("word_wrap", value).apply()

    var lineNumbers: Boolean
        get() = sp.getBoolean("line_numbers", true)
        set(value) = sp.edit().putBoolean("line_numbers", value).apply()

    var autosave: Boolean
        get() = sp.getBoolean("autosave", true)
        set(value) = sp.edit().putBoolean("autosave", value).apply()

    companion object {
        // Community LaTeX-On-HTTP style endpoint. Swap this for your own
        // compile server in Settings for reliability/uptime you control.
        const val DEFAULT_ENDPOINT = "https://latexonline.cc/compile"
    }
}
