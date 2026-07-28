package com.latexeditor.app.editor

import android.text.Editable
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import java.util.regex.Pattern

/**
 * Applies LaTeX syntax highlighting to an Editable in place using Spans.
 * Runs on the UI thread but only over the visible/edited region for
 * performance (see CodeEditorView which debounces + limits range).
 */
class LatexSyntaxHighlighter(private val scheme: ColorScheme) {

    data class ColorScheme(
        val command: Int,
        val comment: Int,
        val math: Int,
        val string: Int,
        val brace: Int,
        val envName: Int
    )

    companion object {
        // \command  (letters only, optional *)
        val COMMAND: Pattern = Pattern.compile("\\\\[a-zA-Z]+\\*?")
        // % comment to end of line (ignore escaped \%)
        val COMMENT: Pattern = Pattern.compile("(?<!\\\\)%.*")
        // $...$ or $$...$$ inline/display math
        val MATH: Pattern = Pattern.compile("\\$\\$?[^$]*\\$?\\$")
        val BRACE: Pattern = Pattern.compile("[{}\\[\\]]")
        // the "figure" in \begin{figure}
        val ENV_NAME: Pattern = Pattern.compile("(?<=\\\\begin\\{)[a-zA-Z\\*]+(?=\\})|(?<=\\\\end\\{)[a-zA-Z\\*]+(?=\\})")
        val STRING: Pattern = Pattern.compile("\"[^\"]*\"")
    }

    fun highlight(editable: Editable, start: Int = 0, end: Int = editable.length) {
        if (start >= end) return
        clearSpans(editable, start, end)
        val region = editable.subSequence(start, end).toString()

        applyPattern(editable, COMMENT, region, start, scheme.comment, bold = false)
        applyPattern(editable, MATH, region, start, scheme.math, bold = false)
        applyPattern(editable, COMMAND, region, start, scheme.command, bold = true)
        applyPattern(editable, ENV_NAME, region, start, scheme.envName, bold = true)
        applyPattern(editable, BRACE, region, start, scheme.brace, bold = false)
        applyPattern(editable, STRING, region, start, scheme.string, bold = false)
    }

    private fun applyPattern(
        editable: Editable,
        pattern: Pattern,
        region: String,
        offset: Int,
        color: Int,
        bold: Boolean
    ) {
        val matcher = pattern.matcher(region)
        while (matcher.find()) {
            val s = offset + matcher.start()
            val e = offset + matcher.end()
            if (s < 0 || e > editable.length || s >= e) continue
            editable.setSpan(ForegroundColorSpan(color), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (bold) {
                editable.setSpan(StyleSpan(Typeface.BOLD), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun clearSpans(editable: Editable, start: Int, end: Int) {
        val colorSpans = editable.getSpans(start, end, ForegroundColorSpan::class.java)
        for (span in colorSpans) editable.removeSpan(span)
        val styleSpans = editable.getSpans(start, end, StyleSpan::class.java)
        for (span in styleSpans) editable.removeSpan(span)
    }

    object Themes {
        val LIGHT = ColorScheme(
            command = 0xFF1565C0.toInt(),
            comment = 0xFF757575.toInt(),
            math = 0xFF2E7D32.toInt(),
            string = 0xFFEF6C00.toInt(),
            brace = 0xFF6A1B9A.toInt(),
            envName = 0xFFC2185B.toInt()
        )
        val DARK = ColorScheme(
            command = 0xFF82AAFF.toInt(),
            comment = 0xFF616161.toInt(),
            math = 0xFFC3E88D.toInt(),
            string = 0xFFF78C6C.toInt(),
            brace = 0xFFC792EA.toInt(),
            envName = 0xFFFF5370.toInt()
        )
        val HIGH_CONTRAST = ColorScheme(
            command = 0xFF00E5FF.toInt(),
            comment = 0xFF9E9E9E.toInt(),
            math = 0xFF76FF03.toInt(),
            string = 0xFFFFEA00.toInt(),
            brace = 0xFFFF4081.toInt(),
            envName = 0xFFFFFFFF.toInt()
        )
    }
}
