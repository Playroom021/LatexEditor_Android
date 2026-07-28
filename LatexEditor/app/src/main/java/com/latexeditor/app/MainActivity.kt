package com.latexeditor.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.latexeditor.app.data.Project
import com.latexeditor.app.data.ProjectRepository
import com.latexeditor.app.editor.EditorActivity
import com.latexeditor.app.files.ProjectAdapter
import com.latexeditor.app.ui.TemplatePickerActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var repo: ProjectRepository
    private lateinit var adapter: ProjectAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = ProjectRepository(this)

        findViewById<Toolbar>(R.id.toolbar).let { setSupportActionBar(it) }

        recycler = findViewById(R.id.recyclerProjects)
        emptyView = findViewById(R.id.textEmpty)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(mutableListOf(), ::openProject, ::showProjectMenu)
        recycler.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabNewProject).setOnClickListener {
            startActivity(Intent(this, TemplatePickerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val projects = repo.listProjects()
        adapter.submit(projects)
        emptyView.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openProject(project: Project) {
        repo.touchProject(project)
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra("project_id", project.id)
        startActivity(intent)
    }

    private fun showProjectMenu(project: Project, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Rename")
        popup.menu.add("Export ZIP")
        popup.menu.add("Delete")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Rename" -> renameProject(project)
                "Export ZIP" -> exportProject(project)
                "Delete" -> confirmDelete(project)
            }
            true
        }
        popup.show()
    }

    private fun renameProject(project: Project) {
        val input = EditText(this).apply { setText(project.name) }
        AlertDialog.Builder(this)
            .setTitle("Rename project")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                repo.renameProject(project, input.text.toString().ifBlank { project.name })
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportProject(project: Project) {
        val zip = java.io.File(cacheDir, "${project.name}.zip")
        repo.exportProjectZip(project, zip)
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", zip)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export project"))
    }

    private fun confirmDelete(project: Project) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${project.name}\"?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                repo.deleteProject(project)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
