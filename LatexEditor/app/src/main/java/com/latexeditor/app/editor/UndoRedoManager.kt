package com.latexeditor.app.editor

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.ArrayDeque

/**
 * Simple text-snapshot based undo/redo. Coalesces rapid typing into a
 * single undo step by only pushing a snapshot after a short pause or on
 * structural edits (newline, paste, delete-selection).
 */
class UndoRedoManager(private val editText: EditText) {

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var lastSnapshot: String = editText.text.toString()
    private var isUndoOrRedo = false
    private val maxStack = 100

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingSnapshot: Runnable? = null

    fun attach() {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUndoOrRedo) return
                pendingSnapshot?.let { handler.removeCallbacks(it) }
                val r = Runnable { pushSnapshot() }
                pendingSnapshot = r
                handler.postDelayed(r, 600)
            }
        })
    }

    private fun pushSnapshot() {
        val current = editText.text.toString()
        if (current == lastSnapshot) return
        undoStack.push(lastSnapshot)
        if (undoStack.size > maxStack) undoStack.removeLast()
        redoStack.clear()
        lastSnapshot = current
    }

    fun undo() {
        pendingSnapshot?.let { handler.removeCallbacks(it) }
        pushSnapshot() // flush pending edit as a step first
        if (undoStack.isEmpty()) return
        val current = editText.text.toString()
        redoStack.push(current)
        val previous = undoStack.pop()
        applyText(previous)
        lastSnapshot = previous
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = editText.text.toString()
        undoStack.push(current)
        val next = redoStack.pop()
        applyText(next)
        lastSnapshot = next
    }

    private fun applyText(text: String) {
        isUndoOrRedo = true
        val cursor = text.length.coerceAtMost(text.length)
        editText.setText(text)
        editText.setSelection(cursor)
        isUndoOrRedo = false
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}
