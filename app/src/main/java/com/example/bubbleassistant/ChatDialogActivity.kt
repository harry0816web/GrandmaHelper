package com.example.bubbleassistant

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.view.ViewGroup
import android.view.Gravity
import android.view.MotionEvent
import android.graphics.Rect
import android.view.View

class ChatDialogActivity : Activity() {

    private lateinit var editText: EditText
    private lateinit var sendButton: Button
    private lateinit var responseView: TextView
    private lateinit var dialogBox: View
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定透明背景和 Dialog 樣式
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)

        setContentView(R.layout.dialog_chat)

        // 初始化元件
        editText = findViewById(R.id.input_message)
        sendButton = findViewById(R.id.send_button)
        responseView = findViewById(R.id.response_text)
        rootView = findViewById(R.id.dialog_root)
        dialogBox = findViewById(R.id.dialog_box)

        // 點擊「送出」按鈕時處理輸入
        sendButton.setOnClickListener {
            val message = editText.text.toString().trim()
            if (message.isNotEmpty()) {
                responseView.text = "Thinking..."
                sendToGemini(message)
            }
        }

        // 點擊 dialog 外部時關閉
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val rect = Rect()
                dialogBox.getGlobalVisibleRect(rect)
                if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    finish()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun sendToGemini(prompt: String) {
        Thread {
            Thread.sleep(1000)  // 模擬延遲
            val response = "Gemini 回覆：" + prompt.reversed()

            runOnUiThread {
                responseView.text = response
                saveConversation(prompt, response)
            }
        }.start()
    }

    private fun saveConversation(user: String, gemini: String) {
        val sharedPref = getSharedPreferences("chat_history", MODE_PRIVATE)
        val current = sharedPref.getString("log", "") ?: ""
        sharedPref.edit().putString("log", "$current\nYou: $user\nAI: $gemini").apply()
    }
}
