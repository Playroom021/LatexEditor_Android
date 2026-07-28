package com.latexeditor.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.latexeditor.app.R
import com.latexeditor.app.data.ProjectRepository
import com.latexeditor.app.editor.EditorActivity
import com.latexeditor.app.editor.LatexTemplates

class TemplatePickerActivity : AppCompatActivity() {

    private lateinit var repo: ProjectRepository
    private lateinit var nameInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_picker)
        repo = ProjectRepository(this)

        findViewById<Toolbar>(R.id.toolbar).let {
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.setNavigationOnClickListener { finish() }
        }

        nameInput = findViewById(R.id.inputProjectName)
        nameInput.setText("Untitled")

        val recycler = findViewById<RecyclerView>(R.id.recyclerTemplates)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = TemplateAdapter(LatexTemplates.catalog) { id, _ -> createAndOpen(id) }
    }

    private fun createAndOpen(templateId: String) {
        val name = nameInput.text.toString().ifBlank { "Untitled" }
        val content = LatexTemplates.byId(templateId)
        val project = repo.createProject(name, content)
        Toast.makeText(this, "Created \"$name\"", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra("project_id", project.id)
        startActivity(intent)
        finish()
    }

    private class TemplateAdapter(
        private val items: List<Pair<String, String>>,
        private val onClick: (String, String) -> Unit
    ) : RecyclerView.Adapter<TemplateAdapter.VH>() {

        class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val text: TextView = itemView.findViewById(R.id.textTemplateName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_template, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (id, label) = items[position]
            holder.text.text = label
            holder.itemView.setOnClickListener { onClick(id, label) }
        }

        override fun getItemCount() = items.size
    }
}
