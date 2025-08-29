package com.example.bubbleassistant

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object OverlayAgent {

    // 全域、可長存的 scope（不會跟 Activity 一起被銷毀）
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 網路
    private val apiUrl = "https://app-api-service-855188038216.asia-east1.run.app"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor { m -> Log.d("OkHttp", m) }
            .apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** 封裝呼叫 Cloud Run（提供給畫面直接用） */
    @Throws(IOException::class)
    fun callAssistantApi(userMsg: String, summaryText: String, timestampMs: Long): String {
        val bodyJson = JSONObject().apply {
            put("user_message", userMsg)
            put("screen_info", JSONObject().apply {
                put("summaryText", summaryText)
                put("timestampMs", timestampMs)
            })
        }.toString()

        val req = Request.Builder()
            .url(apiUrl)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            Log.d("API", "code=${resp.code}, body=$bodyStr")
            if (!resp.isSuccessful) {
                return "HTTP ${resp.code}\n$bodyStr"
            }
            return try {
                val obj = JSONObject(bodyStr)
                obj.optString("message", bodyStr.ifBlank { "(空回應)" })
            } catch (_: Exception) {
                bodyStr.ifBlank { "(空回應)" }
            }
        }
    }
}
