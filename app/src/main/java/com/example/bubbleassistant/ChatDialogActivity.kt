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

class ChatDialogActivity : Activity() {

    private lateinit var editText: EditText
    private lateinit var sendButton: Button
    private lateinit var responseView: TextView
    private lateinit var dialogBox: View
    private lateinit var rootView: View

    // Overlay 步驟 UI
    private var wm: WindowManager? = null
    private var stepView: View? = null
    private var stepLp: WindowManager.LayoutParams? = null
    private var steps: MutableList<String> = mutableListOf()
    private var stepIdx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 對話框樣式
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)

        setContentView(R.layout.dialog_chat)

        // 綁定元件
        editText = findViewById(R.id.input_message)
        sendButton = findViewById(R.id.send_button)
        responseView = findViewById(R.id.response_text)
        rootView = findViewById(R.id.dialog_root)
        dialogBox = findViewById(R.id.dialog_box)

        // 用 applicationContext 拿 WindowManager，避免受 Activity lifecycle 影響
        wm = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager

        // 送出：模擬 Gemini 產生步驟 → 顯示置頂步驟 → 關閉對話框
        sendButton.setOnClickListener {
            val message = editText.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener

            responseView.text = "Thinking..."
            dialogBox.postDelayed({
                steps = fakeGeminiSteps(message).toMutableList()
                stepIdx = 0
                showStepOverlay()   // 顯示第一步
                finish()            // 關掉輸入對話框（不會影響 overlay 存活）
            }, 800)
        }

        // 點擊對話框外部關閉
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

    /** 假的 Gemini 回覆：依輸入字串生成示範步驟。之後直接替換為真的 API 回傳即可。 */
    private fun fakeGeminiSteps(userMsg: String): List<String> = listOf(
        "1. 打開你要操作的畫面（例如：$userMsg）",
        "2. 找到螢幕上的相關按鈕或欄位",
        "3. 點一下並輸入需要的內容",
        "4. 檢查無誤後按下『送出』或『確認』"
    )

    /** 顯示或更新頂部步驟 Overlay */
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
                // 可以點擊自己的元素，但不攔截整個畫面
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

    /** 綁定步驟框互動（勾選下一步、上滑關閉） */
    private fun bindStepEvents() {
        val cb = stepView!!.findViewById<CheckBox>(R.id.btn_check)
        cb.isChecked = false  // 初始未勾

        cb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                stepIdx++
                if (stepIdx >= steps.size) {
                    showSuccessThenDismiss()
                } else {
                    cb.isChecked = false   // 下一步前清除勾勾，等使用者再勾
                    updateStepText()
                }
            }
        }

        // 上滑快速收起（可選）
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

    /** 將目前步驟文字塞入 TextView（覆蓋 XML 預設） */
    private fun updateStepText() {
        val tv = stepView?.findViewById<TextView>(R.id.tv_step) ?: return
        tv.text = steps[stepIdx]
        stepView?.findViewById<CheckBox>(R.id.btn_check)?.visibility = View.VISIBLE
    }

    /** 最後一步：顯示恭喜文字並在 1.2 秒後自動收起 */
    private fun showSuccessThenDismiss() {
        val tv = stepView?.findViewById<TextView>(R.id.tv_step) ?: return
        tv.text = "🎉 恭喜成功！"
        stepView?.findViewById<CheckBox>(R.id.btn_check)?.isVisible = false
        stepView?.postDelayed({ dismissOverlay() }, 1200)
    }

    /** 收起 Overlay、重置狀態（避免記憶體洩漏） */
    private fun dismissOverlay() {
        stepView?.let { v -> try { wm?.removeView(v) } catch (_: Exception) {} }
        stepView = null
        stepLp = null
        steps.clear()
        stepIdx = 0
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        super.onDestroy()
        // 這裡不要主動關掉 overlay，避免送出後立刻被移除
        // dismissOverlay()
    }
}
