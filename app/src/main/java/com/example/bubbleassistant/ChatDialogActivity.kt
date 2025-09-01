package com.example.bubbleassistant

import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatDialogActivity : Activity() {

    private lateinit var editText: EditText
    private lateinit var sendButton: Button
    private lateinit var cancelButton: Button
    private lateinit var responseView: TextView
    private lateinit var dialogBox: View
    private lateinit var rootView: View

    // Overlay
    private var wm: WindowManager? = null
    private var stepView: View? = null
    private var stepLp: WindowManager.LayoutParams? = null
    private var steps: MutableList<String> = mutableListOf() // 永遠只放一行
    private var initialUserMsg: String = ""                  // 第一次輸入
    private var isBusy: Boolean = false                      // 勿連點

    // HTTP client for fetching screen info
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 對話框樣式
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        setContentView(R.layout.dialog_chat)

        // 綁定
        editText = findViewById(R.id.input_message)
        sendButton = findViewById(R.id.send_button)
        responseView = findViewById(R.id.response_text)
        rootView = findViewById(R.id.dialog_root)
        dialogBox = findViewById(R.id.dialog_box)
        cancelButton = findViewById(R.id.cancel_button)

        wm = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager

        // 首次送出
        sendButton.setOnClickListener {
            val message = editText.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener

            Log.i("ChatDialog", "🚀 用戶點擊送出按鈕")
            Log.i("ChatDialog", "📝 用戶輸入: $message")
            
            initialUserMsg = message
            sendButton.isEnabled = false

            // 先關掉鍵盤與對話框，讓畫面不被遮住
            try {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
            } catch (_: Exception) {}
            
            // 關閉對話框
            finish()
            
            // 背景流程：等待鍵盤和對話框完全消失後抓取乾淨的螢幕資訊
            OverlayAgent.scope.launch {
                Log.i("ChatDialog", "⏳ 等待 100ms 讓鍵盤和對話框完全消失")
                // 等待 100ms 讓鍵盤和對話框完全消失
                kotlinx.coroutines.delay(100)
                
                Log.i("ChatDialog", "📱 開始抓取乾淨的螢幕資訊")
                // 在乾淨的螢幕上抓取螢幕資訊
                val screenInfo = withContext(Dispatchers.IO) {
                    getRealTimeScreenInfo()
                }
                
                // 顯示抓取到的螢幕資訊詳細內容
                Log.i("ChatDialog", "📱 勾選確認 - 抓取到的螢幕資訊詳細內容:")
                Log.i("ChatDialog", "📱 螢幕資訊長度: ${screenInfo.length} 字元")
                Log.i("ChatDialog", "📱 螢幕資訊前200字: ${screenInfo.take(200)}")
                Log.i("ChatDialog", "📱 螢幕資訊後200字: ${screenInfo.takeLast(200)}")
                Log.i("ChatDialog", "📱 是否包含假資料標記: ${screenInfo.contains("fakeSummaryText") || screenInfo.contains("備用資料")}")
                Log.i("ChatDialog", "📱 是否包含錯誤訊息: ${screenInfo.contains("無法獲取") || screenInfo.contains("失敗")}")
                
                // 輸出完整的螢幕資訊作為 Chat Input (勾選確認)
                Log.i("ChatDialog", "🔍 === 勾選確認 - 完整的螢幕資訊 (Chat Input) ===")
                Log.i("ChatDialog", "🔍 使用者問題: $initialUserMsg")
                Log.i("ChatDialog", "🔍 螢幕資訊:")
                // 分段輸出長字串，避免 log 被截斷
                val confirmScreenInfoChunks = screenInfo.chunked(1000)
                confirmScreenInfoChunks.forEachIndexed { index, chunk ->
                    Log.i("ChatDialog", "🔍 螢幕資訊片段 ${index + 1}/${confirmScreenInfoChunks.size}: $chunk")
                }
                Log.i("ChatDialog", "🔍 === 勾選確認 - 螢幕資訊輸出完成 ===")
                
                // 顯示抓取到的螢幕資訊詳細內容
                Log.i("ChatDialog", "📱 抓取到的螢幕資訊詳細內容:")
                Log.i("ChatDialog", "📱 螢幕資訊長度: ${screenInfo.length} 字元")
                Log.i("ChatDialog", "📱 螢幕資訊前200字: ${screenInfo.take(200)}")
                Log.i("ChatDialog", "📱 螢幕資訊後200字: ${screenInfo.takeLast(200)}")
                Log.i("ChatDialog", "📱 是否包含假資料標記: ${screenInfo.contains("fakeSummaryText") || screenInfo.contains("備用資料")}")
                Log.i("ChatDialog", "📱 是否包含錯誤訊息: ${screenInfo.contains("無法獲取") || screenInfo.contains("失敗")}")
                
                // 輸出完整的螢幕資訊作為 Chat Input
                Log.i("ChatDialog", "🔍 === 完整的螢幕資訊 (Chat Input) ===")
                Log.i("ChatDialog", "🔍 使用者問題: $initialUserMsg")
                Log.i("ChatDialog", "🔍 螢幕資訊:")
                // 分段輸出長字串，避免 log 被截斷
                val screenInfoChunks = screenInfo.chunked(1000)
                screenInfoChunks.forEachIndexed { index, chunk ->
                    Log.i("ChatDialog", "🔍 螢幕資訊片段 ${index + 1}/${screenInfoChunks.size}: $chunk")
                }
                Log.i("ChatDialog", "🔍 === 螢幕資訊輸出完成 ===")
                
                steps = mutableListOf("請稍候…")
                withContext(Dispatchers.Main) {
                    showStepOverlay()
                    // 等候階段不需要勾選
                    stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = false
                }

                // 呼叫 API 取得下一步（使用已抓取的螢幕資訊）
                try {
                    Log.i("ChatDialog", "🤖 開始呼叫 Gemini API")
                    Log.i("ChatDialog", "📝 發送使用者問題: $initialUserMsg")
                    Log.i("ChatDialog", "📱 發送螢幕資訊長度: ${screenInfo.length} 字元")
                    
                    val serverMessage = withContext(Dispatchers.IO) {
                        OverlayAgent.callAssistantApi(
                            userMsg = initialUserMsg,
                            goal = initialUserMsg,
                            summaryText = screenInfo,
                            timestampMs = System.currentTimeMillis()
                        )
                    }.trim()
                    
                    Log.i("ChatDialog", "🤖 收到 Gemini 回應: $serverMessage")

                    withContext(Dispatchers.Main) {
                        if (serverMessage.contains("恭喜成功")) {
                            steps = mutableListOf("🎉 恭喜成功！")
                            updateStepText()
                            showSuccessThenDismiss()
                        } else {
                            steps = mutableListOf(serverMessage.ifBlank { "請依畫面提示操作下一步" })
                            updateStepText()
                            // 進入可互動狀態 → 顯示勾選
                            stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        steps = mutableListOf("發生錯誤：${e.message ?: "未知錯誤"}")
                        updateStepText()
                        stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = false
                        stepView?.postDelayed({ dismissOverlay() }, 1500)
                    }
                }
            }
        }


        cancelButton.setOnClickListener {
            try {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
            } catch (_: Exception) {}
            finish()
        }

        // 點外面關閉
        rootView.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                val rect = Rect()
                dialogBox.getGlobalVisibleRect(rect)
                if (!rect.contains(e.rawX.toInt(), e.rawY.toInt())) {
                    finish(); return@setOnTouchListener true
                }
            }
            false
        }
    }

    /** 先用假 summaryText（之後換成真監控） */
    private fun fakeSummaryText(): String = """
Captured elements: 20 (showing up to 20)
• (no text)  [id=jp.naver.line.android:id/viewpager]  <androidx.viewpager.widget.ViewPager>  @(0,0,1080,2253)
• "主頁分頁"  [id=jp.naver.line.android:id/bnb_button_clickable_area]  <android.view.View>  @(39,2187,231,2355)
• "聊天選項 勾選 431個新項目"  [id=jp.naver.line.android:id/bnb_button_clickable_area]  <android.view.View>  @(309,2187,501,2355)
• "LINE VOOM選項"  [id=jp.naver.line.android:id/bnb_button_clickable_area]  <android.view.View>  @(579,2187,771,2355)
• "新聞選單"  [id=jp.naver.line.android:id/bnb_button_clickable_area]  <android.view.View>  @(849,2187,1041,2355)
• (no text)  [id=jp.naver.line.android:id/home_tab_list_container]  <android.widget.ScrollView>  @(0,75,0,2253)
• (no text)  [id=jp.naver.line.android:id/header]  <android.view.ViewGroup>  @(0,0,1080,237)
• (no text)  [id=jp.naver.line.android:id/coordinator_layout]  <android.widget.ScrollView>  @(0,237,1080,2253)
• (no text)  [id=jp.naver.line.android:id/voom_tab_header]  <android.view.ViewGroup>  @(1080,0,1080,237)
• (no text)  [id=jp.naver.line.android:id/timeline_feed_view_pager]  <androidx.viewpager.widget.ViewPager>  @(1080,75,1080,2208)
• "楊弘奕的個人圖片"  [id=jp.naver.line.android:id/home_tab_profile_toolbar]  <android.widget.FrameLayout>  @(0,237,0,529)
• "搜尋"  [id=jp.naver.line.android:id/main_tab_search_bar]  <android.view.View>  @(-47,535,0,637)
• "掃描行動條碼"  [id=jp.naver.line.android:id/main_tab_search_bar_scanner_icon]  <android.widget.ImageView>  @(-47,535,0,637)
• (no text)  [id=jp.naver.line.android:id/home_tab_recycler_view]  <androidx.recyclerview.widget.RecyclerView>  @(0,670,0,2253)
• (no text)  [id=jp.naver.line.android:id/keep_header_button]  <android.widget.LinearLayout>  @(-368,75,0,237)
• (no text)  [id=jp.naver.line.android:id/notification_header_button]  <android.widget.LinearLayout>  @(-254,75,0,237)
• (no text)  [id=jp.naver.line.android:id/add_friends_header_button]  <android.widget.LinearLayout>  @(-140,75,0,237)
• (no text)  [id=jp.naver.line.android:id/settings_header_button]  <android.widget.LinearLayout>  @(-26,75,0,237)
• "搜尋"  [id=jp.naver.line.android:id/main_tab_search_bar]  <android.view.View>  @(48,237,1032,259)
• "掃描行動條碼"  [id=jp.naver.line.android:id/main_tab_search_bar_scanner_icon]  <android.widget.ImageView>  @(920,237,1032,259)
""".trimIndent()

    /** 顯示/更新頂部步驟（只顯示目前一步） */
    private fun showStepOverlay() {
        if (steps.isEmpty()) return
        if (stepView == null) {
            stepView = layoutInflater.inflate(R.layout.step_overlay, null)
            stepLp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                x = 0
                y = dp(12)
            }
            wm?.addView(stepView, stepLp)
            bindStepEvents()
        }
        updateStepText()
    }

    /** 勾選後：帶第一次輸入 + 最新 summaryText 再 call；只有含「恭喜成功」才收尾 */
    private fun bindStepEvents() {
        val cb = stepView!!.findViewById<CheckBox>(R.id.btn_check)
        cb.isChecked = false

        cb.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) return@setOnCheckedChangeListener
            if (isBusy) return@setOnCheckedChangeListener
            isBusy = true

            Log.i("ChatDialog", "✅ 用戶勾選確認")
            Log.i("ChatDialog", "📝 當前步驟: ${steps.firstOrNull().orEmpty().trim()}")
            
            cb.isChecked = false
            val currentText = steps.firstOrNull().orEmpty().trim()
            if (currentText.contains("恭喜成功")) {
                Log.i("ChatDialog", "🎉 任務完成，顯示成功訊息")
                showSuccessThenDismiss()
                isBusy = false
                return@setOnCheckedChangeListener
            }
            // 隱藏覆蓋層並抓取乾淨的螢幕資訊
            val tv = stepView?.findViewById<TextView>(R.id.tv_step)
            val cb = stepView?.findViewById<CheckBox>(R.id.btn_check)
            tv?.text = "請稍候…"
            cb?.isVisible = false   // ← 把勾勾隱藏

            OverlayAgent.scope.launch {
                Log.i("ChatDialog", "🔄 勾選確認流程開始")
                
                // 隱藏覆蓋層
                withContext(Dispatchers.Main) {
                    Log.i("ChatDialog", "👁️ 隱藏覆蓋層")
                    flashHideOverlay(100L)
                }
                
                // 等待覆蓋層完全消失
                Log.i("ChatDialog", "⏳ 等待覆蓋層完全消失")
                kotlinx.coroutines.delay(300) // 增加等待時間確保覆蓋層完全消失
                
                // 強制重新抓取螢幕資訊
                Log.i("ChatDialog", "📱 強制重新抓取螢幕資訊")
                val screenInfo = withContext(Dispatchers.IO) {
                    // 使用強制刷新方法
                    Log.i("ChatDialog", "🔄 使用強制刷新方法")
                    val refreshedScreenInfo = ScreenMonitor.forceRefreshScreenInfo()
                    
                    // 如果強制刷新成功，使用它；否則使用原本的方法
                    if (refreshedScreenInfo.isNotEmpty() && !refreshedScreenInfo.contains("螢幕監控服務未運行")) {
                        Log.i("ChatDialog", "✅ 強制刷新成功")
                        refreshedScreenInfo
                    } else {
                        Log.i("ChatDialog", "🔄 強制刷新失敗，使用原本方法")
                        getRealTimeScreenInfo()
                    }
                }
                
                try {
                    Log.i("ChatDialog", "🤖 勾選確認 - 呼叫 Gemini API")
                    Log.i("ChatDialog", "📝 發送原始問題: $initialUserMsg")
                    Log.i("ChatDialog", "📱 發送更新後的螢幕資訊長度: ${screenInfo.length} 字元")
                    
                    // 輸出完整的螢幕資訊作為 Chat Input
                    Log.i("ChatDialog", "🔍 === 勾選確認後的完整螢幕資訊 ===")
                    Log.i("ChatDialog", "🔍 使用者問題: $initialUserMsg")
                    Log.i("ChatDialog", "🔍 螢幕資訊:")
                    val confirmScreenInfoChunks = screenInfo.chunked(1000)
                    confirmScreenInfoChunks.forEachIndexed { index, chunk ->
                        Log.i("ChatDialog", "🔍 螢幕資訊片段 ${index + 1}/${confirmScreenInfoChunks.size}: $chunk")
                    }
                    Log.i("ChatDialog", "🔍 === 勾選確認螢幕資訊輸出完成 ===")
                    
                    val nextMsg = withContext(Dispatchers.IO) {
                        OverlayAgent.callAssistantApi(
                            userMsg = initialUserMsg,                // 每次都帶第一次的輸入
                            goal = initialUserMsg,
                            summaryText = screenInfo,                // 使用已抓取的螢幕監控資料
                            timestampMs = System.currentTimeMillis()
                        )
                    }.trim()
                    
                    Log.i("ChatDialog", "🤖 勾選確認 - 收到 Gemini 回應: $nextMsg")

                    if (nextMsg.contains("恭喜成功")) {
                        steps = mutableListOf("🎉 恭喜成功！")
                        withContext(Dispatchers.Main) {
                            updateStepText()
                            showSuccessThenDismiss()
                        }
                    } else {
                        steps = mutableListOf(nextMsg.ifBlank { "請依畫面提示操作下一步" })
                        withContext(Dispatchers.Main) {
                            // 重新顯示 overlay 並更新內容
                            Log.i("ChatDialog", "🔄 重新顯示 overlay 並更新下一步")
                            stepView?.visibility = View.VISIBLE
                            updateStepText()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "取得下一步失敗：${e.message ?: "未知錯誤"}", Toast.LENGTH_SHORT).show()
                    updateStepText() // 還原
                } finally {
                    isBusy = false
                }
            }
        }
        val closeBtn = stepView!!.findViewById<ImageButton>(R.id.btn_close)
        closeBtn.setOnClickListener {
            steps = mutableListOf("已關閉任務，有問題請再次點擊泡泡詢問喔！")
            updateStepText() // 先更新文字
            // 這次不需要打勾 → 隱藏勾選框
            stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = false
            // 1.2 秒後關掉 overlay
            stepView?.postDelayed({ dismissOverlay() }, 1200)
        }
        // 上滑收起（可選）
        stepView!!.setOnTouchListener(object : View.OnTouchListener {
            private var downY = 0f
            override fun onTouch(v: View?, e: MotionEvent?): Boolean {
                e ?: return false
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> downY = e.rawY
                    MotionEvent.ACTION_UP -> if (e.rawY - downY < -80) { dismissOverlay(); return true }
                }
                return false
            }
        })
    }
    private fun flashHideOverlay(durationMs: Long = 100L) {
        stepView?.let { v ->
            v.visibility = View.GONE
            // 完全隱藏 overlay，不讓它阻擋鍵盤
            Log.i("ChatDialog", "👁️ 完全隱藏 overlay，避免阻擋鍵盤")
        }
    }
    private fun updateStepText() {
        val tv = stepView?.findViewById<TextView>(R.id.tv_step) ?: return
        tv.text = steps.firstOrNull().orEmpty()
        
        // 根據內容決定是否顯示勾選框
        val currentText = steps.firstOrNull().orEmpty()
        val shouldShowCheckbox = !currentText.contains("恭喜成功") && !currentText.contains("已關閉任務")
        
        stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = shouldShowCheckbox
        
        Log.i("ChatDialog", "📝 更新步驟文字: $currentText")
        Log.i("ChatDialog", "📝 是否顯示勾選框: $shouldShowCheckbox")
    }

    private fun showSuccessThenDismiss() {
        val tv = stepView?.findViewById<TextView>(R.id.tv_step) ?: return
        tv.text = "🎉 恭喜成功！"
        stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = false
        stepView?.visibility = View.VISIBLE // 確保 overlay 可見
        stepView?.postDelayed({ dismissOverlay() }, 1200)
        Log.i("ChatDialog", "🎉 顯示成功訊息並準備關閉")
    }

    private fun dismissOverlay() {
        stepView?.let { v -> try { wm?.removeView(v) } catch (_: Exception) {} }
        stepView = null
        stepLp = null
        steps.clear()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    /** 從本地 ScreenInfoServer 獲取即時螢幕資訊 */
    private fun getRealTimeScreenInfo(): String {
        Log.i("ScreenMonitor", "🔍 開始獲取即時螢幕資訊")
        Log.i("ScreenMonitor", "🔍 當前時間: ${System.currentTimeMillis()}")
        Log.i("ScreenMonitor", "🔍 是否為勾選確認流程: ${Thread.currentThread().stackTrace.any { it.methodName.contains("setOnCheckedChangeListener") }}")
        
        // 方案 1: 嘗試 HTTP 方式
        val url = "http://127.0.0.1:${ScreenInfoServer.DEFAULT_PORT}/screen-info"
        Log.i("ScreenMonitor", "🌐 嘗試 HTTP 請求 URL: $url")
        
        val request = Request.Builder().url(url).build()
        return try {
            val response = httpClient.newCall(request).execute()
            Log.i("ScreenMonitor", "📥 收到回應 - HTTP 狀態碼: ${response.code}")
            
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: "{}"
                Log.i("ScreenMonitor", "📱 收到 JSON 資料長度: ${jsonString.length} 字元")
                Log.i("ScreenMonitor", "📱 完整 JSON 回應: $jsonString")
                
                val jsonObject = JSONObject(jsonString)
                val summaryText = jsonObject.optString("summaryText", "無法獲取螢幕資訊")
                
                Log.i("ScreenMonitor", "✅ 成功獲取螢幕資訊 (HTTP)")
                Log.i("ScreenMonitor", "📱 螢幕資訊長度: ${summaryText.length} 字元")
                Log.i("ScreenMonitor", "📱 螢幕資訊前300字: ${summaryText.take(300)}")
                Log.i("ScreenMonitor", "📱 螢幕資訊後300字: ${summaryText.takeLast(300)}")
                
                // 檢查是否為真實資料
                val isRealData = !summaryText.contains("fakeSummaryText") && 
                                !summaryText.contains("備用資料") && 
                                !summaryText.contains("無法獲取") &&
                                summaryText.length > 100
                
                Log.i("ScreenMonitor", "🔍 資料來源判斷:")
                Log.i("ScreenMonitor", "🔍 是否為真實資料: $isRealData")
                Log.i("ScreenMonitor", "🔍 包含假資料標記: ${summaryText.contains("fakeSummaryText")}")
                Log.i("ScreenMonitor", "🔍 包含備用資料標記: ${summaryText.contains("備用資料")}")
                Log.i("ScreenMonitor", "🔍 包含錯誤訊息: ${summaryText.contains("無法獲取")}")
                
                summaryText
            } else {
                Log.e("ScreenMonitor", "❌ HTTP 錯誤: ${response.code}")
                throw IOException("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("ScreenMonitor", "❌ HTTP 連線失敗: ${e.message}")
            
            // 方案 2: 嘗試直接從 ScreenMonitor 獲取
            Log.i("ScreenMonitor", "🔄 嘗試直接從 ScreenMonitor 獲取螢幕資訊")
            try {
                val directScreenInfo = ScreenMonitor.getLatestScreenInfo()
                Log.i("ScreenMonitor", "📱 直接獲取螢幕資訊成功")
                Log.i("ScreenMonitor", "📱 螢幕資訊長度: ${directScreenInfo.length} 字元")
                Log.i("ScreenMonitor", "📱 螢幕資訊前300字: ${directScreenInfo.take(300)}")
                
                // 檢查是否為真實資料
                val isRealData = !directScreenInfo.contains("Waiting for elements") && 
                                !directScreenInfo.contains("螢幕監控服務未運行") &&
                                directScreenInfo.length > 100
                
                Log.i("ScreenMonitor", "🔍 直接獲取資料來源判斷:")
                Log.i("ScreenMonitor", "🔍 是否為真實資料: $isRealData")
                Log.i("ScreenMonitor", "🔍 包含等待標記: ${directScreenInfo.contains("Waiting for elements")}")
                Log.i("ScreenMonitor", "🔍 服務未運行: ${directScreenInfo.contains("螢幕監控服務未運行")}")
                
                directScreenInfo
            } catch (e2: Exception) {
                Log.e("ScreenMonitor", "❌ 直接獲取也失敗: ${e2.message}")
                Log.w("ScreenMonitor", "🔄 最終使用備用假資料")
                "獲取螢幕資訊失敗：${e.message}\n\n使用備用資料：\n${fakeSummaryText()}"
            }
        }
    }
}
