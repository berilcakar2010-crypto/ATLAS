package com.atlas.app.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Tek paylaşılan HTTP istemcisi. Açık adresler dışında hiçbir sunucuya veri göndermez. */
object Http {
    private const val USER_AGENT = "ATLAS/0.2 (personal research reader; Android)"
    private const val MAX_BYTES = 8L * 1024 * 1024

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** GET isteği; 2xx değilse veya gövde çok büyükse IOException fırlatır. */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url).header("User-Agent", USER_AGENT)
            headers.forEach { (k, v) -> builder.header(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Boş yanıt")
                if (body.contentLength() > MAX_BYTES) throw IOException("Yanıt çok büyük")
                body.string()
            }
        }

    /** JSON POST; yalnızca yapay zekâ sağlayıcıları için (kullanıcı açıkça etkinleştirdiğinde). */
    suspend fun postJson(url: String, json: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            headers.forEach { (k, v) -> builder.header(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${text.take(300)}")
                }
                text
            }
        }
}
