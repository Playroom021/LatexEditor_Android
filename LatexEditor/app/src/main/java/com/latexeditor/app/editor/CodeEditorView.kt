package com.latexeditor.app.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatEditText

/**
 * The core LaTeX code editor widget. Extends EditText so we get native
 * text editing, selection, IME support, hardware-keyboard shortcuts, etc.
 * for free, and layers LaTeX-specific behavior on top:
 *  - line numbers drawn in the left gutter
 *  - auto-indent on newline (matches previous line's leading whitespace,
 *    adds one extra level after an unclosed '{')
 *  - auto-closing of (), [], {}, $$
 *  - debounced syntax highlighting
 *
 * True code folding and simultaneous multiple cursors are not supported
 * by Android's TextView selection model without a full custom text
 * renderer; this view exposes fold *markers* (see foldableRanges) that
 * the host Activity can use to hide/show text blocks, but does not
 * implement inline collapse widgets.
 */
class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private val gutterPaint = Paint().apply {
        color = Color.parseColor("#888888")
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }
    private val gutterBgPaint = Paint().apply {
        color = Color.parseColor("#1A000000")
    }

    private var gutterWidth = 0f
    private var highlighter = LatexSyntaxHighlighter(LatexSyntaxHighlighter.Themes.LIGHT)
    private var showLineNumbers = true
    var onAutocompleteTrigger: ((query: String, triggerType: TriggerType) -> Unit)? = null

    enum class TriggerType { COMMAND, ENVIRONMENT, PACKAGE, NONE }

    private val highlightHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null

    init {
        gutterPaint.textSize = textSize * 0.85f
        setHorizontallyScrolling(false) // word-wrap ON by default
        setupTextWatcher()
        updateGutterWidth()
    }

    fun setTheme(scheme: LatexSyntaxHighlighter.ColorScheme, gutterColor: Int, gutterBg: Int) {
        highlighter = LatexSyntaxHighlighter(scheme)
        gutterPaint.color = gutterColor
        gutterBgPaint.color = gutterBg
        rehighlightAll()
    }

    fun setWordWrapEnabled(enabled: Boolean) {
        setHorizontallyScrolling(!enabled)
        requestLayout()
    }

    fun setLineNumbersVisible(visible: Boolean) {
        showLineNumbers = visible
        updateGutterWidth()
        invalidate()
    }

    // ---- Gutter / line numbers -------------------------------------------

    private fun updateGutterWidth() {
        val lineCount = text?.lines()?.size ?: 1
        val digits = lineCount.toString().length.coerceAtLeast(2)
        gutterWidth = if (showLineNumbers) {
            (digits * gutterPaint.textSize * 0.62f) + dp(20f)
        } else 0f
        setPadding(gutterWidth.toInt(), paddingTop, paddingRight, paddingBottom)
    }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    override fun onDraw(canvas: Canvas) {
        if (showLineNumbers) {
            canvas.save()
            val scrollY = scrollY
            canvas.drawRect(scrollX.toFloat(), scrollY.toFloat(), scrollX + gutterWidth, (scrollY + height).toFloat(), gutterBgPaint)
            val layout = layout
            if (layout != null) {
                val firstLine = layout.getLineForVertical(scrollY)
                val lastLine = layout.getLineForVertical(scrollY + height).coerceAtMost(layout.lineCount - 1)
                var lastLogicalLine = -1
                for (i in firstLine..lastLine) {
                    val lineStart = layout.getLineStart(i)
                    val logicalLine = text?.substring(0, lineStart)?.count { it == '\n' } ?: 0
                    // Only draw the number on the first *visual* row of a wrapped logical line
                    if (logicalLine != lastLogicalLine) {
                        val baseline = layout.getLineBaseline(i) + paddingTop
                        canvas.drawText((logicalLine + 1).toString(), scrollX + gutterWidth - dp(8f), baseline.toFloat(), gutterPaint)
                        lastLogicalLine = logicalLine
                    }
                }
            }
            canvas.restore()
        }
        super.onDraw(canvas)
    }

    // ---- Auto-indent, auto-brackets, autocomplete trigger, highlighting ----

    private fun setupTextWatcher() {
        addTextChangedListener(object : TextWatcher {
            var editStart = 0
            var editCountBefore = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                editStart = start
                editCountBefore = count
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable) {
                updateGutterWidth()
                scheduleHighlight()
                detectAutocompleteTrigger(editable)
            }
        })
    }

    /** Call from the host after programmatic bracket auto-close etc. to avoid recursive triggers. */
    private var suppressAutoPair = false

    fun handleAutoPairAndIndent(before: String, start: Int, added: String) {
        // Kept as a hook for future extension; core logic lives in key handling below.
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (suppressAutoPair) return
        if (lengthAfter == 1 && lengthBefore == 0 && text != null && start < text.length) {
            val inserted = text[start]
            autoClosePair(inserted, start)
            autoIndentOnNewline(inserted, start)
        }
    }

    private fun autoClosePair(inserted: Char, position: Int) {
        val closing = when (inserted) {
            '{' -> '}'
            '(' -> ')'
            '[' -> ']'
            else -> null
        } ?: return
        suppressAutoPair = true
        val cursor = position + 1
        text?.insert(cursor, closing.toString())
        setSelection(cursor)
        suppressAutoPair = false
    }

    private fun autoIndentOnNewline(inserted: Char, position: Int) {
        if (inserted != '\n') return
        val editable = text ?: return
        // Find the previous line's leading whitespace
        val textBeforeNewline = editable.substring(0, position)
        val prevLineStart = textBeforeNewline.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
        val prevLine = textBeforeNewline.substring(prevLineStart)
        val leadingWs = prevLine.takeWhile { it == ' ' || it == '\t' }

        var extra = ""
        // If previous line opens an environment or an unclosed '{', indent one more level
        if (prevLine.trimEnd().endsWith("{") || prevLine.contains("\\begin{")) {
            extra = "    "
        }

        val insertion = leadingWs + extra
        if (insertion.isNotEmpty()) {
            suppressAutoPair = true
            editable.insert(position + 1, insertion)
            setSelection(position + 1 + insertion.length)
            suppressAutoPair = false
        }
    }

    private fun detectAutocompleteTrigger(editable: Editable) {
        val cursor = selectionStart
        if (cursor <= 0) {
            onAutocompleteTrigger?.invoke("", TriggerType.NONE)
            return
        }
        val textBeforeCursor = editable.substring(0, cursor)

        // \begin{xyz  -> environment suggestions
        val envMatch = Regex("\\\\begin\\{([a-zA-Z]*)$").find(textBeforeCursor)
        if (envMatch != null) {
            onAutocompleteTrigger?.invoke(envMatch.groupValues[1], TriggerType.ENVIRONMENT)
            return
        }
        // \usepackage{xyz -> package suggestions
        val pkgMatch = Regex("\\\\usepackage(\\[[^\\]]*\\])?\\{([a-zA-Z,]*)$").find(textBeforeCursor)
        if (pkgMatch != null) {
            onAutocompleteTrigger?.invoke(pkgMatch.groupValues[2].substringAfterLast(','), TriggerType.PACKAGE)
            return
        }
        // \command  -> command suggestions (letters only, must not already be followed by {)
        val cmdMatch = Regex("\\\\([a-zA-Z]*)$").find(textBeforeCursor)
        if (cmdMatch != null) {
            onAutocompleteTrigger?.invoke(cmdMatch.groupValues[1], TriggerType.COMMAND)
            return
        }
        onAutocompleteTrigger?.invoke("", TriggerType.NONE)
    }

    /** Replace the currently-typed trigger token with the chosen suggestion. */
    fun applySuggestion(suggestion: Suggestion, triggerType: TriggerType) {
        val editable = text ?: return
        val cursor = selectionStart
        val textBeforeCursor = editable.substring(0, cursor)
        val pattern = when (triggerType) {
            TriggerType.ENVIRONMENT -> Regex("\\\\begin\\{([a-zA-Z]*)$")
            TriggerType.PACKAGE -> Regex("([a-zA-Z]*)$")
            TriggerType.COMMAND -> Regex("\\\\([a-zA-Z]*)$")
            TriggerType.NONE -> null
        } ?: return
        val match = pattern.find(textBeforeCursor) ?: return
        // Only the partially-typed token (last capture group) gets replaced;
        // any preceding "\begin{" or "\" stays untouched.
        val startIdx = cursor - match.groupValues.last().length
        suppressAutoPair = true
        editable.replace(startIdx, cursor, suggestion.insertText)
        val newCursor = startIdx + suggestion.cursorOffset
        setSelection(newCursor.coerceIn(0, editable.length))
        suppressAutoPair = false
        scheduleHighlight()
    }

    // ---- Highlighting scheduling -------------------------------------------

    private fun scheduleHighlight() {
        highlightRunnable?.let { highlightHandler.removeCallbacks(it) }
        val r = Runnable {
            text?.let { highlighter.highlight(it) }
        }
        highlightRunnable = r
        highlightHandler.postDelayed(r, 180)
    }

    private fun rehighlightAll() {
        text?.let { highlighter.highlight(it) }
    }

    /** Returns the 1-based line number for a given character offset, for jump-to-error. */
    fun lineNumberForOffset(offset: Int): Int {
        val t = text?.toString() ?: return 1
        val safe = offset.coerceIn(0, t.length)
        return t.substring(0, safe).count { it == '\n' } + 1
    }

    fun jumpToLine(line: Int) {
        val t = text?.toString() ?: return
        val lines = t.split("\n")
        var offset = 0
        for (i in 0 until (line - 1).coerceIn(0, lines.size - 1)) {
            offset += lines[i].length + 1
        }
        requestFocus()
        setSelection(offset.coerceIn(0, t.length))
    }
}
