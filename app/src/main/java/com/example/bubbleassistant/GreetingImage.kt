// GreetingImage.kt
package com.example.bubbleassistant

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow

@Composable
fun GreetingImageScreen(
    vm: MorningImageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var prompt by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2C8C82), Color(0xFF6BC8C5))
                        ),
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color(0x55000000),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        ),
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize
                    )
                ) { append("Grandma Helper") }
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 標題 Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "早安圖 Icon",
                        tint = Color(0xFF42A09D),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "每日一張早安圖",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 結果顯示區（正方形）
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .background(Color(0xFFE2F4F3)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        vm.resultBitmap.value != null -> {
                            Image(
                                bitmap = vm.resultBitmap.value!!.asImageBitmap(),
                                contentDescription = "生成結果"
                            )
                        }
                        vm.isLoading.value -> {
                            CircularProgressIndicator()
                        }
                        else -> {
                            Text("請按生成取得早安圖", color = Color.Gray)
                        }
                    }
                }

                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text(text = "想要哪種早安圖", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.heightIn(min = 56.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE2F4F3),
                        unfocusedContainerColor = Color(0xFFE2F4F3)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 生成
                    Button(
                        enabled = !vm.isLoading.value,
                        onClick = {
                            val userPrompt = prompt
                            prompt = ""
                            vm.regenerate(userPrompt) // 只有按下去才叫 API
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A09D))
                    ) {
                        Text(if (vm.isLoading.value) "生成中..." else "生成", color = Color.White)
                    }

                    Spacer(Modifier.width(16.dp))

                    // 存到相簿
                    Button(
                        enabled = vm.resultBitmap.value != null && !vm.isLoading.value,
                        onClick = {
                            val bmp = vm.resultBitmap.value ?: return@Button
                            // 用 IO 執行保存
                            CoroutineScope(Dispatchers.IO).launch {
                                val ok = saveBitmapToGallery(
                                    context = context,
                                    bitmap = bmp,
                                    displayName = "morning_image_${System.currentTimeMillis()}.png"
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        if (ok) "已存到相簿/Pictures/GrandmaHelper" else "儲存失敗",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A09D))
                    ) {
                        Text("存到相簿", color = Color.White)
                    }
                }

                vm.errorMsg.value?.let { msg ->
                    if (msg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(msg, color = Color.Red)
                    }
                }
            }
        }
    }
}

// --- API + 儲存功能（原樣保留） ---
suspend fun generateMorningImage(
    context: android.content.Context,
    prompt: String
): Bitmap = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("prompt", if (prompt.isNotBlank()) prompt else "傳給我一張圖")
        .build()

    val request = Request.Builder()
        .url(GEMINI_IMAGE_API_URL)
        .post(multipart)
        .build()

    val bytes = client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
        resp.body?.bytes() ?: throw IllegalStateException("空回應")
    }

    val outFile = File(context.cacheDir, "morning_${System.currentTimeMillis()}.png")
    FileOutputStream(outFile).use { it.write(bytes) }

    BitmapFactory.decodeFile(outFile.absolutePath) ?: throw IllegalStateException("無法解析回傳圖片")
}

private suspend fun saveBitmapToGallery(
    context: android.content.Context,
    bitmap: Bitmap,
    displayName: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GrandmaHelper")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return@withContext false
        resolver.openOutputStream(uri)?.use { out ->
            val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!ok) return@withContext false
        } ?: return@withContext false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (_: Throwable) {
        false
    }
}

private const val GEMINI_IMAGE_API_URL =
    "https://morning-image-api-855188038216.asia-east1.run.app/generate"
