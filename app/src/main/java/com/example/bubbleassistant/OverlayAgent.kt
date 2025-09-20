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

data class AssistantResult(
    val message: String,
    val bounds: String? = null
)

object OverlayAgent {
    @Volatile
    var taskActive: Boolean = false
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val apiUrl = "https://app-api-service2-855188038216.asia-east1.run.app"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor { m -> Log.d("OkHttp", m) }
            .apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Throws(IOException::class)
    fun callAssistantApi(
        userMsg: String,
        goal: String,
        summaryText: String,
        timestampMs: Long
    ): AssistantResult {
        Log.i("GeminiAPI", "開始呼叫 Gemini API")
        Log.i("GeminiAPI", "使用者問題: $userMsg")
        Log.i("GeminiAPI", "目標: $goal")
        Log.i("GeminiAPI", "螢幕監控資料長度: ${summaryText.length} 字元")
        Log.i("GeminiAPI", "時間戳: $timestampMs")

        val bodyJson = JSONObject().apply {
            put("user_message", userMsg)
            put("goal", goal)
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
            Log.i("GeminiAPI", "HTTP 狀態碼: ${resp.code}")

            if (!resp.isSuccessful) {
                Log.e("GeminiAPI", "API 失敗: HTTP ${resp.code} / $bodyStr")
                return AssistantResult(
                    message = "系統繁忙，請稍後再試（HTTP ${resp.code}）",
                    bounds = null
                )
            }

            return try {
                val outer = JSONObject(bodyStr)

                var bounds: String? = outer.optString("bounds", null).takeIf { !it.isNullOrBlank() }

                val outerMsgRaw = outer.optString("message", bodyStr.ifBlank { "(空回應)" })
                var displayMsg = outerMsgRaw

                // 清掉 markdown 圍欄
                val cleaned = outerMsgRaw
                    .replaceFirst("^```json".toRegex(), "")
                    .replace("```", "")
                    .trim()

                // 如果 cleaned 是 JSON，就再往內解析
                if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                    try {
                        val nested = JSONObject(cleaned)
                        // 取內層要顯示的 message
                        displayMsg = nested.optString("message", displayMsg).ifBlank { displayMsg }
                        // 若外層沒 bounds，再試圖從內層拿
                        if (bounds.isNullOrBlank()) {
                            bounds = nested.optString("bounds", null).takeIf { !it.isNullOrBlank() }
                        }
                    } catch (_: Exception) {
                        // 不是合法 JSON，就用外層字串
                    }
                }

                Log.i("GeminiAPI", "解析成功 -> message='$displayMsg', bounds=$bounds")
                AssistantResult(message = displayMsg, bounds = bounds)
            } catch (e: Exception) {
                Log.e("GeminiAPI", "解析回應失敗: ${e.message}")
                AssistantResult(message = bodyStr.ifBlank { "(空回應)" }, bounds = null)
            }
        }
    }}
