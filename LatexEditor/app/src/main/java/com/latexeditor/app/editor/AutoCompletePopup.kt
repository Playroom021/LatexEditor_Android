package com.latexeditor.app.editor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Small anchored popup (not the IME's own suggestion strip, which we don't
 * control) listing matching \commands, environments, or packages. Tapping
 * an item inserts it into the editor at the cursor.
 */
class AutoCompletePopup(
    private val context: Context,
    private val onPick: (Suggestion, CodeEditorView.TriggerType) -> Unit
) {
    private val recycler = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }
    private val popup = PopupWindow(recycler, ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply {
        isOutsideTouchable = false
        isFocusable = false
        elevation = 8f
    }
    private var currentTriggerType = CodeEditorView.TriggerType.NONE

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    fun update(anchor: View, query: String, triggerType: CodeEditorView.TriggerType) {
        currentTriggerType = triggerType
        if (triggerType == CodeEditorView.TriggerType.NONE) {
            dismiss()
            return
        }
        val source: List<Suggestion> = when (triggerType) {
            CodeEditorView.TriggerType.COMMAND -> LatexData.allCommandLike()
            CodeEditorView.TriggerType.ENVIRONMENT -> LatexData.environments
            CodeEditorView.TriggerType.PACKAGE -> LatexData.packages
            CodeEditorView.TriggerType.NONE -> emptyList()
        }
        val filtered = if (query.isEmpty()) source.take(15) else source.filter {
            it.trigger.startsWith(query, ignoreCase = true)
        }.take(15)

        if (filtered.isEmpty()) {
            dismiss()
            return
        }
        recycler.adapter = SuggestionAdapter(filtered) { s -> onPick(s, currentTriggerType) }
        if (!popup.isShowing) {
            popup.showAtLocation(anchor, android.view.Gravity.BOTTOM, 0, 0)
        }
    }

    fun dismiss() {
        if (popup.isShowing) popup.dismiss()
    }

    private class SuggestionAdapter(
        private val items: List<Suggestion>,
        private val onClick: (Suggestion) -> Unit
    ) : RecyclerView.Adapter<SuggestionAdapter.VH>() {

        class VH(val text: TextView) : RecyclerView.ViewHolder(text)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                setPadding(dpPx(parent, 16), dpPx(parent, 12), dpPx(parent, 16), dpPx(parent, 12))
                textSize = 14f
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            return VH(tv)
        }

        private fun dpPx(parent: ViewGroup, v: Int) = (v * parent.resources.displayMetrics.density).toInt()

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.text.text = if (item.category == "command") "\\${item.trigger}" else item.trigger
            holder.text.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
