package com.latexeditor.app.compile

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

/**
 * Generic multipart-upload compile endpoint contract. The default
 * configured server (see SettingsActivity) is a community LaTeX-on-HTTP
 * style service; you can point this at your own compile server (e.g. a
 * small Docker container running `pdflatex`/`latexmk`) by changing the
 * base URL in Settings - no code changes needed as long as it accepts
 * the same multipart contract: a zip of the project under field "project",
 * and a "main" field naming the entry .tex file, returning either the
 * compiled PDF bytes (200, content-type application/pdf) or a JSON/plain
 * text error log (4xx/5xx).
 */
interface CompileApi {

    @Multipart
    @POST
    suspend fun compile(
        @Url endpointUrl: String,
        @Part project: MultipartBody.Part,
        @Part main: MultipartBody.Part,
        @Part engine: MultipartBody.Part
    ): Response<ResponseBody>
}
