package com.latexeditor.app.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.latexeditor.app.R
import com.latexeditor.app.data.Project
import java.text.DateFormat
import java.util.Date

class ProjectAdapter(
    private val items: MutableList<Project>,
    private val onOpen: (Project) -> Unit,
    private val onMore: (Project, View) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.textProjectName)
        val meta: TextView = itemView.findViewById(R.id.textProjectMeta)
        val more: ImageButton = itemView.findViewById(R.id.buttonMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.name.text = p.name
        holder.meta.text = "${p.mainFile} \u00B7 ${p.compiler} \u00B7 ${DateFormat.getDateInstance().format(Date(p.lastOpened))}"
        holder.itemView.setOnClickListener { onOpen(p) }
        holder.more.setOnClickListener { onMore(p, it) }
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<Project>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
