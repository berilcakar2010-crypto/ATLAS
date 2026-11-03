package com.atlas.app.ai

import com.atlas.app.network.Http
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

data class AiMessage(val role: String, val content: String) // role: "user" | "assistant"

/** Yapay zekâ sağlayıcısı arayüzü. Uygulamanın hiçbir özelliği bunun varlığına bağlı değildir. */
interface AiProvider {
    val label: String
    suspend fun chat(system: String, history: List<AiMessage>): String
}

class AiNotConfigured(message: String) : IOException(message)

object AiFactory {
    fun create(store: SecretStore): AiProvider {
        val key = store.apiKey()
        if (store.provider == "NONE" || key.isNullOrBlank()) {
            throw AiNotConfigured("Yapay zekâ kapalı veya anahtar girilmemiş.")
        }
        if (store.model.isBlank()) throw AiNotConfigured("Model adı seçilmemiş.")
        return when (store.provider) {
            "GEMINI" -> GeminiProvider(key, store.model)
            "OPENAI_COMPAT" -> {
                if (store.baseUrl.isBlank()) throw AiNotConfigured("Uç nokta adresi girilmemiş.")
                OpenAiCompatProvider(store.baseUrl, key, store.model)
            }
            else -> throw AiNotConfigured("Bilinmeyen sağlayıcı.")
        }
    }
}

/** Google Gemini REST API. Anahtar adres satırında değil, başlıkta gönderilir. */
class GeminiProvider(private val apiKey: String, private val model: String) : AiProvider {
    override val label = "Gemini ($model)"

    override suspend fun chat(system: String, history: List<AiMessage>): String {
        val contents = JSONArray()
        history.forEach {
            contents.put(
                JSONObject()
                    .put("role", if (it.role == "assistant") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", it.content))),
            )
        }
        val body = JSONObject()
            .put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
        val name = model.removePrefix("models/")
        val raw = Http.postJson(
            "https://generativelanguage.googleapis.com/v1beta/models/$name:generateContent",
            body.toString(),
            mapOf("x-goog-api-key" to apiKey),
        )
        val parts = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IOException("Yanıt boş veya engellendi.")
        return (0 until parts.length()).joinToString("") { parts.getJSONObject(it).optString("text") }.trim()
    }

    companion object {
        /** Anahtarın erişebildiği, metin üretebilen modelleri listeler (uydurma model adı yok). */
        suspend fun listModels(apiKey: String): List<String> {
            val raw = Http.get(
                "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100",
                mapOf("x-goog-api-key" to apiKey),
            )
            val arr = JSONObject(raw).optJSONArray("models") ?: return emptyList()
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                val methods = m.optJSONArray("supportedGenerationMethods") ?: continue
                val ok = (0 until methods.length()).any { methods.getString(it) == "generateContent" }
                if (ok) out += m.optString("name").removePrefix("models/")
            }
            return out
        }
    }
}

/** OpenAI uyumlu /chat/completions uç noktası (kullanıcının seçtiği sunucu). */
class OpenAiCompatProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
) : AiProvider {
    override val label = "Özel uç nokta ($model)"

    override suspend fun chat(system: String, history: List<AiMessage>): String {
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        history.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val body = JSONObject().put("model", model).put("messages", msgs)
        val raw = Http.postJson("$baseUrl/chat/completions", body.toString(), mapOf("Authorization" to "Bearer $apiKey"))
        return JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content")?.trim()
            ?: throw IOException("Yanıt boş.")
    }
}

/** Deep Dive için sabit sistem yönergesi: uydurmayı ve sahte kaynağı açıkça yasaklar. */
object AiPrompts {
    const val DEEP_DIVE = "Kullanıcı bir lise öğrencisi; Türkçe, sade ve dürüst yanıt ver. " +
        "Kaynak, DOI, yazar, istatistik veya alıntı UYDURMA. Emin olmadığın bir şeyi 'emin değilim' diye belirt. " +
        "Gerçek kaynak vermen gerekirse yalnızca bilinen, doğrulanabilir bir kaynağın genel adını söyle ve kullanıcıdan doğrulamasını iste. " +
        "Önce soruyu netleştir, sonra varsayımları, olası hipotezleri ve belirsizlikleri ayır. Cevabı kısa tut."
}
