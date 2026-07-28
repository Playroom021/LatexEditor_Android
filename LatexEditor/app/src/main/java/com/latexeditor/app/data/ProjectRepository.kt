package com.latexeditor.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles all on-disk persistence for projects. No cloud sync - everything
 * lives under Context.filesDir/projects/<id>/ so it survives app restarts
 * and works fully offline except for the compile step itself.
 */
class ProjectRepository(private val context: Context) {

    private val gson = Gson()
    private val projectsRoot = File(context.filesDir, "projects").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "projects_index.json")

    fun listProjects(): MutableList<Project> {
        if (!indexFile.exists()) return mutableListOf()
        val type = object : TypeToken<MutableList<Project>>() {}.type
        return try {
            gson.fromJson(indexFile.readText(), type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveIndex(list: List<Project>) {
        indexFile.writeText(gson.toJson(list))
    }

    fun createProject(name: String, template: String): Project {
        val id = UUID.randomUUID().toString()
        val dir = projectDir(id)
        dir.mkdirs()
        File(dir, "main.tex").writeText(template)
        val project = Project(id = id, name = name)
        val list = listProjects()
        list.add(0, project)
        saveIndex(list)
        return project
    }

    fun deleteProject(project: Project) {
        projectDir(project.id).deleteRecursively()
        val list = listProjects()
        list.removeAll { it.id == project.id }
        saveIndex(list)
    }

    fun renameProject(project: Project, newName: String) {
        val list = listProjects()
        list.find { it.id == project.id }?.name = newName
        saveIndex(list)
    }

    fun touchProject(project: Project) {
        val list = listProjects()
        list.find { it.id == project.id }?.lastOpened = System.currentTimeMillis()
        saveIndex(list.sortedByDescending { it.lastOpened }.toMutableList())
    }

    fun updateProject(project: Project) {
        val list = listProjects()
        val idx = list.indexOfFirst { it.id == project.id }
        if (idx >= 0) list[idx] = project
        saveIndex(list)
    }

    fun projectDir(projectId: String): File = File(projectsRoot, projectId)

    // ---- File tree operations -------------------------------------------------

    fun listFiles(project: Project, subPath: String = ""): List<ProjectFile> {
        val dir = File(projectDir(project.id), subPath)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?.map {
                val rel = if (subPath.isEmpty()) it.name else "$subPath/${it.name}"
                ProjectFile(rel, it.isDirectory)
            } ?: emptyList()
    }

    fun readFile(project: Project, relativePath: String): String {
        val f = File(projectDir(project.id), relativePath)
        return if (f.exists()) f.readText() else ""
    }

    fun writeFile(project: Project, relativePath: String, content: String) {
        val f = File(projectDir(project.id), relativePath)
        f.parentFile?.mkdirs()
        f.writeText(content)
    }

    fun createFile(project: Project, relativePath: String, isDirectory: Boolean) {
        val f = File(projectDir(project.id), relativePath)
        if (isDirectory) f.mkdirs() else {
            f.parentFile?.mkdirs()
            if (!f.exists()) f.createNewFile()
        }
    }

    fun deleteFile(project: Project, relativePath: String) {
        File(projectDir(project.id), relativePath).deleteRecursively()
    }

    fun renameFile(project: Project, relativePath: String, newName: String): String {
        val f = File(projectDir(project.id), relativePath)
        val newFile = File(f.parentFile, newName)
        f.renameTo(newFile)
        val parent = relativePath.substringBeforeLast('/', "")
        return if (parent.isEmpty()) newName else "$parent/$newName"
    }

    // ---- Export / Import --------------------------------------------------

    fun exportProjectZip(project: Project, destZip: File) {
        val root = projectDir(project.id)
        ZipOutputStream(destZip.outputStream()).use { zos ->
            root.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = file.relativeTo(root).path
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun importProjectZip(name: String, zipFile: File): Project {
        val id = UUID.randomUUID().toString()
        val dir = projectDir(id)
        dir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(dir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        val project = Project(id = id, name = name)
        val list = listProjects()
        list.add(0, project)
        saveIndex(list)
        return project
    }
}
