package com.example.bubbleassistant

import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            initialUserMsg = message
            responseView.text = "Thinking..."
            sendButton.isEnabled = false

            OverlayAgent.scope.launch {
                try {
                    val serverMessage = withContext(Dispatchers.IO) {
                        OverlayAgent.callAssistantApi(
                            userMsg = initialUserMsg,
                            summaryText = fakeSummaryText(),            // TODO: 換成實際監控
                            timestampMs = System.currentTimeMillis()
                        )
                    }.trim()

                    if (serverMessage.contains("恭喜成功")) {
                        steps = mutableListOf("🎉 恭喜成功！")
                        showStepOverlay()
                        showSuccessThenDismiss()
                    } else {
                        steps = mutableListOf(serverMessage.ifBlank { "請依畫面提示操作下一步" })
                        showStepOverlay()
                    }

                    // 現在可以安心關畫面（scope 在 OverlayAgent，不會被取消）
                    finish()
                } catch (e: Exception) {
                    responseView.text = "發生錯誤：${e.message ?: "未知錯誤"}"
                    sendButton.isEnabled = true
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
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
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

            cb.isChecked = false
            val currentText = steps.firstOrNull().orEmpty().trim()
            if (currentText.contains("恭喜成功")) {
                showSuccessThenDismiss()
                isBusy = false
                return@setOnCheckedChangeListener
            }

            stepView?.findViewById<TextView>(R.id.tv_step)?.text = "請稍候…"

            OverlayAgent.scope.launch {
                try {
                    val nextMsg = withContext(Dispatchers.IO) {
                        OverlayAgent.callAssistantApi(
                            userMsg = initialUserMsg,                // 每次都帶第一次的輸入
                            summaryText = fakeSummaryText(),         // TODO: 換成實際監控
                            timestampMs = System.currentTimeMillis()
                        )
                    }.trim()

                    if (nextMsg.contains("恭喜成功")) {
                        steps = mutableListOf("🎉 恭喜成功！")
                        updateStepText()
                        showSuccessThenDismiss()
                    } else {
                        steps = mutableListOf(nextMsg.ifBlank { "請依畫面提示操作下一步" })
                        updateStepText()
                    }
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "取得下一步失敗：${e.message ?: "未知錯誤"}", Toast.LENGTH_SHORT).show()
                    updateStepText() // 還原
                } finally {
                    isBusy = false
                }
            }
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

    private fun updateStepText() {
        val tv = stepView?.findViewById<TextView>(R.id.tv_step) ?: return
        tv.text = steps.firstOrNull().orEmpty()
        stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = true
    }

    private fun showSuccessThenDismiss() {
        val tv = stepView?.findViewById<TextView>(R.id.tv_step) ?: return
        tv.text = "🎉 恭喜成功！"
        stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = false
        stepView?.postDelayed({ dismissOverlay() }, 1200)
    }

    private fun dismissOverlay() {
        stepView?.let { v -> try { wm?.removeView(v) } catch (_: Exception) {} }
        stepView = null
        stepLp = null
        steps.clear()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
}
