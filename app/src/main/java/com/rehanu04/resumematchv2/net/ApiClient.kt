package com.rehanu04.resumematchv2.net

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {
    // TODO: replace with your Railway URL
    var baseUrl: String = "https://resumematch-v2-api-production.up.railway.app"
    // TODO: set your current X-APP-KEY
    var appKey: String = "REPLACE_ME"

    private val client = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun reqBuilder(path: String): Request.Builder {
        return Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .addHeader("X-APP-KEY", appKey)
    }

    fun analyzeJobText(jdText: String): String {
        val json = JSONObject().put("jd_text", jdText).toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = reqBuilder("/v1/analyze")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val b = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: $b")
            return b
        }
    }

    fun analyzePdf(pdfFile: File, jdText: String): String {
        val mp = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("jd_text", jdText)
            .addFormDataPart(
                "resume",
                pdfFile.name,
                pdfFile.asRequestBody("application/pdf".toMediaType())
            )
            .build()

        val req = reqBuilder("/v1/analyze/pdf")
            .post(mp)
            .build()

        client.newCall(req).execute().use { resp ->
            val b = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: $b")
            return b
        }
    }
}
