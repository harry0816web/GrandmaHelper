package com.example.bubbleassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import android.widget.TextView
import com.example.bubbleassistant.R
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

class ScreenMonitor : AccessibilityService() {

    private val TAG = "ScreenMonitor"

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var overlayTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var eventCount = 0

    // Lightweight HTTP server to expose screen info to LAN/ADB
    private var screenInfoServer: ScreenInfoServer? = null
    private val latestScreenInfoJson = AtomicReference("{\"summaryText\":\"Waiting for elements...\",\"timestampMs\":0}")
    
    // 靜態引用，供其他類別直接訪問
    companion object {
        private var instance: ScreenMonitor? = null
        
        fun getInstance(): ScreenMonitor? = instance
        
        fun getLatestScreenInfo(): String {
            return instance?.let { monitor ->
                val json = monitor.latestScreenInfoJson.get()
                try {
                    val jsonObject = JSONObject(json)
                    jsonObject.optString("summaryText", "無法獲取螢幕資訊")
                } catch (e: Exception) {
                    "無法解析螢幕資訊: ${e.message}"
                }
            } ?: "螢幕監控服務未運行"
        }
        
        fun forceRefreshScreenInfo(): String {
            return instance?.let { monitor ->
                Log.i("ScreenMonitor", "🔄 強制刷新螢幕資訊")
                // 強制刷新所有視窗，專注在 LINE 上
                try {
                    Log.i("ScreenMonitor", "🔍 強制掃描所有視窗，尋找 LINE 應用")
                    monitor.tryGetDataFromAllWindows()
                    // 等待一下讓螢幕資訊更新
                    Thread.sleep(200)
                } catch (e: Exception) {
                    Log.w("ScreenMonitor", "強制刷新失敗: ${e.message}")
                }
                getLatestScreenInfo()
            } ?: "螢幕監控服務未運行"
        }
    }

    // 當服務連接時呼叫
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected!")
        
        // 設置靜態引用
        instance = this

        // 可選：設定服務資訊 - 這些設定會覆蓋 XML 中的配置
        val info = serviceInfo
        // 擴展事件類型以捕獲更多區域的資料
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SELECTED or
                AccessibilityEvent.TYPE_ANNOUNCEMENT or
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        info.notificationTimeout = 50 // 降低延遲以更快響應
        this.serviceInfo = info

        // Initialize a lightweight overlay to show captured elements
        setupOverlay()

        // Start an embedded HTTP server to share captured info
        try {
            if (screenInfoServer == null) {
                screenInfoServer = ScreenInfoServer(jsonProvider = { latestScreenInfoJson.get() }).also { it.start() }
                Log.i(TAG, "ScreenInfoServer started on port ${ScreenInfoServer.DEFAULT_PORT}")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start ScreenInfoServer", t)
        }
    }

