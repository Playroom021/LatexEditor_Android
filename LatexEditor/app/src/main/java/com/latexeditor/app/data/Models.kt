package com.latexeditor.app.data

/**
 * A LaTeX project stored under the app's internal storage:
 *   filesDir/projects/<projectId>/...
 */
data class Project(
    val id: String,
    var name: String,
    var mainFile: String = "main.tex",
    var lastOpened: Long = System.currentTimeMillis(),
    var compiler: String = "pdflatex" // pdflatex | xelatex | lualatex
)

/** A single file or folder inside a project's directory tree. */
data class ProjectFile(
    val relativePath: String, // e.g. "chapters/intro.tex"
    val isDirectory: Boolean
) {
    val name: String get() = relativePath.substringAfterLast('/')
}

/** Result of a compile request. */
data class CompileResult(
    val success: Boolean,
    val pdfPath: String? = null,
    val log: String = "",
    val errors: List<LatexError> = emptyList()
)

data class LatexError(
    val file: String,
    val line: Int,
    val message: String
)
