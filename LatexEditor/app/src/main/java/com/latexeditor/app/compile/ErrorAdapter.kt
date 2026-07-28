package com.latexeditor.app.compile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.latexeditor.app.data.LatexError

class ErrorAdapter(
    private val items: MutableList<LatexError> = mutableListOf(),
    private val onClick: (LatexError) -> Unit
) : RecyclerView.Adapter<ErrorAdapter.VH>() {

    class VH(val root: LinearLayout, val line: TextView, val message: TextView) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val density = parent.resources.displayMetrics.density
        val root = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, (4 * density).toInt())
            layoutParams = lp
            setBackgroundColor(0xFFFFEBEE.toInt())
        }
        val line = TextView(parent.context).apply { textSize = 12f; setTextColor(0xFFB71C1C.toInt()) }
        val message = TextView(parent.context).apply { textSize = 14f; setTextColor(0xFF212121.toInt()) }
        root.addView(line)
        root.addView(message)
        return VH(root, line, message)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.line.text = if (e.line > 0) "Line ${e.line}" else "General error"
        holder.message.text = e.message
        holder.root.setOnClickListener { onClick(e) }
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<LatexError>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
