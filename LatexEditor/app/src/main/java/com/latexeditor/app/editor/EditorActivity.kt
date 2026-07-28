package com.latexeditor.app.editor

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.latexeditor.app.R
import com.latexeditor.app.compile.CompileRepository
import com.latexeditor.app.compile.ErrorAdapter
import com.latexeditor.app.compile.ErrorParser
import com.latexeditor.app.data.LatexError
import com.latexeditor.app.data.Prefs
import com.latexeditor.app.data.Project
import com.latexeditor.app.data.ProjectFile
import com.latexeditor.app.data.ProjectRepository
import com.latexeditor.app.files.FileAdapter
import com.latexeditor.app.pdf.PdfPageAdapter
import com.latexeditor.app.ui.SettingsActivity
import com.latexeditor.app.ui.SymbolPalette
import com.latexeditor.app.ui.TableGeneratorDialog
import kotlinx.coroutines.launch
import java.io.File

class EditorActivity : AppCompatActivity() {

    private lateinit var projectRepo: ProjectRepository
    private lateinit var compileRepo: CompileRepository
    private lateinit var prefs: Prefs
    private lateinit var project: Project

    private lateinit var drawer: DrawerLayout
    private lateinit var editor: CodeEditorView
    private lateinit var tabLayout: TabLayout
    private lateinit var editorScroll: View
    private lateinit var previewContainer: View
    private lateinit var recyclerErrors: RecyclerView
    private lateinit var recyclerFiles: RecyclerView
    private lateinit var recyclerPdfPages: RecyclerView
    private lateinit var progressCompiling: ProgressBar
    private lateinit var textNoPreview: TextView
    private lateinit var textCurrentPath: TextView
    private lateinit var textDrawerProjectName: TextView

    private lateinit var fileAdapter: FileAdapter
    private lateinit var errorAdapter: ErrorAdapter
    private var pdfAdapter: PdfPageAdapter? = null

    private lateinit var undoRedo: UndoRedoManager
    private lateinit var searchHelper: SearchReplaceHelper
    private lateinit var autoComplete: AutoCompletePopup

    private var currentFilePath: String = "main.tex"
    private var currentDirPath: String = ""
    private val saveHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var saveRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        projectRepo = ProjectRepository(this)
        prefs = Prefs(this)
        compileRepo = CompileRepository(this, projectRepo) { prefs.compileEndpoint }

        val projectId = intent.getStringExtra("project_id") ?: run { finish(); return }
        val loaded = projectRepo.listProjects().find { it.id == projectId }
        if (loaded == null) { finish(); return }
        project = loaded
        currentFilePath = project.mainFile

        bindViews()
        setupToolbar()
        setupTabs()
        setupEditor()
        setupFileExplorer()
        setupQuickToolbar()
        setupFab()

