// MorningImageViewModel.kt
package com.example.bubbleassistant

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MorningImageViewModel(private val appContext: Context) : ViewModel() {

    val resultBitmap = mutableStateOf<Bitmap?>(null)
    val isLoading = mutableStateOf(false)
    val errorMsg = mutableStateOf<String?>(null)

    private var hasGeneratedOnce = false

    /** App 啟動時只自動跑一次 */
    fun generateOnce() {
        if (hasGeneratedOnce) return
        hasGeneratedOnce = true
        regenerate("") // 第一次不帶 prompt
    }

    /** 使用者按下「生成」才再請求 */
    fun regenerate(prompt: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMsg.value = null
            try {
                // 呼叫你在 GreetingImage.kt 裡的 suspend 函式（已移除 private）
                resultBitmap.value = generateMorningImage(appContext, prompt)
            } catch (t: Throwable) {
                errorMsg.value = t.message ?: "生成失敗"
            } finally {
                isLoading.value = false
            }
        }
    }
}