package com.latexeditor.app.compile

import android.content.Context
import com.latexeditor.app.data.CompileResult
import com.latexeditor.app.data.Project
import com.latexeditor.app.data.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit

class CompileRepository(
    private val context: Context,
    private val projectRepo: ProjectRepository,
    private val endpointUrlProvider: () -> String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: CompileApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://placeholder.invalid/") // overridden per-call via @Url
            .client(client)
            .build()
            .create(CompileApi::class.java)
    }

    suspend fun compile(project: Project): CompileResult = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "compile").apply { mkdirs() }
            val zipFile = File(cacheDir, "${project.id}.zip")
            projectRepo.exportProjectZip(project, zipFile)

            val projectPart = MultipartBody.Part.createFormData(
                "project", "project.zip", zipFile.asRequestBody("application/zip".toMediaTypeOrNull())
            )
            val mainPart = MultipartBody.Part.createFormData(
                "main", "main", project.mainFile.toRequestBody("text/plain".toMediaTypeOrNull())
            )
            val enginePart = MultipartBody.Part.createFormData(
                "engine", "engine", project.compiler.toRequestBody("text/plain".toMediaTypeOrNull())
            )

            val response = api.compile(endpointUrlProvider(), projectPart, mainPart, enginePart)

            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext CompileResult(false, log = "Empty response from server")
                val contentType = response.headers()["Content-Type"] ?: ""
                if (contentType.contains("pdf")) {
                    val outFile = File(cacheDir, "${project.id}.pdf")
                    body.byteStream().use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    CompileResult(success = true, pdfPath = outFile.absolutePath, log = "Compiled successfully.")
                } else {
                    val log = body.string()
                    CompileResult(success = false, log = log, errors = ErrorParser.parse(log, project.mainFile))
                }
            } else {
                val errLog = response.errorBody()?.string() ?: "Compile failed (HTTP ${response.code()})"
                CompileResult(success = false, log = errLog, errors = ErrorParser.parse(errLog, project.mainFile))
            }
        } catch (e: Exception) {
            CompileResult(success = false, log = "Network/compile error: ${e.message ?: e.toString()}")
        }
    }
}