        loadFileIntoEditor(currentFilePath)
    }

    private fun bindViews() {
        drawer = findViewById(R.id.drawerLayout)
        editor = findViewById(R.id.codeEditor)
        tabLayout = findViewById(R.id.tabLayout)
        editorScroll = findViewById(R.id.editorScroll)
        previewContainer = findViewById(R.id.previewContainer)
        recyclerErrors = findViewById(R.id.recyclerErrors)
        recyclerFiles = findViewById(R.id.recyclerFiles)
        recyclerPdfPages = findViewById(R.id.recyclerPdfPages)
        progressCompiling = findViewById(R.id.progressCompiling)
        textNoPreview = findViewById(R.id.textNoPreview)
        textCurrentPath = findViewById(R.id.textCurrentPath)
        textDrawerProjectName = findViewById(R.id.textDrawerProjectName)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = project.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { drawer.openDrawer(GravityCompat.START) }
        textDrawerProjectName.text = project.name
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Editor"))
        tabLayout.addTab(tabLayout.newTab().setText("Preview"))
        tabLayout.addTab(tabLayout.newTab().setText("Errors"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                editorScroll.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                previewContainer.visibility = if (tab.position == 1) View.VISIBLE else View.GONE
                recyclerErrors.visibility = if (tab.position == 2) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupEditor() {
        applyEditorTheme()
        editor.setWordWrapEnabled(prefs.wordWrap)
        editor.setLineNumbersVisible(prefs.lineNumbers)

        undoRedo = UndoRedoManager(editor).also { it.attach() }
        searchHelper = SearchReplaceHelper(editor)
        autoComplete = AutoCompletePopup(this) { suggestion, triggerType ->
            editor.applySuggestion(suggestion, triggerType)
        }
        editor.onAutocompleteTrigger = { query, type -> autoComplete.update(editor, query, type) }

        editor.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                scheduleAutosave()
            }
        })
    }

    private fun applyEditorTheme() {
        val scheme = when (prefs.theme) {
            "dark" -> LatexSyntaxHighlighter.Themes.DARK
            "high_contrast" -> LatexSyntaxHighlighter.Themes.HIGH_CONTRAST
            else -> LatexSyntaxHighlighter.Themes.LIGHT
        }
        val bg = when (prefs.theme) {
            "dark" -> 0xFF1E1E2E.toInt()
            "high_contrast" -> 0xFF000000.toInt()
            else -> 0xFFFFFFFF.toInt()
        }
        val fg = when (prefs.theme) {
            "dark" -> 0xFFE0E0E0.toInt()
            "high_contrast" -> 0xFFFFFFFF.toInt()
            else -> 0xFF212121.toInt()
        }
        editor.setBackgroundColor(bg)
        editor.setTextColor(fg)
        editor.setTheme(scheme, gutterColor = 0xFF888888.toInt(), gutterBg = 0x1A000000)
    }

    private fun scheduleAutosave() {
        if (!prefs.autosave) return
        saveRunnable?.let { saveHandler.removeCallbacks(it) }
        val r = Runnable { saveCurrentFile() }
        saveRunnable = r
        saveHandler.postDelayed(r, 800)
    }

    private fun saveCurrentFile() {
        projectRepo.writeFile(project, currentFilePath, editor.text?.toString() ?: "")
    }

    private fun loadFileIntoEditor(path: String) {
        saveCurrentFile() // persist whatever was open before switching
        currentFilePath = path
        val content = projectRepo.readFile(project, path)
        editor.setText(content)
        editor.setSelection(0)
        supportActionBar?.subtitle = path
    }

    // ---- File explorer -----------------------------------------------------

    private fun setupFileExplorer() {
        recyclerFiles.layoutManager = LinearLayoutManager(this)
        fileAdapter = FileAdapter(mutableListOf(), ::onFileClick, ::onFileLongClick)
        recyclerFiles.adapter = fileAdapter
        refreshFileList()

        findViewById<ImageButton>(R.id.buttonNewFile).setOnClickListener { showNewFileDialog() }
        findViewById<android.widget.Button>(R.id.buttonExportZip).setOnClickListener { exportZip() }
        findViewById<android.widget.Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
    }

    private fun refreshFileList() {
        val files = projectRepo.listFiles(project, currentDirPath)
        fileAdapter.submit(files)
        textCurrentPath.text = if (currentDirPath.isEmpty()) "/" else "/$currentDirPath"
    }

    private fun onFileClick(file: ProjectFile) {
        if (file.isDirectory) {
            currentDirPath = file.relativePath
            refreshFileList()
        } else {
            loadFileIntoEditor(file.relativePath)
            drawer.closeDrawer(GravityCompat.START)
        }
    }

    private fun onFileLongClick(file: ProjectFile, anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.add("Rename")
        popup.menu.add("Delete")
        if (!file.isDirectory) popup.menu.add("Set as main file")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Rename" -> renameFile(file)
                "Delete" -> deleteFile(file)
                "Set as main file" -> {
                    project.mainFile = file.relativePath
                    projectRepo.updateProject(project)
                    Toast.makeText(this, "Main file set to ${file.name}", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
        popup.show()
    }

    private fun showNewFileDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val input = EditText(this).apply { hint = "filename.tex or folder-name" }
        container.addView(input)
        AlertDialog.Builder(this)
            .setTitle("New file or folder")
            .setMessage("End with a file extension (e.g. .tex, .bib) to create a file, or leave without a dot to create a folder.")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val isDir = !name.contains(".")
                val rel = if (currentDirPath.isEmpty()) name else "$currentDirPath/$name"
                projectRepo.createFile(project, rel, isDir)
                refreshFileList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameFile(file: ProjectFile) {
        val input = EditText(this).apply { setText(file.name) }
        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    projectRepo.renameFile(project, file.relativePath, newName)
                    refreshFileList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFile(file: ProjectFile) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${file.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                projectRepo.deleteFile(project, file.relativePath)
                refreshFileList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportZip() {
        val zip = File(cacheDir, "${project.name}.zip")
        projectRepo.exportProjectZip(project, zip)
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", zip)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Export project"))
    }

    // ---- Quick toolbar (bold/italic/symbols/table/undo/redo/search) --------

    private fun setupQuickToolbar() {
        val container = findViewById<LinearLayout>(R.id.quickToolbarContainer)
        fun addButton(label: String, action: () -> Unit) {
            val tv = TextView(this).apply {
                text = label
                textSize = 14f
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setOnClickListener { action() }
            }
            container.addView(tv)
        }

        addButton("Undo") { undoRedo.undo() }
        addButton("Redo") { undoRedo.redo() }
        addButton("B") { wrapSelection("\\textbf{", "}") }
        addButton("I") { wrapSelection("\\textit{", "}") }
        addButton("{ }") { insertAtCursor("{}", cursorBack = 1) }
        addButton("Table") { TableGeneratorDialog.show(this) { code -> insertAtCursor(code, cursorBack = 0) } }
        addButton("\u03a3 Symbols") { showSymbolPalette() }
        addButton("Search") { showSearchDialog() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun wrapSelection(prefix: String, suffix: String) {
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(start)
        val editable = editor.text ?: return
        val selected = editable.substring(start, end)
        editable.replace(start, end, "$prefix$selected$suffix")
        editor.setSelection(start + prefix.length + selected.length + suffix.length)
    }

    private fun insertAtCursor(text: String, cursorBack: Int) {
        val start = editor.selectionStart.coerceAtLeast(0)
        editor.text?.insert(start, text)
        editor.setSelection(start + text.length - cursorBack)
    }

    private fun showSymbolPalette() {
        val allSymbols = SymbolPalette.greek + SymbolPalette.operators + SymbolPalette.structures
        val labels = allSymbols.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Insert symbol")
            .setItems(labels) { _, which ->
                val s = allSymbols[which]
                val start = editor.selectionStart.coerceAtLeast(0)
                editor.text?.insert(start, s.insert)
                val cursor = if (s.cursorOffset >= 0) start + s.cursorOffset else start + s.insert.length
                editor.setSelection(cursor)
            }
            .show()
    }

    private fun showSearchDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), 0)
        }
        val searchInput = EditText(this).apply { hint = "Search" }
        val replaceInput = EditText(this).apply { hint = "Replace with" }
        layout.addView(searchInput)
        layout.addView(replaceInput)

        AlertDialog.Builder(this)
            .setTitle("Search & Replace")
            .setView(layout)
            .setPositiveButton("Replace All") { _, _ ->
                val count = searchHelper.replaceAll(searchInput.text.toString(), replaceInput.text.toString())
                Toast.makeText(this, "Replaced $count occurrence(s)", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Find Next") { _, _ ->
                val count = searchHelper.search(searchInput.text.toString())
                if (count == 0) Toast.makeText(this, "No matches", Toast.LENGTH_SHORT).show()
                else searchHelper.next()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ---- Compile -------------------------------------------------------------

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fabCompile).setOnClickListener { compile() }
    }

    private fun compile() {
        saveCurrentFile()

        // Instant local check before hitting the network, so obvious
        // mistakes (unbalanced \begin/\end) surface immediately.
        val mainContent = projectRepo.readFile(project, project.mainFile)
        val localIssues = ErrorParser.checkUnbalancedEnvironments(mainContent)
        if (localIssues.isNotEmpty()) {
            showErrors(localIssues)
            tabLayout.getTabAt(2)?.select()
            Toast.makeText(this, "Fix structural errors before compiling", Toast.LENGTH_SHORT).show()
            return
        }

        progressCompiling.visibility = View.VISIBLE
        textNoPreview.visibility = View.GONE

        lifecycleScope.launch {
            val result = compileRepo.compile(project)
            progressCompiling.visibility = View.GONE
            if (result.success && result.pdfPath != null) {
                showPdf(File(result.pdfPath))
                showErrors(emptyList())
                tabLayout.getTabAt(1)?.select()
            } else {
                textNoPreview.visibility = View.VISIBLE
                textNoPreview.text = "Compile failed \u2014 see Errors tab"
                showErrors(result.errors.ifEmpty { listOf(LatexError(project.mainFile, 0, result.log.take(400))) })
                tabLayout.getTabAt(2)?.select()
                Toast.makeText(this@EditorActivity, "Compile failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPdf(file: File) {
        textNoPreview.visibility = View.GONE
        if (pdfAdapter == null) {
            pdfAdapter = PdfPageAdapter(file)
            recyclerPdfPages.layoutManager = LinearLayoutManager(this)
            recyclerPdfPages.adapter = pdfAdapter
        } else {
            pdfAdapter?.swapPdf(file)
        }
    }

    // ---- Errors -----------------------------------------------------------

    private fun showErrors(errors: List<LatexError>) {
        if (!this::recyclerErrors.isInitialized) return
        recyclerErrors.layoutManager = LinearLayoutManager(this)
        errorAdapter = ErrorAdapter(mutableListOf()) { err ->
            if (err.line > 0) {
                loadFileIntoEditor(err.file.ifBlank { project.mainFile })
                editor.jumpToLine(err.line)
                tabLayout.getTabAt(0)?.select()
            }
        }
        recyclerErrors.adapter = errorAdapter
        errorAdapter.submit(errors)
    }

    override fun onPause() {
        super.onPause()
        saveCurrentFile()
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfAdapter?.close()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
