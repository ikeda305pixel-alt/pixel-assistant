package com.antigravity.pixelassistant.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient(
    private val apiKey: String,
    private val quotaManager: FreeTierQuotaManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateReplies(platform: String, partnerText: String): List<ReplyCandidate> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || !quotaManager.canMakeRequest()) {
            return@withContext SmartMockEngine.generateReplyCandidates(partnerText)
        }

        val prompt = """
            あなたは$platform（マッチングアプリ・メッセージアプリ）の返信アシスタントです。
            相手からの未返信メッセージ: 「$partnerText」
            この文脈に合わせた自然で好感度の高い4つの返信案を作成してください。
            出力は以下の形式のJSON配列のみを返してください：
            [
              {"id": "1", "label": "😊 共感＋質問", "text": "本文1"},
              {"id": "2", "label": "✨ フレンドリー", "text": "本文2"},
              {"id": "3", "label": "😄 ユーモア", "text": "本文3"},
              {"id": "4", "label": "☕ お誘い・提案", "text": "本文4"}
            ]
        """.trimIndent()

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val partsArray = JSONArray().put(JSONObject().put("text", prompt))
                val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
                put("contents", contentsArray)
                put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext SmartMockEngine.generateReplyCandidates(partnerText)
            }

            quotaManager.recordRequest()
            val respString = response.body?.string() ?: ""
            parseRepliesFromJson(respString, partnerText)
        } catch (_: Exception) {
            SmartMockEngine.generateReplyCandidates(partnerText)
        }
    }

    suspend fun translateSubtitle(englishText: String): String = withContext(Dispatchers.IO) {
        if (englishText.isBlank()) return@withContext ""
        if (apiKey.isBlank() || !quotaManager.canMakeRequest()) {
            return@withContext SmartMockEngine.translateSubtitles(englishText)
        }

        val prompt = "以下の映画・ドラマの英語字幕を、映像の字幕として読みやすい自然で簡潔な日本語に翻訳してください（説明不要、日本語の字幕文のみ出力）：\n\"$englishText\""

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val partsArray = JSONArray().put(JSONObject().put("text", prompt))
                val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext SmartMockEngine.translateSubtitles(englishText)
            }

            quotaManager.recordRequest()
            val respString = response.body?.string() ?: ""
            val root = JSONObject(respString)
            val text = root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removeSurrounding("\"")
            text
        } catch (_: Exception) {
            SmartMockEngine.translateSubtitles(englishText)
        }
    }

    private fun parseRepliesFromJson(rawResponse: String, fallbackText: String): List<ReplyCandidate> {
        try {
            val root = JSONObject(rawResponse)
            val text = root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val jsonArray = JSONArray(text.trim())
            val list = mutableListOf<ReplyCandidate>()
            for (i in 0 until jsonArray.length().coerceAtMost(4)) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ReplyCandidate(
                        id = obj.optString("id", "${i + 1}"),
                        label = obj.optString("label", "返信案 ${i + 1}"),
                        text = obj.optString("text", "")
                    )
                )
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {}

        return SmartMockEngine.generateReplyCandidates(fallbackText)
    }
}
