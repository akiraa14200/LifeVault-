package com.lifevault.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LifeVaultAiClient {
    fun ask(endpoint: String, model: String, apiKey: String, prompt: String, localContext: String): String {
        require(endpoint.startsWith("https://")) { "Use a secure HTTPS API endpoint." }
        require(model.isNotBlank() && apiKey.isNotBlank() && prompt.isNotBlank()) { "Enter a model, API key and question." }

        val normalizedEndpoint = endpoint.trimEnd('/').removeSuffix("/chat/completions")
        val url = URL("$normalizedEndpoint/chat/completions")
        val messages = JSONArray()
            .put(JSONObject()
                .put("role", "system")
                .put("content", "You are LifeVault Assistant. Be clear, practical and privacy-conscious. Do not claim to perform actions in the app. For financial topics, provide general organisation insights, not professional financial advice."))
            .put(JSONObject().put("role", "user").put("content", buildString {
                append(prompt)
                if (localContext.isNotBlank()) {
                    append("\n\nUser-approved local LifeVault summary:\n")
                    append(localContext)
                }
            }))
        val payload = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.4)

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
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
                    message?.takeIf { it.isNotBlank() } ?: "AI provider returned HTTP $code."
                )
            }

            val responseText = JSONObject(body)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The AI provider returned an empty response.")
            return responseText
        } finally {
            connection.disconnect()
        }
    }
}
