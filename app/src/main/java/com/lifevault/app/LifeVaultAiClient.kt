package com.lifevault.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LifeVaultAiClient {
    private const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    fun ask(apiKey: String, prompt: String, localContext: String): String {
        require(apiKey.isNotBlank() && prompt.isNotBlank()) { "Add your Google AI Studio API key and enter a question." }

        val userText = buildString {
            append(prompt)
            if (localContext.isNotBlank()) {
                append("\n\nUser-approved local LifeVault summary:\n")
                append(localContext)
            }
        }
        val payload = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(
                JSONObject().put("text", "You are LifeVault Assistant. Be clear, practical and privacy-conscious. Do not claim to perform actions in the app. For financial topics, provide general organisation insights, not professional financial advice.")
            )))
            .put("contents", JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userText)))
            ))
            .put("generationConfig", JSONObject().put("temperature", 0.4))

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IllegalStateException(
                    message?.takeIf { it.isNotBlank() } ?: "Google Gemini returned HTTP $code."
                )
            }

            val responseText = JSONObject(body)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.let { parts ->
                    (0 until parts.length()).mapNotNull { index ->
                        parts.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() }
                    }.joinToString("\n")
                }
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Google Gemini returned an empty response.")
            return responseText
        } finally {
            connection.disconnect()
        }
    }
}
