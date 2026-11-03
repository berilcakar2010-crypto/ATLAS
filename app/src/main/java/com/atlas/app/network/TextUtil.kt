package com.atlas.app.network

import android.text.Html
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TextUtil {
    /** HTML'i düz metne çevirir, boşlukları toplar ve gerekirse sözcük sınırında keser. */
    fun cleanHtml(raw: String, maxChars: Int = 700): String {
        if (raw.isBlank()) return ""
        val plain = Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT).toString()
            .replace('￼', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (plain.length <= maxChars) return plain
        val cut = plain.take(maxChars)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > maxChars / 2) cut.take(lastSpace) else cut) + "…"
    }

    fun isHttpUrl(url: String): Boolean {
        val lower = url.trim().lowercase(Locale.ROOT)
        return lower.startsWith("https://") || lower.startsWith("http://")
    }

    private val trackingParams = setOf("fbclid", "gclid", "mc_cid", "mc_eid", "ref", "ref_src", "source")

    /**
     * Çift kaydı önlemek için adresi normalleştirir: küçük harf ana makine, "www." yok,
     * parça (#...) yok, izleme parametreleri yok, sondaki "/" yok. Sonra SHA-1 özeti alınır.
     */
    fun urlKey(url: String): String {
        val normalized = try {
            val uri = URI(url.trim())
            val host = (uri.host ?: "").lowercase(Locale.ROOT).removePrefix("www.")
            val path = (uri.rawPath ?: "").trimEnd('/')
            val query = (uri.rawQuery ?: "")
                .split('&')
                .filter { it.isNotBlank() }
                .filterNot { p ->
                    val key = p.substringBefore('=').lowercase(Locale.ROOT)
                    key.startsWith("utm_") || key in trackingParams
                }
                .sorted()
                .joinToString("&")
            host + path + if (query.isNotEmpty()) "?$query" else ""
        } catch (_: Exception) {
            url.trim().lowercase(Locale.ROOT)
        }
        return sha1(normalized)
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** RSS (RFC 1123), Atom (ISO 8601) ve yalın tarih biçimlerini dener; başarısızsa null döner. */
    fun parseDate(raw: String?): Long? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        try {
            return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        } catch (_: Exception) {
        }
        try {
            return OffsetDateTime.parse(s).toInstant().toEpochMilli()
        } catch (_: Exception) {
        }
        try {
            return Instant.parse(s).toEpochMilli()
        } catch (_: Exception) {
        }
        try {
            return LocalDate.parse(s.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) {
        }
        return null
    }
}
