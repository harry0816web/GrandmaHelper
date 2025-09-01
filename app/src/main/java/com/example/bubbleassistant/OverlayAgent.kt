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
    fun callAssistantApi(userMsg: String, goal: String, summaryText: String, timestampMs: Long): String {
        Log.i("GeminiAPI", "🚀 開始呼叫 Gemini API")
        Log.i("GeminiAPI", "📝 使用者問題: $userMsg")
        Log.i("GeminiAPI", "🎯 目標: $goal")
        Log.i("GeminiAPI", "📱 螢幕監控資料長度: ${summaryText.length} 字元")
        Log.i("GeminiAPI", "⏰ 時間戳: $timestampMs")
        
        // 顯示螢幕監控資料的前 200 字元和後 200 字元
        if (summaryText.length > 400) {
            Log.i("GeminiAPI", "📱 螢幕監控資料 (前200字): ${summaryText.take(200)}...")
            Log.i("GeminiAPI", "📱 螢幕監控資料 (後200字): ...${summaryText.takeLast(200)}")
        } else {
            Log.i("GeminiAPI", "📱 螢幕監控資料: $summaryText")
        }

        val bodyJson = JSONObject().apply {
            put("user_message", userMsg)
            put("goal", goal)
            put("screen_info", JSONObject().apply {
                put("summaryText", summaryText)
                put("timestampMs", timestampMs)
            })
        }.toString()

        Log.i("GeminiAPI", "📤 發送 JSON 資料長度: ${bodyJson.length} 字元")
        Log.d("GeminiAPI", "📤 完整 JSON: $bodyJson")
        
        // 輸出發送給 Gemini 的完整資料
        Log.i("GeminiAPI", "🔍 === 發送給 Gemini 的完整資料 ===")
        Log.i("GeminiAPI", "🔍 使用者問題: $userMsg")
        Log.i("GeminiAPI", "🔍 目標: $goal")
        Log.i("GeminiAPI", "🔍 螢幕資訊:")
        // 分段輸出長字串，避免 log 被截斷
        val chunks = summaryText.chunked(1000)
        chunks.forEachIndexed { index, chunk ->
            Log.i("GeminiAPI", "🔍 螢幕資訊片段 ${index + 1}/${chunks.size}: $chunk")
        }
        Log.i("GeminiAPI", "🔍 時間戳: $timestampMs")
        Log.i("GeminiAPI", "🔍 === 發送資料完成 ===")

        val req = Request.Builder()
            .url(apiUrl)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .build()

        Log.i("GeminiAPI", "🌐 發送請求到: $apiUrl")

        httpClient.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            Log.i("GeminiAPI", "📥 收到回應 - HTTP 狀態碼: ${resp.code}")
            Log.i("GeminiAPI", "📥 回應內容長度: ${bodyStr.length} 字元")
            
            if (!resp.isSuccessful) {
                Log.e("GeminiAPI", "❌ API 呼叫失敗: HTTP ${resp.code}")
                Log.e("GeminiAPI", "❌ 錯誤回應: $bodyStr")
                return "HTTP ${resp.code}\n$bodyStr"
            }
            
            Log.i("GeminiAPI", "✅ API 呼叫成功")
            Log.d("GeminiAPI", "📥 完整回應: $bodyStr")
            
            return try {
                val obj = JSONObject(bodyStr)
                val message = obj.optString("message", bodyStr.ifBlank { "(空回應)" })
                Log.i("GeminiAPI", "🤖 Gemini 回應: $message")
                message
            } catch (e: Exception) {
                Log.e("GeminiAPI", "❌ 解析回應失敗: ${e.message}")
                val fallback = bodyStr.ifBlank { "(空回應)" }
                Log.i("GeminiAPI", "🤖 使用備用回應: $fallback")
                fallback
            }
        }
    }
}
