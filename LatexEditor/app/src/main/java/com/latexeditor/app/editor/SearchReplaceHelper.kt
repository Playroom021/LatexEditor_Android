package com.latexeditor.app.editor

class SearchReplaceHelper(private val editor: CodeEditorView) {

    private var matches: List<IntRange> = emptyList()
    private var currentIndex = -1
    private var lastQuery = ""

    fun search(query: String, caseSensitive: Boolean = false): Int {
        lastQuery = query
        val text = editor.text?.toString() ?: ""
        if (query.isEmpty()) {
            matches = emptyList()
            currentIndex = -1
            return 0
        }
        val haystack = if (caseSensitive) text else text.lowercase()
        val needle = if (caseSensitive) query else query.lowercase()
        val found = mutableListOf<IntRange>()
        var idx = haystack.indexOf(needle)
        while (idx >= 0) {
            found.add(idx until (idx + needle.length))
            idx = haystack.indexOf(needle, idx + 1)
        }
        matches = found
        currentIndex = if (found.isNotEmpty()) 0 else -1
        selectCurrent()
        return matches.size
    }

    fun next() {
        if (matches.isEmpty()) return
        currentIndex = (currentIndex + 1) % matches.size
        selectCurrent()
    }

    fun previous() {
        if (matches.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) matches.size - 1 else currentIndex - 1
        selectCurrent()
    }

    private fun selectCurrent() {
        if (currentIndex !in matches.indices) return
        val range = matches[currentIndex]
        editor.requestFocus()
        editor.setSelection(range.first, range.last + 1)
    }

    fun replaceCurrent(replacement: String) {
        if (currentIndex !in matches.indices) return
        val range = matches[currentIndex]
        editor.text?.replace(range.first, range.last + 1, replacement)
        search(lastQuery) // recompute matches after text shift
    }

    fun replaceAll(query: String, replacement: String, caseSensitive: Boolean = false): Int {
        val text = editor.text?.toString() ?: return 0
        val haystack = if (caseSensitive) text else text.lowercase()
        val needle = if (caseSensitive) query else query.lowercase()
        if (needle.isEmpty()) return 0
        var count = 0
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (haystack.startsWith(needle, i)) {
                sb.append(replacement)
                i += needle.length
                count++
            } else {
                sb.append(text[i])
                i++
            }
        }
        if (count > 0) {
            editor.setText(sb.toString())
            editor.setSelection(editor.text?.length ?: 0)
        }
        return count
    }

    fun matchCount() = matches.size
    fun currentMatchIndex() = currentIndex
}