    // 當輔助功能事件發生時呼叫
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }

        Log.d(TAG, "Event Type: ${AccessibilityEvent.eventTypeToString(event.eventType)}")
        Log.d(TAG, "Event Package Name: ${event.packageName}")
        Log.d(TAG, "Event Class Name: ${event.className}")
        Log.d(TAG, "Event Text: ${event.text}")
        
        // 每 10 次事件顯示一次狀態
        eventCount++
        if (eventCount % 10 == 0) {
            Log.i(TAG, "📊 螢幕監控服務運行中 - 已處理 $eventCount 個事件")
            Log.i(TAG, "📊 當前應用: ${event.packageName}")
            Log.i(TAG, "📊 事件類型: ${AccessibilityEvent.eventTypeToString(event.eventType)}")
        }

        // 忽略我們自己的應用程式事件
        if (event.packageName == "com.example.bubbleassistant") {
            Log.d(TAG, "忽略 GrandmaHelper 自己的事件")
            return
        }

        // 獲取當前視窗的根節點
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow

        if (rootNode != null) {
            Log.d(TAG, "Root Node Class Name: ${rootNode.className}")
            Log.d(TAG, "Root Node View Id Resource Name: ${rootNode.viewIdResourceName}")
            Log.d(TAG, "Root Node Package Name: ${rootNode.packageName}")
            Log.d(TAG, "Root Node Child Count: ${rootNode.childCount}")
            
            // 忽略我們自己的視窗
            if (rootNode.packageName == "com.example.bubbleassistant") {
                Log.d(TAG, "忽略 GrandmaHelper 自己的視窗")
                return
            }
            
            // 特別針對 LINE 應用進行深度內容掃描
            if (event.packageName == "jp.naver.line.android") {
                Log.d(TAG, "LINE app detected, performing deep content scan...")
                val lineContentSummary = buildLineSpecificSummary(rootNode)
                if (lineContentSummary.isNotEmpty() && !lineContentSummary.contains("No LINE content detected")) {
                    Log.d(TAG, "LINE content found, updating display")
                    updateLatestScreenInfo(lineContentSummary)
                    updateOverlay(lineContentSummary)
                } else {
                    Log.w(TAG, "No LINE content found: $lineContentSummary")
                    // 如果沒找到內容，使用通用掃描結果
                    val summary = buildNodeSummary(rootNode, maxItems = 100)
                    updateLatestScreenInfo("LINE General Scan:\n$summary")
                    updateOverlay("LINE General Scan:\n$summary")
                }
            } else {
                // 非 LINE 應用，使用通用掃描
                val summary = buildNodeSummary(rootNode, maxItems = 100)
                updateLatestScreenInfo(summary)
                updateOverlay(summary)
            }
            
            // 如果摘要為空或項目很少，嘗試獲取所有窗口
            val currentSummary = latestScreenInfoJson.get()
            if (currentSummary.contains("Captured elements: 0") || currentSummary.contains("Captured elements: 1")) {
                tryGetDataFromAllWindows()
            }
        } else {
            Log.w(TAG, "Root node is null. Trying alternative methods...")
            tryGetDataFromAllWindows()
        }
    }

    // 當服務被中斷時呼叫
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted.")
    }

    // 當服務取消綁定時呼叫
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.d(TAG, "Accessibility Service Unbound.")
        removeOverlay()
        stopServerIfRunning()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServerIfRunning()
    }

    // --- Overlay helpers ---
    private fun setupOverlay() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Log.w(TAG, "Overlay not supported on this Android version (< Q). Skipping overlay setup.")
                return
            }
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.overlay_view, null)
            val textView = view.findViewById<TextView>(R.id.tvContent)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            layoutParams.gravity = Gravity.TOP or Gravity.END
            layoutParams.x = 8
            layoutParams.y = 50

            overlayView = view
            overlayTextView = textView
            windowManager.addView(view, layoutParams)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to setup overlay", t)
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
            }
        } catch (_: Throwable) {
        } finally {
            overlayView = null
            overlayTextView = null
        }
    }

    private fun updateOverlay(text: String) {
        mainHandler.post {
            // 只顯示簡單狀態，不顯示完整內容以避免干擾測試
            val summary = when {
                text.contains("LINE app detected") -> "LINE 監控中"
                text.contains("Captured elements: 0") -> "等待中"
                else -> "監控中"
            }
            overlayTextView?.text = summary
        }
    }

    private fun updateLatestScreenInfo(summary: String) {
        val escaped = escapeJsonString(summary)
        val json = "{\"summaryText\":\"$escaped\",\"timestampMs\":${System.currentTimeMillis()}}"
        latestScreenInfoJson.set(json)
        
        Log.i("ScreenMonitor", "💾 更新螢幕資訊快取")
        Log.i("ScreenMonitor", "📱 螢幕資訊長度: ${summary.length} 字元")
        Log.i("ScreenMonitor", "⏰ 更新時間: ${System.currentTimeMillis()}")
        
        // 顯示螢幕資訊的前 100 字元
        if (summary.length > 100) {
            Log.i("ScreenMonitor", "📱 螢幕資訊預覽: ${summary.take(100)}...")
        } else {
            Log.i("ScreenMonitor", "📱 螢幕資訊: $summary")
        }
    }

    private fun escapeJsonString(input: String): String {
        val sb = StringBuilder(input.length + 16)
        input.forEach { ch ->
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun stopServerIfRunning() {
        try {
            screenInfoServer?.stop()
        } catch (_: Throwable) {
        } finally {
            screenInfoServer = null
        }
    }

    // --- Node summary builder ---
    private fun buildNodeSummary(root: AccessibilityNodeInfo, maxItems: Int = 200): String {
        val items = mutableListOf<String>()
        val queue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        queue.add(root)

        while (queue.isNotEmpty() && items.size < maxItems) {
            val node = queue.removeFirst()

            val text = (node.text?.toString()?.takeIf { it.isNotBlank() }
                ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    node.hintText?.toString()?.takeIf { it.isNotBlank() }
                } else null)
            val viewId = node.viewIdResourceName
            val className = node.className?.toString()

            val rect = Rect()
            node.getBoundsInScreen(rect)
            
            // 更寬鬆的可見性檢查
            val isVisible = rect.width() > 0 && rect.height() > 0

            // 更寬鬆的包含條件，抓取更多元素
            val shouldInclude = text != null || 
                    (viewId != null && viewId.isNotBlank()) ||
                    node.isClickable ||
                    node.isCheckable ||
                    node.isEditable ||
                    node.isScrollable ||
                    node.isSelected ||
                    node.isFocused ||
                    (className != null && isImportantClass(className)) ||
                    (node.childCount > 0 && rect.width() > 20 && rect.height() > 20) || // 降低容器大小要求
                    (className != null && (className.contains("View") || className.contains("Layout"))) // 包含更多 View 類型

            if (shouldInclude && isVisible) {
                val label = buildString {
                    if (text != null) append("\u2022 \"$text\"") else append("\u2022 (no text)")
                    if (!viewId.isNullOrBlank()) append("  [id=$viewId]")
                    if (!className.isNullOrBlank()) append("  <$className>")
                    
                    // 添加交互性標記
                    val interactions = mutableListOf<String>()
                    if (node.isClickable) interactions.add("clickable")
                    if (node.isEditable) interactions.add("editable")
                    if (node.isCheckable) interactions.add("checkable")
                    if (node.isScrollable) interactions.add("scrollable")
                    if (node.isSelected) interactions.add("selected")
                    if (node.isFocused) interactions.add("focused")
                    if (interactions.isNotEmpty()) append("  {${interactions.joinToString(",")}}")
                    
                    append("  @(${rect.left},${rect.top},${rect.width()}x${rect.height()})")
                }
                items.add(label)
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    queue.add(child)
                }
            }
        }

        val header = "Captured elements: ${items.size} (showing up to $maxItems)"
        return (sequenceOf(header) + items.asSequence()).joinToString(separator = "\n")
    }
    
    private fun isImportantClass(className: String): Boolean {
        return className.contains("Button", ignoreCase = true) ||
                className.contains("TextView", ignoreCase = true) ||
                className.contains("EditText", ignoreCase = true) ||
                className.contains("ImageView", ignoreCase = true) ||
                className.contains("RecyclerView", ignoreCase = true) ||
                className.contains("ListView", ignoreCase = true) ||
                className.contains("TabHost", ignoreCase = true) ||
                className.contains("ViewPager", ignoreCase = true) ||
                className.contains("LinearLayout", ignoreCase = true) ||
                className.contains("FrameLayout", ignoreCase = true) ||
                className.contains("RelativeLayout", ignoreCase = true) ||
                className.contains("ConstraintLayout", ignoreCase = true) ||
                className.contains("ScrollView", ignoreCase = true) ||
                className.contains("NestedScrollView", ignoreCase = true) ||
                className.contains("CardView", ignoreCase = true) ||
                className.contains("MaterialCardView", ignoreCase = true) ||
                className.contains("ViewGroup", ignoreCase = true) ||
                className.contains("View", ignoreCase = true)
    }
    
    private fun buildLineSpecificSummary(root: AccessibilityNodeInfo): String {
        val items = mutableListOf<String>()
        
        try {
            Log.d(TAG, "Building LINE specific summary...")
            
            // 識別當前頁面類型
            val currentPageType = identifyLinePageType(root)
            val currentPageTitle = extractCurrentPageTitle(root)
            
            // 專門查找 LINE 中的所有可見內容
            findLineContent(root, items, 0, maxDepth = 30)
            
            Log.d(TAG, "Found ${items.size} LINE content items")
            
            // 根據當前頁面類型過濾相關內容
            val filteredItems = filterItemsByPageType(items, currentPageType)
            
            // 按重要性排序內容
            val sortedItems = filteredItems.sortedBy { item ->
                when {
                    item.contains("clickable") && !item.contains("(no text)") -> 0  // 可點擊元素優先
                    item.contains("editable") -> 1  // 可編輯元素
                    item.contains("TextView") && !item.contains("(no text)") -> 2  // 有文字的TextView
                    item.contains("ImageView") -> 3  // 圖片元素
                    item.contains("Button") -> 4  // 按鈕元素
                    item.contains("scrollable") -> 5  // 可滾動元素
                    item.contains("selected") -> 6  // 選中狀態
                    item.contains("focused") -> 7  // 焦點狀態
                    else -> 8
                }
            }
            
            // 分類不同類型的內容
            val clickableItems = sortedItems.filter { it.contains("clickable") && !it.contains("(no text)") }
            val textItems = sortedItems.filter { it.contains("TextView") && !it.contains("(no text)") }
            val imageItems = sortedItems.filter { it.contains("ImageView") }
            val buttonItems = sortedItems.filter { it.contains("Button") }
            val otherItems = sortedItems.filter { 
                !clickableItems.contains(it) && !textItems.contains(it) && 
                !imageItems.contains(it) && !buttonItems.contains(it)
            }
            
            return if (sortedItems.isNotEmpty()) {
                buildString {
                    // 頁面信息頭部
                    append("📱 LINE 頁面信息\n")
                    append("頁面類型: $currentPageType\n")
                    append("頁面標題: $currentPageTitle\n")
                    append("掃描項目: ${sortedItems.size} 個 (已過濾)\n\n")
                    
                    append("🎯 === 當前頁面內容分析 ===\n")
                    
                    // 可點擊元素（按鈕、選單等）
                    if (clickableItems.isNotEmpty()) {
                        append("\n🖱️ === 可點擊元素 (${clickableItems.size} 項) ===\n")
                        append(clickableItems.take(15).joinToString("\n"))
                        if (clickableItems.size > 15) {
                            append("\n... 還有 ${clickableItems.size - 15} 個可點擊元素\n")
                        }
                    }
                    
                    // 文字內容
                    if (textItems.isNotEmpty()) {
                        append("\n📝 === 文字內容 (${textItems.size} 項) ===\n")
                        append(textItems.take(20).joinToString("\n"))
                        if (textItems.size > 20) {
                            append("\n... 還有 ${textItems.size - 20} 個文字元素\n")
                        }
                    }
                    
                    // 圖片元素
                    if (imageItems.isNotEmpty()) {
                        append("\n🖼️ === 圖片元素 (${imageItems.size} 項) ===\n")
                        append(imageItems.take(10).joinToString("\n"))
                        if (imageItems.size > 10) {
                            append("\n... 還有 ${imageItems.size - 10} 個圖片元素\n")
                        }
                    }
                    
                    // 按鈕元素
                    if (buttonItems.isNotEmpty()) {
                        append("\n🔘 === 按鈕元素 (${buttonItems.size} 項) ===\n")
                        append(buttonItems.take(10).joinToString("\n"))
                        if (buttonItems.size > 10) {
                            append("\n... 還有 ${buttonItems.size - 10} 個按鈕元素\n")
                        }
                    }
                    
                    // 其他元素（只顯示重要的）
                    val importantOtherItems = otherItems.filter { 
                        it.contains("scrollable") || it.contains("selected") || it.contains("focused")
                    }
                    if (importantOtherItems.isNotEmpty()) {
                        append("\n🔧 === 重要其他元素 (${importantOtherItems.size} 項) ===\n")
                        append(importantOtherItems.take(8).joinToString("\n"))
                        if (importantOtherItems.size > 8) {
                            append("\n... 還有 ${importantOtherItems.size - 8} 個重要元素\n")
                        }
                    }
                    
                    // 添加頁面特定的信息
                    when (currentPageType) {
                        "Chat Settings" -> {
                            append("\n💬 === 聊天設定頁面 ===\n")
                            append("當前在聊天設定頁面，可以調整聊天相關的設定\n")
                            append("主要設定項目：背景、字體大小、傳送設定等\n")
                        }
                        "Profile Settings" -> {
                            append("\n👤 === 個人檔案設定頁面 ===\n")
                            append("當前在個人檔案設定頁面，可以修改個人資料\n")
                            append("主要設定項目：個人資訊、帳號設定、隱私設定等\n")
                        }
                        "Privacy Settings" -> {
                            append("\n🔒 === 隱私設定頁面 ===\n")
                            append("當前在隱私設定頁面，可以調整隱私相關設定\n")
                        }
                        "General Settings" -> {
                            append("\n⚙️ === 一般設定頁面 ===\n")
                            append("當前在一般設定頁面，可以調整應用程式基本設定\n")
                        }
                        "Account Settings" -> {
                            append("\n🔐 === 帳號設定頁面 ===\n")
                            append("當前在帳號設定頁面，可以管理帳號相關設定\n")
                        }
                    }
                }
            } else {
                "No LINE content detected - trying alternative scan..."
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error building LINE specific summary", e)
            return "Error scanning LINE content: ${e.message}"
        }
    }
    
    /**
     * 根據頁面類型過濾相關內容
     */
    private fun filterItemsByPageType(items: List<String>, pageType: String): List<String> {
        return when (pageType) {
            "Chat Settings" -> {
                // 聊天設定頁面：只顯示聊天相關的設定項目
                items.filter { item ->
                    val text = item.lowercase()
                    text.contains("聊天") || text.contains("chat") ||
                    text.contains("背景") || text.contains("background") ||
                    text.contains("字體") || text.contains("font") ||
                    text.contains("字型") || text.contains("傳送") ||
                    text.contains("send") || text.contains("預覽") ||
                    text.contains("preview") || text.contains("設定") ||
                    text.contains("setting") || text.contains("description") ||
                    text.contains("說明") || text.contains("inlined_value")
                }
            }
            "Profile Settings" -> {
                // 個人檔案設定頁面：只顯示個人相關的設定項目
                items.filter { item ->
                    val text = item.lowercase()
                    text.contains("個人") || text.contains("profile") ||
                    text.contains("帳號") || text.contains("account") ||
                    text.contains("隱私") || text.contains("privacy") ||
                    text.contains("貼圖") || text.contains("sticker") ||
                    text.contains("字型") || text.contains("font") ||
                    text.contains("提醒") || text.contains("reminder") ||
                    text.contains("照片") || text.contains("photo") ||
                    text.contains("影片") || text.contains("video") ||
                    text.contains("聊天") || text.contains("chat") ||
                    text.contains("通話") || text.contains("call") ||
                    text.contains("相簿") || text.contains("album") ||
                    text.contains("設定") || text.contains("setting") ||
                    text.contains("title") || text.contains("description")
                }
            }
            "Privacy Settings" -> {
                // 隱私設定頁面：只顯示隱私相關的設定項目
                items.filter { item ->
                    val text = item.lowercase()
                    text.contains("隱私") || text.contains("privacy") ||
                    text.contains("設定") || text.contains("setting") ||
                    text.contains("權限") || text.contains("permission")
                }
            }
            "General Settings" -> {
                // 一般設定頁面：只顯示一般相關的設定項目
                items.filter { item ->
                    val text = item.lowercase()
                    text.contains("一般") || text.contains("general") ||
                    text.contains("設定") || text.contains("setting") ||
                    text.contains("基本") || text.contains("basic")
                }
            }
            "Account Settings" -> {
                // 帳號設定頁面：只顯示帳號相關的設定項目
                items.filter { item ->
                    val text = item.lowercase()
                    text.contains("帳號") || text.contains("account") ||
                    text.contains("設定") || text.contains("setting") ||
                    text.contains("登入") || text.contains("login") ||
                    text.contains("登出") || text.contains("logout")
                }
            }
            else -> {
                // 未知頁面：顯示所有內容
                items
            }
        }
    }
    
    private fun findLineContent(node: AccessibilityNodeInfo, items: MutableList<String>, depth: Int, maxDepth: Int) {
        if (depth > maxDepth) return
        
        try {
            val text = node.text?.toString()?.trim()
            val contentDesc = node.contentDescription?.toString()?.trim()
            val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                node.hintText?.toString()?.trim()
            } else null
            val viewId = node.viewIdResourceName
            val className = node.className?.toString()
            
            val rect = Rect()
            node.getBoundsInScreen(rect)
            
            val hasText = !text.isNullOrBlank() || !contentDesc.isNullOrBlank() || !hint.isNullOrBlank()
            val isInteractive = node.isClickable || node.isEditable || node.isCheckable
            val isImportantContainer = isImportantLineContainer(viewId, className, rect)
            
            // 更寬鬆的可見性檢查，抓取更多元素
            val isVisible = rect.width() > 0 && rect.height() > 0
            
            // 更積極地捕獲內容，降低門檻
            if (isVisible && (hasText || isInteractive || isImportantContainer || depth < 10 || rect.width() > 10 || rect.height() > 10)) {
                val displayText = text ?: contentDesc ?: hint ?: "(no text)"
                
                val interactions = mutableListOf<String>()
                if (node.isClickable) interactions.add("clickable")
                if (node.isEditable) interactions.add("editable")
                if (node.isScrollable) interactions.add("scrollable")
                if (node.isSelected) interactions.add("selected")
                if (node.isLongClickable) interactions.add("longClickable")
                if (node.isFocused) interactions.add("focused")
                
                val label = buildString {
                    append("${"  ".repeat(depth)}• \"$displayText\"")
                    if (!viewId.isNullOrBlank()) {
                        val shortId = viewId.substringAfterLast("/")
                        append("  [id=$shortId]")
                    }
                    if (!className.isNullOrBlank()) {
                        val shortClass = className.substringAfterLast(".")
                        append("  <$shortClass>")
                    }
                    if (interactions.isNotEmpty()) append("  {${interactions.joinToString(",")}}")
                    append("  @(${rect.left},${rect.top},${rect.width()}x${rect.height()})")
                }
                items.add(label)
            }
            
            // 遞歸搜索子節點
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                child?.let { findLineContent(it, items, depth + 1, maxDepth) }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error processing node at depth $depth", e)
        }
    }
    
    private fun isImportantLineContainer(viewId: String?, className: String?, rect: Rect): Boolean {
        val id = viewId?.lowercase() ?: ""
        val cls = className?.lowercase() ?: ""
        
        val isGeneralContainer = id.contains("list") || id.contains("recycler") || id.contains("scroll") ||
                cls.contains("recyclerview") || cls.contains("listview") || cls.contains("scrollview") ||
                cls.contains("layout") || cls.contains("view") || cls.contains("group")
        
        return isGeneralContainer && rect.width() > 20 && rect.height() > 20
    }
    
    private fun tryGetDataFromAllWindows() {
        try {
            Log.d(TAG, "Trying to get data from all windows...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val windows = windows
                Log.d(TAG, "Found ${windows?.size ?: 0} windows")
                
                // 優先尋找 LINE 視窗
                var lineWindowFound = false
                windows?.forEach { windowInfo ->
                    val root = windowInfo.root
                    if (root != null) {
                        val packageName = root.packageName?.toString()
                        Log.d(TAG, "Processing window: ${windowInfo.title}, Package: $packageName")
                        
                        // 忽略我們自己的視窗
                        if (packageName == "com.example.bubbleassistant") {
                            Log.d(TAG, "忽略 GrandmaHelper 視窗")
                            return@forEach
                        }
                        
                        // 優先處理 LINE 視窗
                        if (packageName == "jp.naver.line.android") {
                            Log.d(TAG, "找到 LINE 視窗，進行深度掃描")
                            lineWindowFound = true
                            val lineContentSummary = buildLineSpecificSummary(root)
                            if (lineContentSummary.isNotEmpty() && !lineContentSummary.contains("No LINE content detected")) {
                                updateLatestScreenInfo(lineContentSummary)
                                updateOverlay(lineContentSummary)
                                return@forEach
                            }
                        }
                    }
                }
                
                // 如果沒有找到 LINE 視窗，尋找其他非 GrandmaHelper 視窗
                if (!lineWindowFound) {
                    windows?.forEach { windowInfo ->
                        val root = windowInfo.root
                        if (root != null) {
                            val packageName = root.packageName?.toString()
                            if (packageName != "com.example.bubbleassistant") {
                                Log.d(TAG, "處理非 LINE 視窗: ${windowInfo.title}")
                                val summary = buildNodeSummary(root, maxItems = 50)
                                if (summary.contains("Captured elements:") && !summary.contains("Captured elements: 0")) {
                                    updateLatestScreenInfo("Other App:\n$summary")
                                    updateOverlay("Other App:\n$summary")
                                    return@forEach
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting data from all windows", e)
        }
    }
    
    /**
     * 提取當前頁面標題
     */
    private fun extractCurrentPageTitle(rootNode: AccessibilityNodeInfo): String {
        try {
            val queue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
            queue.add(rootNode)
            
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                
                // 檢查是否為頁面標題
                val viewId = node.viewIdResourceName?.lowercase() ?: ""
                val text = node.text?.toString()?.trim() ?: ""
                
                if (viewId.contains("title") || viewId.contains("header") || 
                    viewId.contains("toolbar") || viewId.contains("action_bar")) {
                    if (text.isNotBlank()) {
                        return text
                    }
                }
                
                // 檢查子節點
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting page title", e)
        }
        
        return "Unknown Page"
    }
    
    /**
     * 識別 LINE 頁面類型
     */
    private fun identifyLinePageType(rootNode: AccessibilityNodeInfo): String {
        try {
            val queue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
            queue.add(rootNode)
            
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                
                val viewId = node.viewIdResourceName?.lowercase() ?: ""
                val text = node.text?.toString()?.trim() ?: ""
                
                // 檢查各種頁面類型
                when {
                    viewId.contains("chat") || text.contains("聊天") -> return "Chat Settings"
                    viewId.contains("profile") || text.contains("個人") -> return "Profile Settings"
                    viewId.contains("privacy") || text.contains("隱私") -> return "Privacy Settings"
                    viewId.contains("sticker") || text.contains("貼圖") -> return "Sticker Settings"
                    viewId.contains("store") || text.contains("商店") -> return "Store"
                    viewId.contains("general") || text.contains("一般") -> return "General Settings"
                    viewId.contains("account") || text.contains("帳號") -> return "Account Settings"
                }
                
                // 檢查子節點
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error identifying page type", e)
        }
        
        return "Unknown"
    }
}

