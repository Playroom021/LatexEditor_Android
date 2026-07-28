package com.latexeditor.app.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.latexeditor.app.R
import com.latexeditor.app.data.ProjectFile

class FileAdapter(
    private val items: MutableList<ProjectFile>,
    private val onClick: (ProjectFile) -> Unit,
    private val onLongClick: (ProjectFile, View) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iconFile)
        val name: TextView = itemView.findViewById(R.id.textFileName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = items[position]
        holder.name.text = f.name
        holder.icon.setImageResource(
            when {
                f.isDirectory -> android.R.drawable.ic_menu_agenda
                f.name.endsWith(".tex") -> android.R.drawable.ic_menu_edit
                f.name.endsWith(".bib") -> android.R.drawable.ic_menu_sort_by_size
                f.name.endsWith(".png") || f.name.endsWith(".jpg") || f.name.endsWith(".jpeg") -> android.R.drawable.ic_menu_gallery
                else -> android.R.drawable.ic_menu_save
            }
        )
        holder.itemView.setOnClickListener { onClick(f) }
        holder.itemView.setOnLongClickListener { onLongClick(f, it); true }
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<ProjectFile>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
