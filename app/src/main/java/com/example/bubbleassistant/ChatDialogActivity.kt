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

class ChatDialogActivity : Activity() {

    private lateinit var editText: EditText
    private lateinit var sendButton: Button
    private lateinit var responseView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 半透明背景 + 對話框樣式
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)

        setContentView(R.layout.dialog_chat)

        editText = findViewById(R.id.input_message)
        sendButton = findViewById(R.id.send_button)
        responseView = findViewById(R.id.response_text)

        sendButton.setOnClickListener {
            val message = editText.text.toString().trim()
            if (message.isNotEmpty()) {
                responseView.text = "Thinking..."
                sendToGemini(message)
            }
        }
    }

    private fun sendToGemini(prompt: String) {
        // TODO: 改成真正的 Gemini API 呼叫
        Thread {
            // 模擬延遲
            Thread.sleep(1000)
            val response = "Gemini 回覆：" + prompt.reversed() // 假資料

            runOnUiThread {
                responseView.text = response
                saveConversation(prompt, response)
            }
        }.start()
    }

    private fun saveConversation(user: String, gemini: String) {
        // TODO: 可換成 SQLite 或 DataStore
        val sharedPref = getSharedPreferences("chat_history", MODE_PRIVATE)
        val current = sharedPref.getString("log", "") ?: ""
        sharedPref.edit().putString("log", "$current\nYou: $user\nAI: $gemini").apply()
    }
}